package com.example.data.repository

import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.TeacherEntity
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    val allUsers: Flow<List<UserEntity>>
    val usersState: StateFlow<List<UserEntity>>
    val allTeachers: Flow<List<TeacherEntity>>
    val teachersState: StateFlow<List<TeacherEntity>>

    fun observeUsers(): Flow<List<UserEntity>>
    fun observeUsersByRole(role: String): Flow<List<UserEntity>>
    fun observeTeachers(): Flow<List<TeacherEntity>>
    suspend fun getUserById(userId: String): UserEntity?
    suspend fun insertUser(user: UserEntity)
    suspend fun insertUsers(users: List<UserEntity>)
    suspend fun getTeacherById(teacherId: String): TeacherEntity?
    suspend fun insertTeacher(teacher: TeacherEntity)
    suspend fun insertTeachers(teachers: List<TeacherEntity>)
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dao: SchoolDao
) : UserRepository {
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    override val usersState: StateFlow<List<UserEntity>> = allUsers
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override val allTeachers: Flow<List<TeacherEntity>> = dao.getAllTeachers()
    override val teachersState: StateFlow<List<TeacherEntity>> = allTeachers
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override fun observeUsers(): Flow<List<UserEntity>> = dao.getAllUsers()
    override fun observeUsersByRole(role: String): Flow<List<UserEntity>> = dao.getUsersByRole(role)
    override fun observeTeachers(): Flow<List<TeacherEntity>> = dao.getAllTeachers()

    override suspend fun getUserById(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        dao.getUserById(userId)
    }

    override suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    override suspend fun insertUsers(users: List<UserEntity>) = withContext(Dispatchers.IO) {
        dao.insertUsers(users)
    }

    override suspend fun getTeacherById(teacherId: String): TeacherEntity? = withContext(Dispatchers.IO) {
        dao.getTeacherById(teacherId)
    }

    override suspend fun insertTeacher(teacher: TeacherEntity) = withContext(Dispatchers.IO) {
        dao.insertTeacher(teacher)
    }

    override suspend fun insertTeachers(teachers: List<TeacherEntity>) = withContext(Dispatchers.IO) {
        dao.insertTeachers(teachers)
    }
}
