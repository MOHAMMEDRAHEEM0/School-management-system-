package com.example.di

import android.content.Context
import com.example.cloud.dr.DisasterRecoveryManager
import com.example.cloud.fcm.FcmDeviceTokenManager
import com.example.cloud.storage.CloudDocumentStorageService
import com.example.data.academic.AcademicSessionManager
import com.example.data.local.dao.SchoolDao
import com.example.data.remote.sync.CloudSyncManager
import com.example.data.remote.sync.DurableOutboxManager
import com.example.data.remote.sync.NetworkConnectivityMonitor
import com.example.data.repository.AiAcademicAssistant
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth? {
        return runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }

    @Provides
    @Singleton
    fun provideAiAcademicAssistant(): AiAcademicAssistant {
        return AiAcademicAssistant()
    }

    @Provides
    @Singleton
    fun provideCloudSyncManager(dao: SchoolDao): CloudSyncManager {
        return CloudSyncManager(dao)
    }

    @Provides
    @Singleton
    fun provideNetworkConnectivityMonitor(@ApplicationContext context: Context): NetworkConnectivityMonitor {
        return NetworkConnectivityMonitor(context)
    }

    @Provides
    @Singleton
    fun provideDurableOutboxManager(
        dao: SchoolDao,
        monitor: NetworkConnectivityMonitor
    ): DurableOutboxManager {
        return DurableOutboxManager(dao, monitor)
    }

    @Provides
    @Singleton
    fun provideAcademicSessionManager(dao: SchoolDao): AcademicSessionManager {
        return AcademicSessionManager(dao)
    }

    @Provides
    @Singleton
    fun provideFcmDeviceTokenManager(dao: SchoolDao): FcmDeviceTokenManager {
        return FcmDeviceTokenManager(dao)
    }

    @Provides
    @Singleton
    fun provideCloudDocumentStorageService(dao: SchoolDao): CloudDocumentStorageService {
        return CloudDocumentStorageService(dao)
    }

    @Provides
    @Singleton
    fun provideDisasterRecoveryManager(dao: SchoolDao): DisasterRecoveryManager {
        return DisasterRecoveryManager(dao)
    }

    @Provides
    @Singleton
    fun providePaymentGatewayService(dao: SchoolDao): com.example.payment.PaymentGatewayService {
        return com.example.payment.PaymentGatewayService(dao)
    }

    @Provides
    @Singleton
    fun provideAutomationDispatchEngine(dao: SchoolDao): com.example.whatsapp.automation.AutomationDispatchEngine {
        return com.example.whatsapp.automation.AutomationDispatchEngine(dao)
    }

    @Provides
    @Singleton
    fun provideImmutableAuditLoggingService(dao: SchoolDao): com.example.security.audit.ImmutableAuditLoggingService {
        return com.example.security.audit.ImmutableAuditLoggingService(dao)
    }
}

