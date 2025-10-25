package com.distributedMessenger.data.irepositories

import com.distributedMessenger.core.File
import java.util.UUID

interface IFileRepository {
    suspend fun getFile(id: UUID): File?
    suspend fun getAllFiles(): List<File>
    suspend fun getFilesByUser(userId: UUID): List<File>
    suspend fun addFile(file: File): UUID
    suspend fun updateFile(file: File): Boolean
    suspend fun deleteFile(id: UUID): Boolean
}
