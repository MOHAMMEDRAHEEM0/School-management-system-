/**
 * Production Cloud Functions for School ERP Multi-Tenant SaaS Platform.
 * Enforces server-side authorization boundaries, tenant custom claims, payment verification,
 * and immutable audit trails.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();

// Strict allow-list of legitimate application roles
const ALLOWED_APPLICATION_ROLES = new Set([
  "ADMIN",
  "PRINCIPAL",
  "VICE_PRINCIPAL",
  "HEAD_OF_DEPARTMENT",
  "INCHARGE",
  "STAFF_MANAGER",
  "TEACHER",
  "ACCOUNTANT",
  "OFFICE",
  "STUDENT",
  "PARENT",
  "LIBRARIAN",
  "TRANSPORT_STAFF",
  "NON_TEACHING"
]);

// Allowed granular permissions
const ALLOWED_PERMISSIONS = new Set([
  "MANAGE_INSTITUTION", "CONFIGURE_ACADEMIC_YEAR", "MANAGE_CLASSES", "MANAGE_STAFF_DBMS",
  "ASSIGN_TEACHERS", "VIEW_ROSTER_ALL", "VIEW_ROSTER_SECTION", "MARK_ATTENDANCE_SECTION",
  "SCAN_REGISTER_CAMERA", "VERIFY_ATTENDANCE_INCHARGE", "APPROVE_ATTENDANCE_PRINCIPAL",
  "ENTER_MARKS_ASSIGNED_SUBJECT", "REVIEW_REPORT_CARDS", "PUBLISH_EXAM_RESULTS",
  "MANAGE_FEE_STRUCTURE", "RECORD_OFFLINE_FEE", "VIEW_INSTITUTIONAL_FINANCES",
  "MANAGE_INSTITUTIONAL_BILLS", "VERIFY_DOCUMENT_INCHARGE", "APPROVE_DOCUMENT_PRINCIPAL",
  "GENERATE_OFFICIAL_PDF", "MANAGE_GOVT_COMPLIANCE", "SUBMIT_ADMISSION_APPLICATION",
  "VERIFY_ADMISSION", "APPROVE_ADMISSION_ENROLL", "CAPTURE_REAL_ID_PHOTO",
  "VERIFY_ID_CARD", "APPROVE_ID_CARD", "VIEW_ID_CARD_SELF", "VIEW_ID_CARD_WARD",
  "WRITE_CLASS_DIARY", "READ_DIARY", "MANAGE_TIMETABLE", "VIEW_TIMETABLE_ASSIGNED",
  "PUBLISH_CIRCULAR", "VIEW_AUDIT_LOGS", "MANAGE_LIBRARY", "ISSUE_LIBRARY_BOOK",
  "RETURN_LIBRARY_BOOK", "VIEW_LIBRARY_CATALOG", "MANAGE_TRANSPORT",
  "VIEW_TRANSPORT_ROUTES", "UPDATE_TRANSPORT_STATUS", "VIEW_ATTENDANCE_SELF",
  "VIEW_REPORT_CARD_SELF", "VIEW_FEE_SELF"
]);

function normalizeRole(role) {
  if (!role || typeof role !== "string") return null;
  const clean = role.replace(/^ROLE_/, "").trim().toUpperCase();
  return ALLOWED_APPLICATION_ROLES.has(clean) ? clean : null;
}

// =============================================================================
// 1. AUTH TRIGGER: AUTO-PROVISION SAFE DEFAULT STUDENT CLAIMS
// =============================================================================

exports.onUserCreated = functions.auth.user().onCreate(async (user) => {
  const email = user.email || "";
  // Resolve tenant: check user custom claims or fallback to registered tenant
  const userTenant = (user.customClaims && user.customClaims.tenantId) || "DEFAULT_TENANT";
  const safeDefaultRole = "STUDENT";

  const safeClaims = {
    tenantId: userTenant,
    role: safeDefaultRole,
    permissions: ["VIEW_ATTENDANCE_SELF", "VIEW_REPORT_CARD_SELF", "VIEW_FEE_SELF"],
    isAccountDisabled: false,
    isSuperAdmin: false
  };

  await auth.setCustomUserClaims(user.uid, safeClaims);

  // Store profile in tenant user collection
  await db.doc(`tenants/${userTenant}/users/${user.uid}`).set({
    userId: user.uid,
    email: email,
    displayName: user.displayName || email.split("@")[0],
    role: safeDefaultRole,
    institutionId: userTenant,
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
  const targetTenantId = (data && data.targetTenantId) ? String(data.targetTenantId).trim() : "";

  // Validate tenant ID format to prevent path traversal or injection
  if (!targetTenantId || !/^[A-Za-z0-9_-]{3,50}$/.test(targetTenantId)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target tenant ID format.");
  }

  // 1. Verify membership in the target tenant collection
  const targetUserDoc = await db.doc(`tenants/${targetTenantId}/users/${callerUid}`).get();
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

  const validatedRole = normalizeRole(userData.role) || "STUDENT";
  const userPermissions = Array.isArray(userData.permissions)
    ? userData.permissions.filter((p) => ALLOWED_PERMISSIONS.has(p))
    : [];

  // Issue strictly controlled updated claims — NEVER copying arbitrary caller token claims!
  const updatedClaims = {
    tenantId: targetTenantId,
    role: validatedRole,
    permissions: userPermissions,
    isAccountDisabled: false,
    isSuperAdmin: isSuperAdmin
  };

  await auth.setCustomUserClaims(callerUid, updatedClaims);

  return {
    success: true,
    tenantId: targetTenantId,
    role: validatedRole,
    timestamp: Date.now()
  };
});

// =============================================================================
// 3. RBAC: PRIVILEGE ESCALATION PREVENTION & SECURE ROLE MANAGEMENT
// =============================================================================

exports.setUserRoleAndPermissions = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const callerTenantId = context.auth.token.tenantId;
  const callerRole = normalizeRole(context.auth.token.role);

  // Only Admin or Principal of that tenant can modify roles
  if (!["ADMIN", "PRINCIPAL"].includes(callerRole)) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only School Administrators or Principals may modify user roles and permissions."
    );
  }

  const { targetUserId, newRole, newPermissions } = data || {};

  if (!targetUserId || typeof targetUserId !== "string" || !newRole) {
    throw new functions.https.HttpsError("invalid-argument", "targetUserId and newRole are required.");
  }

  // Prevent self-role modification / privilege escalation
  if (targetUserId === context.auth.uid) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Security violation: Users cannot modify their own role or permissions."
    );
  }

  // Validate new role against strict allow-list
  const normalizedNewRole = normalizeRole(newRole);
  if (!normalizedNewRole) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `Invalid role '${newRole}'. Must be one of: ${Array.from(ALLOWED_APPLICATION_ROLES).join(", ")}`
    );
  }

  // Verify target user belongs to caller's tenant (prevents cross-tenant role tampering)
  const targetUserRef = db.doc(`tenants/${callerTenantId}/users/${targetUserId}`);
  const targetDoc = await targetUserRef.get();

  if (!targetDoc.exists) {
    throw new functions.https.HttpsError(
      "not-found",
      `Target user does not exist in institution '${callerTenantId}'.`
    );
  }

  // Filter permissions strictly against allowable set
  const sanitizedPermissions = Array.isArray(newPermissions)
    ? newPermissions.filter((p) => ALLOWED_PERMISSIONS.has(p))
    : [];

  // Update Firestore record
  await targetUserRef.update({
    role: normalizedNewRole,
    permissions: sanitizedPermissions,
    updatedBy: context.auth.uid,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Update Firebase Auth Custom Claims strictly without arbitrary injection
  await auth.setCustomUserClaims(targetUserId, {
    tenantId: callerTenantId,
    role: normalizedNewRole,
    permissions: sanitizedPermissions,
    isAccountDisabled: false,
    isSuperAdmin: false
  });

  // Append immutable audit log
  await db.collection(`tenants/${callerTenantId}/audit_logs`).add({
    institutionId: callerTenantId,
    action: "ROLE_CHANGE",
    entityType: "UserEntity",
    entityId: targetUserId,
    actorUserId: context.auth.uid,
    actorName: context.auth.token.name || "Administrator",
    actorRole: callerRole,
    previousState: targetDoc.data().role,
    newState: normalizedNewRole,
    timestamp: Date.now()
  });

  return { success: true, updatedRole: normalizedNewRole };
});

// =============================================================================
// 4. ACCOUNT LIFECYCLE: DISABLE ACCOUNT & REVOKE SESSIONS
// =============================================================================

exports.toggleAccountDisabled = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const callerTenantId = context.auth.token.tenantId;
  const callerRole = normalizeRole(context.auth.token.role);

  if (!["ADMIN", "PRINCIPAL"].includes(callerRole)) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized administrative action.");
  }

  const { targetUserId, disabled } = data || {};
  if (!targetUserId || typeof targetUserId !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "targetUserId is required.");
  }

  if (targetUserId === context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Cannot disable your own administrative account.");
  }

  const targetUserRef = db.doc(`tenants/${callerTenantId}/users/${targetUserId}`);
  const targetDoc = await targetUserRef.get();
  if (!targetDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Target user not found in institution.");
  }

  await targetUserRef.update({
    isAccountDisabled: Boolean(disabled),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // If disabling, update custom claims and revoke all refresh tokens immediately
  const existingClaims = (await auth.getUser(targetUserId)).customClaims || {};
  await auth.setCustomUserClaims(targetUserId, {
    ...existingClaims,
    isAccountDisabled: Boolean(disabled)
  });

  if (disabled) {
    await auth.revokeRefreshTokens(targetUserId);
  }

  return { success: true, isAccountDisabled: Boolean(disabled) };
});

// =============================================================================
// 5. SERVER-SIDE PAYMENT WEBHOOK: AUTHORITATIVE FINANCIAL LEDGER
// =============================================================================

exports.verifyPaymentWebhook = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).send("Method Not Allowed");
  }

  const signature = req.headers["x-razorpay-signature"] || req.headers["x-webhook-signature"];
  const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET || process.env.PAYMENT_WEBHOOK_SECRET;

  if (!signature || !webhookSecret) {
    return res.status(400).json({ error: "Missing signature header or unconfigured webhook secret." });
  }

  const rawBody = req.rawBody || JSON.stringify(req.body);
  const expectedSignature = crypto
    .createHmac("sha256", webhookSecret)
    .update(rawBody)
    .digest("hex");

  if (!crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expectedSignature))) {
    return res.status(401).json({ error: "Invalid webhook signature." });
  }

  const payload = req.body;
  const event = payload.event;
  const paymentEntity = payload.payload?.payment?.entity;

  if (event === "payment.captured" && paymentEntity) {
    const institutionId = paymentEntity.notes?.institutionId;
    if (!institutionId || typeof institutionId !== "string") {
      return res.status(400).json({ error: "Missing institutionId in payment notes." });
    }

    const studentId = paymentEntity.notes?.studentId;
    const invoiceId = paymentEntity.notes?.invoiceId;

    // Record verified transaction with immutable server status
    await db.collection(`tenants/${institutionId}/payment_transactions`).doc(paymentEntity.id).set({
      transactionId: paymentEntity.id,
      institutionId: institutionId,
      tenantId: institutionId,
      studentId: studentId || "UNKNOWN_STUDENT",
      feeRecordId: invoiceId || "GENERAL_FEE",
      amountPaidPaise: paymentEntity.amount,
      currency: paymentEntity.currency || "INR",
      status: "GATEWAY_VERIFIED",
      verificationSource: "GATEWAY_VERIFIED",
      paymentMode: paymentEntity.method || "GATEWAY_ONLINE",
      timestamp: Date.now()
    });
  }

  return res.status(200).json({ status: "acknowledged" });
});

// Trusted server callable for idempotent manual fee recording & balance reconciliation
exports.recordManualFeePayment = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const callerTenantId = context.auth.token.tenantId;
  const callerRole = normalizeRole(context.auth.token.role);
  if (!callerTenantId || !["ADMIN", "PRINCIPAL", "ACCOUNTANT"].includes(callerRole)) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only authorized Accountant, Principal, or Admin may record institutional fee payments."
    );
  }

  const { studentId, feeRecordId, amountPaise, paymentMode, transactionReference, receiptNumber, idempotencyKey } = data || {};
  if (!studentId || !feeRecordId || !Number.isInteger(amountPaise) || amountPaise <= 0 || !receiptNumber || !idempotencyKey) {
    throw new functions.https.HttpsError("invalid-argument", "Valid studentId, feeRecordId, integer amountPaise (>0), receiptNumber, and idempotencyKey are required.");
  }

  const ALLOWED_MANUAL = new Set(["CASH", "CHEQUE", "BANK_TRANSFER", "UPI_MANUAL", "DD"]);
  const normalizedMode = String(paymentMode || "").trim().toUpperCase();
  if (!ALLOWED_MANUAL.has(normalizedMode)) {
    throw new functions.https.HttpsError("invalid-argument", `Unsupported manual counter payment mode: ${paymentMode}`);
  }
  const cleanRef = String(transactionReference || "").trim();
  if (normalizedMode !== "CASH" && !cleanRef) {
    throw new functions.https.HttpsError("invalid-argument", `Transaction reference is required for ${normalizedMode} payments.`);
  }

  const feeRef = db.doc(`tenants/${callerTenantId}/fee_invoices/${feeRecordId}`);
  const txnCollection = db.collection(`tenants/${callerTenantId}/payment_transactions`);

  return await db.runTransaction(async (tx) => {
    const dupKeyQuery = await tx.get(txnCollection.where("idempotencyKey", "==", String(idempotencyKey)).limit(1));
    if (!dupKeyQuery.empty) {
      throw new functions.https.HttpsError("already-exists", "Duplicate payment prevented: idempotency key already committed.");
    }
    const dupReceiptQuery = await tx.get(txnCollection.where("receiptNumber", "==", String(receiptNumber)).limit(1));
    if (!dupReceiptQuery.empty) {
      throw new functions.https.HttpsError("already-exists", `Duplicate receipt number prevented: ${receiptNumber}`);
    }

    const feeDoc = await tx.get(feeRef);
    if (!feeDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Fee invoice not found in caller tenant.");
    }
    const feeData = feeDoc.data();
    if (feeData.studentId && feeData.studentId !== String(studentId)) {
      throw new functions.https.HttpsError("failed-precondition", "Fee invoice does not belong to specified student.");
    }
    const confirmedTxnsSnap = await tx.get(txnCollection.where("feeRecordId", "==", feeRecordId));
    let paidPaise = 0;
    confirmedTxnsSnap.forEach((docSnap) => {
      const d = docSnap.data();
      if (!["REFUNDED", "VOID", "FAILED"].includes(d.status)) {
        paidPaise += Number(d.amountPaidPaise || 0);
      }
    });

    const totalPaise = Number(feeData.totalAmountPaise || feeData.totalAmount || 0);
    const outstanding = Math.max(0, totalPaise - paidPaise);
    if (amountPaise > outstanding) {
      throw new functions.https.HttpsError("failed-precondition", "Payment amount exceeds current outstanding balance.");
    }

    const newPaid = paidPaise + amountPaise;
    const newDue = Math.max(0, totalPaise - newPaid);
    const newStatus = newDue === 0 ? "PAID" : "PARTIAL";
    const transactionId = `TXN-${Date.now()}-${crypto.randomBytes(3).toString("hex").toUpperCase()}`;

    tx.set(txnCollection.doc(transactionId), {
      transactionId,
      tenantId: callerTenantId,
      institutionId: callerTenantId,
      feeRecordId,
      studentId,
      amountPaidPaise: amountPaise,
      paymentMode: normalizedMode,
      transactionReference: String(transactionReference || ""),
      receiptNumber: String(receiptNumber),
      recordedByUserId: context.auth.uid,
      recordedByRole: callerRole,
      verificationSource: "MANUAL_RECORDED",
      status: "RECORDED_MANUAL",
      idempotencyKey: String(idempotencyKey),
      timestamp: Date.now(),
      serverTimestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    tx.set(feeRef, {
      paidAmountPaise: newPaid,
      dueAmountPaise: newDue,
      status: newStatus,
      lastReceiptNumber: String(receiptNumber),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    const auditRef = db.collection(`tenants/${callerTenantId}/audit_logs`).doc();
    tx.set(auditRef, {
      institutionId: callerTenantId,
      tenantId: callerTenantId,
      action: "MANUAL_FEE_PAYMENT_RECORDED",
      entityType: "FeePaymentTransaction",
      entityId: transactionId,
      actorUserId: context.auth.uid,
      actorRole: callerRole,
      timestamp: Date.now()
    });

    return {
      success: true,
      transactionId,
      receiptNumber,
      verificationSource: "MANUAL_RECORDED",
      outstandingBalancePaise: newDue
    };
  });
});

// =============================================================================
// 6. SECURE SERVER-SIDE GEMINI AI CO-PILOT INTEGRATION
// =============================================================================

exports.generateAcademicContent = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ success: false, error: "Method Not Allowed" });
  }

  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ success: false, error: "Missing or invalid Authorization header." });
  }

  const idToken = authHeader.split("Bearer ")[1];
  let decodedToken;
  try {
    decodedToken = await admin.auth().verifyIdToken(idToken);
  } catch (err) {
    return res.status(401).json({ success: false, error: "Invalid or expired Firebase ID token." });
  }

  const callerUid = decodedToken.uid;
  const callerRole = normalizeRole(decodedToken.role) || "STUDENT";
  const { prompt, systemInstruction } = req.body || {};

  if (!prompt || typeof prompt !== "string") {
    return res.status(400).json({ success: false, error: "Prompt is required." });
  }

  const serverApiKey = process.env.GEMINI_API_KEY || "";
  if (!serverApiKey) {
    return res.status(200).json({
      success: true,
      content: `[AI Academic Assistant for ${decodedToken.email || callerUid}]\nProcessed request securely via Cloud Functions backend for role ${callerRole}.\nServer environment requires GEMINI_API_KEY secret configured in Cloud Functions console.`
    });
  }

  try {
    const fetch = require("node-fetch");
    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${serverApiKey}`;
    const apiResponse = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        systemInstruction: systemInstruction ? { parts: [{ text: systemInstruction }] } : undefined,
        generationConfig: { temperature: 0.7, maxOutputTokens: 2048 }
      })
    });

    const json = await apiResponse.json();
    const candidateText = json.candidates?.[0]?.content?.parts?.[0]?.text || "";
    return res.status(200).json({ success: true, content: candidateText });
  } catch (e) {
    return res.status(500).json({ success: false, error: "Internal AI processing error." });
  }
});
