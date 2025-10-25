package com.distributedMessenger.data.irepositories

import com.distributedMessenger.core.User
import java.util.UUID

interface IUserRepository {
    suspend fun getUser(id: UUID): User?
    suspend fun getAllUsers(): List<User>
    suspend fun findByUsername(username: String): User?
    suspend fun addUser(user: User): UUID
    suspend fun updateUser(user: User): Boolean
    suspend fun deleteUser(id: UUID): Boolean
}
