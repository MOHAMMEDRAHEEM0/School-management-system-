package com.example.di

import com.example.data.repository.AttendanceRepository
import com.example.data.repository.AttendanceRepositoryImpl
import com.example.data.repository.AuditLoggerRepository
import com.example.data.repository.AuditLoggerRepositoryImpl
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.UserRepository
import com.example.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuditLoggerRepository(impl: AuditLoggerRepositoryImpl): AuditLoggerRepository
}
