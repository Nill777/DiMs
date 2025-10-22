package com.distributed_messenger.domain.iservices

import com.distributed_messenger.core.User
import com.distributed_messenger.core.UserRole
import com.distributed_messenger.domain.models.LoginResult
import java.util.UUID

interface IUserService {
    suspend fun register(username: String, role: UserRole, password: String): UUID
    suspend fun login(username: String, password: String): LoginResult
    suspend fun changePassword(userId: UUID, oldPassword: String, newPassword: String): Boolean
    suspend fun unlockUser(userId: UUID): Boolean
    suspend fun findByUserName(username: String): User?
    suspend fun getUser(id: UUID): User?
    suspend fun getAllUsers(): List<User>
    suspend fun updateUser(id: UUID, username: String): Boolean
    suspend fun updateUserRole(id: UUID, newRole: UserRole): Boolean
    suspend fun deleteUser(id: UUID): Boolean
}
