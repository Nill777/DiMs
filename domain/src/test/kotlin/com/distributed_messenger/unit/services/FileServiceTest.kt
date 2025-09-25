package com.distributed_messenger.unit.services


import com.distributed_messenger.data.irepositories.IFileRepository
import com.distributed_messenger.domain.iservices.IFileService
import com.distributed_messenger.domain.services.FileService
import com.distributed_messenger.TestObjectMother
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.*

class FileServiceTest {
    @MockK // Создаем мок-объект для репозитория
    private lateinit var mockFileRepository: IFileRepository
    private lateinit var fileService: IFileService
    // Fixture - выполняется перед каждым тестом (Требование ЛР №4)
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fileService = FileService(mockFileRepository)
    }

    @Test
    fun `uploadFile should call repository and return new file id on success`() = runTest {
        // Arrange
        val expectedFileId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val fileName = "test.txt"
        val fileType = "text/plain"
        val filePath = "/data/file.txt"
        coEvery { mockFileRepository.addFile(any()) } returns expectedFileId

        // Act
        val result = fileService.uploadFile(fileName, fileType, filePath, userId)

        // Assert
        assertEquals(expectedFileId, result, "Метод должен возвращать ID, полученный от репозитория")

        // Проверяем, что метод репозитория был вызван ровно 1 раз с корректными параметрами
        coVerify(exactly = 1) {
            mockFileRepository.addFile(match {
                it.name == fileName &&
                        it.type == fileType &&
                        it.path == filePath &&
                        it.uploadedBy == userId
            })
        }
    }

    @Test
    fun `uploadFile should propagate exception when repository throws`() = runTest {
        // Arrange
        val errorMessage = "Failed to insert file into database"
        val repositoryException = Exception(errorMessage)
        coEvery { mockFileRepository.addFile(any()) } throws repositoryException

        // Act & Assert
        val thrownException = assertFailsWith<Exception> {
            fileService.uploadFile("test.txt", "text/plain", "/data/file.txt", UUID.randomUUID())
        }
        assertEquals(errorMessage, thrownException.message, "Сервис должен пробрасывать исключение от репозитория")
    }

    @Test
    fun `getFile should return file when file exists`() = runTest {
        // Arrange
        val fileId = UUID.randomUUID()
        // Используем TestObjectMother для создания консистентных тестовых данных (Требование ЛР №7)
        val expectedFile = TestObjectMother.createFile(id = fileId, uploaderId = UUID.randomUUID())
        coEvery { mockFileRepository.getFile(fileId) } returns expectedFile

        // Act
        val result = fileService.getFile(fileId)

        // Assert
        assertNotNull(result)
        assertEquals(expectedFile, result, "Сервис должен возвращать файл, полученный от репозитория")
        coVerify(exactly = 1) { mockFileRepository.getFile(fileId) }
    }
    @Test
    fun `getFile should return null when file does not exist`() = runTest {
        // Arrange
        val fileId = UUID.randomUUID()
        coEvery { mockFileRepository.getFile(fileId) } returns null

        // Act
        val result = fileService.getFile(fileId)

        // Assert
        assertNull(result, "Сервис должен возвращать null, если репозиторий вернул null")
    }

    @Test
    fun `getUserFiles should return a list of files for a user`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        val expectedFiles = listOf(
            TestObjectMother.createFile(uploaderId = userId),
            TestObjectMother.createFile(uploaderId = userId)
        )
        coEvery { mockFileRepository.getFilesByUser(userId) } returns expectedFiles

        // Act
        val result = fileService.getUserFiles(userId)

        // Assert
        assertEquals(2, result.size)
        assertEquals(expectedFiles, result)
    }
    @Test
    fun `getUserFiles should return an empty list when user has no files`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        coEvery { mockFileRepository.getFilesByUser(userId) } returns emptyList()

        // Act
        val result = fileService.getUserFiles(userId)

        // Assert
        assertNotNull(result)
        assertTrue(result.isEmpty(), "Сервис должен возвращать пустой список, если у пользователя нет файлов")
    }

    @Test
    fun `deleteFile should return true on successful deletion`() = runTest {
        // Arrange
        val fileId = UUID.randomUUID()
        coEvery { mockFileRepository.deleteFile(fileId) } returns true

        // Act
        val result = fileService.deleteFile(fileId)

        // Assert
        assertTrue(result, "Сервис должен возвращать true при успешном удалении")
        coVerify(exactly = 1) { mockFileRepository.deleteFile(fileId) }
    }
    @Test
    fun `deleteFile should return false on failed deletion`() = runTest {
        // Arrange
        val fileId = UUID.randomUUID()
        coEvery { mockFileRepository.deleteFile(fileId) } returns false

        // Act
        val result = fileService.deleteFile(fileId)

        // Assert
        assertFalse(result, "Сервис должен возвращать false, если удаление в репозитории не удалось")
    }
}