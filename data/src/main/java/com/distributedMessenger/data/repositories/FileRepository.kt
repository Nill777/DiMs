package com.distributedMessenger.data.repositories

import com.distributedMessenger.core.File
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.logger.LoggingWrapper
import com.distributedMessenger.data.irepositories.IFileRepository
import com.distributedMessenger.data.local.dao.FileDao
import com.distributedMessenger.data.local.entities.FileEntity
import java.util.UUID

class FileRepository(private val fileDao: FileDao) : IFileRepository {
    private val loggingWrapper = LoggingWrapper(
        origin = this,
        logger = Logger,
        tag = "FileRepository"
    )

    override suspend fun getFile(id: UUID): File? =
        loggingWrapper {
            fileDao.getFileById(id)?.toDomain()
        }

    override suspend fun getAllFiles(): List<File> =
        loggingWrapper {
            fileDao.getAllFiles().map { it.toDomain() }
        }

    override suspend fun getFilesByUser(userId: UUID): List<File> =
        loggingWrapper {
            fileDao.getFilesByUserId(userId).map { it.toDomain() }
        }

    override suspend fun addFile(file: File): UUID =
        loggingWrapper {
            val rowId = fileDao.insertFile(file.toEntity())
            check(rowId != -1L) { "Failed to insert file" }
            file.id
        }

    override suspend fun updateFile(file: File): Boolean =
        loggingWrapper {
            fileDao.updateFile(file.toEntity()) > 0
        }

    override suspend fun deleteFile(id: UUID): Boolean =
        loggingWrapper {
            fileDao.deleteFile(id) > 0
        }

    private fun File.toEntity(): FileEntity {
        return FileEntity(
            id = id,
            name = name,
            type = type,
            path = path,
            uploadedBy = uploadedBy,
            uploadedTimestamp = uploadedTimestamp
        )
    }

    private fun FileEntity.toDomain(): File {
        return File(
            id = id,
            name = name,
            type = type,
            path = path,
            uploadedBy = uploadedBy,
            uploadedTimestamp = uploadedTimestamp
        )
    }
}
