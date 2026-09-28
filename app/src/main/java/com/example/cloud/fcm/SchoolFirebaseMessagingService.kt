package com.example.cloud.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Production Firebase Cloud Messaging Service for School Management System.
 *
 * Implements:
 * - Token rotation callbacks (onNewToken).
 * - Multi-channel notifications with distinct importance, sounds, and priority.
 * - Deep link intent dispatching.
 * - Delivery tracking to local push_notifications table.
 */
class SchoolFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).schoolDao()
                val tokenManager = FcmDeviceTokenManager(dao)
                tokenManager.registerDeviceToken("CURRENT_USER", "ALL")
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "School Notification"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""
        val categoryStr = remoteMessage.data["category"] ?: "GENERAL_ANNOUNCEMENT"
        val deepLinkUri = remoteMessage.data["deepLink"]
        val recipientUserId = remoteMessage.data["recipientUserId"] ?: "ALL"
        val institutionId = remoteMessage.data["institutionId"] ?: "INST-SXA-2026"

        // 1. Record delivery in Room push_notifications table
        serviceScope.launch {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).schoolDao()
                val tokenManager = FcmDeviceTokenManager(dao, institutionId)
                tokenManager.recordIncomingNotification(
                    recipientUserId = recipientUserId,
                    title = title,
                    body = body,
                    category = categoryStr,
                    deepLink = deepLinkUri,
                    institutionId = institutionId
                )
            } catch (_: Exception) {}
        }

        // 2. Display heads-up Android notification
        showNotification(title, body, categoryStr, deepLinkUri)
    }

    private fun showNotification(
        title: String,
        body: String,
        categoryStr: String,
        deepLinkUri: String?
    ) {
        val channelId = when (categoryStr) {
            "EMERGENCY_NOTICE" -> CHANNEL_EMERGENCY
            "ATTENDANCE_ALERT", "HOMEWORK_ASSIGNMENT", "EXAM_RESULT", "TIMETABLE_CHANGE" -> CHANNEL_ACADEMIC
            "FEE_DUE_REMINDER" -> CHANNEL_FEES
            else -> CHANNEL_GENERAL
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            deepLinkUri?.let { data = Uri.parse(it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CHANNEL_EMERGENCY) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
        } catch (_: SecurityException) {
            // Permission POST_NOTIFICATIONS might be denied by user
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency Notices & Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical campus security, closures, and emergency broadcasts"
                enableVibration(true)
            }

            val academicChannel = NotificationChannel(
                CHANNEL_ACADEMIC,
                "Academic, Attendance & Homework",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Attendance alerts, homework updates, and exam results"
            }

            val feesChannel = NotificationChannel(
                CHANNEL_FEES,
                "Fee Billing & Finance",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tuition fee invoices, due dates, and payment receipts"
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Circulars & Announcements",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "School circulars, meetings, and holiday notices"
            }

            manager.createNotificationChannels(
                listOf(emergencyChannel, academicChannel, feesChannel, generalChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_EMERGENCY = "channel_emergency_alerts"
        const val CHANNEL_ACADEMIC = "channel_academic_notices"
        const val CHANNEL_FEES = "channel_fee_billing"
        const val CHANNEL_GENERAL = "channel_general_announcements"
    }
}
