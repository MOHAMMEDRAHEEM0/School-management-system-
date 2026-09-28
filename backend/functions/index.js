/**
 * Production Cloud Functions for School ERP Multi-Tenant SaaS Platform.
 * Enforces server-side authorization boundaries, tenant claims, payment verification,
 * and immutable audit trails.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();

// =============================================================================
// 1. AUTH TRIGGER: AUTO-PROVISION CLAIMS & TENANT GUARDS
// =============================================================================

exports.onUserCreated = functions.auth.user().onCreate(async (user) => {
  const email = user.email || "";
  const defaultTenant = "INST-SXA-2026";
  const defaultRole = "STUDENT";

  const claims = {
    tenantId: defaultTenant,
    role: defaultRole,
    permissions: ["VIEW_ATTENDANCE_SELF", "VIEW_REPORT_CARD_SELF", "VIEW_FEE_SELF"],
    isSuperAdmin: false
  };

  await auth.setCustomUserClaims(user.uid, claims);

  // Store in tenant user database
  await db.doc(`institutions/${defaultTenant}/users/${user.uid}`).set({
    userId: user.uid,
    email: email,
    displayName: user.displayName || email.split("@")[0],
    role: defaultRole,
    institutionId: defaultTenant,
    isAccountDisabled: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
});

// =============================================================================
// 2. MULTI-TENANCY: SECURE SERVER-SIDE TENANT SWITCHING
// =============================================================================

exports.switchSchoolTenant = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const callerUid = context.auth.uid;
  const targetTenantId = data.targetTenantId;

  if (!targetTenantId || typeof targetTenantId !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "Target tenant ID is required.");
  }

  // 1. Verify membership in the target tenant collection
  const targetUserDoc = await db.doc(`institutions/${targetTenantId}/users/${callerUid}`).get();

  const isSuperAdmin = context.auth.token.isSuperAdmin === true;

  if (!targetUserDoc.exists && !isSuperAdmin) {
    throw new functions.https.HttpsError(
      "permission-denied",
      `Access Denied: Caller is not a registered member of tenant '${targetTenantId}'.`
    );
  }

  const userData = targetUserDoc.data() || {};
  if (userData.isAccountDisabled === true) {
    throw new functions.https.HttpsError("permission-denied", "Account is disabled in target institution.");
  }

  const newRole = userData.role || context.auth.token.role || "STUDENT";
  const newPermissions = userData.permissions || [];

  // 2. Issue updated claims for target tenant
  const updatedClaims = {
    ...context.auth.token,
    tenantId: targetTenantId,
    role: newRole,
    permissions: newPermissions
  };

  await auth.setCustomUserClaims(callerUid, updatedClaims);

  return {
    success: true,
    tenantId: targetTenantId,
    role: newRole,
    timestamp: Date.now()
  };
});

// =============================================================================
// 3. RBAC: PRIVILEGE ESCALATION PREVENTION
// =============================================================================

exports.setUserRoleAndPermissions = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const callerTenantId = context.auth.token.tenantId;
  const callerRole = context.auth.token.role;

  // Only Admin or Principal can change roles
  if (!["ADMIN", "ROLE_ADMIN", "PRINCIPAL", "ROLE_PRINCIPAL"].includes(callerRole)) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only School Administrators or Principals may modify user roles and permissions."
    );
  }

  const { targetUserId, newRole, newPermissions } = data;

  if (!targetUserId || !newRole) {
    throw new functions.https.HttpsError("invalid-argument", "targetUserId and newRole are required.");
  }

  // Verify target user is in the same tenant (prevents cross-tenant privilege escalation)
  const targetUserRef = db.doc(`institutions/${callerTenantId}/users/${targetUserId}`);
  const targetDoc = await targetUserRef.get();

  if (!targetDoc.exists) {
    throw new functions.https.HttpsError(
      "not-found",
      `Target user does not exist in institution '${callerTenantId}'.`
    );
  }

  // Update Firestore record
  await targetUserRef.update({
    role: newRole,
    permissions: newPermissions || [],
    updatedBy: context.auth.uid,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Update Firebase Auth Custom Claims
  await auth.setCustomUserClaims(targetUserId, {
    tenantId: callerTenantId,
    role: newRole,
    permissions: newPermissions || []
  });

  // Append immutable audit log
  await db.collection(`institutions/${callerTenantId}/audit_logs`).add({
    institutionId: callerTenantId,
    action: "ROLE_CHANGE",
    entityType: "UserEntity",
    entityId: targetUserId,
    actorUserId: context.auth.uid,
    actorName: context.auth.token.name || "Administrator",
    actorRole: callerRole,
    previousState: targetDoc.data().role,
    newState: newRole,
    timestamp: Date.now()
  });

  return { success: true, updatedRole: newRole };
});

// =============================================================================
// 4. ACCOUNT LIFECYCLE: DISABLE ACCOUNT & REVOKE SESSIONS
// =============================================================================

exports.toggleAccountDisabled = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const callerTenantId = context.auth.token.tenantId;
  const callerRole = context.auth.token.role;

  if (!["ADMIN", "ROLE_ADMIN", "PRINCIPAL", "ROLE_PRINCIPAL"].includes(callerRole)) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized administrative action.");
  }

  const { targetUserId, disabled } = data;
  const targetUserRef = db.doc(`institutions/${callerTenantId}/users/${targetUserId}`);

  await targetUserRef.update({
    isAccountDisabled: Boolean(disabled),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Immediately revoke all existing refresh tokens if disabling
  if (disabled) {
    await auth.revokeRefreshTokens(targetUserId);
  }

  return { success: true, isAccountDisabled: Boolean(disabled) };
});

// =============================================================================
// 5. SERVER-SIDE PAYMENT SIGNATURE VERIFICATION
// =============================================================================

exports.verifyPaymentWebhook = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).send("Method Not Allowed");
  }

  const signature = req.headers["x-razorpay-signature"] || req.headers["x-webhook-signature"];
  const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET || "default_test_secret";

  if (!signature) {
    return res.status(400).json({ error: "Missing signature header." });
  }

  const rawBody = req.rawBody || JSON.stringify(req.body);
  const expectedSignature = crypto
    .createHmac("sha256", webhookSecret)
    .update(rawBody)
    .digest("hex");

  if (!crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expectedSignature))) {
    console.error("Payment webhook signature verification failed.");
    return res.status(401).json({ error: "Invalid webhook signature." });
  }

  const payload = req.body;
  const event = payload.event;
  const paymentEntity = payload.payload?.payment?.entity;

  if (event === "payment.captured" && paymentEntity) {
    const institutionId = paymentEntity.notes?.institutionId || "INST-SXA-2026";
    const studentId = paymentEntity.notes?.studentId;
    const invoiceId = paymentEntity.notes?.invoiceId;

    await db.collection(`institutions/${institutionId}/payment_transactions`).doc(paymentEntity.id).set({
      transactionId: paymentEntity.id,
      institutionId: institutionId,
      studentId: studentId,
      invoiceId: invoiceId,
      amountPaise: paymentEntity.amount,
      currency: paymentEntity.currency,
      status: "SUCCESS",
      paymentMode: paymentEntity.method || "ONLINE",
      timestamp: Date.now()
    });
  }

  return res.status(200).json({ status: "acknowledged" });
});
