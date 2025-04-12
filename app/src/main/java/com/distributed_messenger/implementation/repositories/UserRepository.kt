package com.distributed_messenger.implementation.repositories

import com.distributed_messenger.domain.models.User
import com.distributed_messenger.domain.repositories.IUserRepository
import com.distributed_messenger.data.local.dao.UserDao
import com.distributed_messenger.data.local.entities.UserEntity
import java.util.UUID

class UserRepository(private val userDao: UserDao) : IUserRepository {
    override suspend fun getUser(id: UUID): User? {
        return userDao.getUserById(id)?.toDomain()
    }

    override suspend fun getAllUsers(): List<User> {
        return userDao.getAllUsers().map { it.toDomain() }
    }

    override suspend fun addUser(user: User): UUID {
        return userDao.insertUser(user.toEntity())
    }

    override suspend fun updateUser(user: User): Boolean {
        return userDao.updateUser(user.toEntity()) > 0
    }

    override suspend fun deleteUser(id: UUID): Boolean {
        return userDao.deleteUser(id) > 0
    }

    private fun User.toEntity(): UserEntity {
        return UserEntity(
            id = id,
            username = username,
            role = role
        )
    }

    private fun UserEntity.toDomain(): User {
        return User(
            id = id,
            username = username,
            role = role
        )
    }
}