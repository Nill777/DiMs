package com.distributed_messenger.domain.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.search.BodyTerm
import javax.mail.search.FlagTerm
import javax.mail.search.SearchTerm

class EmailService(
    // Параметры для SMTP (отправка)
    private val smtpHost: String, // smtp.yandex.ru
    private val smtpPort: String, // 465
    // Параметры для IMAP (чтение)
    private val imapHost: String, // imap.yandex.ru
    // Учетные данные
    private val username: String, // your-login@yandex.ru
    private val appPassword: String // Ваш 16-значный пароль приложения
) {

    suspend fun sendTwoFactorCode(toEmail: String, code: String) {
        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.smtp.host", smtpHost)
                put("mail.smtp.port", smtpPort)
                put("mail.smtp.auth", "true")
                put("mail.smtp.socketFactory.port", smtpPort)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(username, appPassword)
            })

            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(username))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    subject = "Your 2FA Code"
                    setText("Your two-factor authentication code is: $code")
                }
                Transport.send(message)
                println("2FA code sent successfully via Yandex to $toEmail")
            } catch (e: MessagingException) {
                e.printStackTrace()
                //            throw RuntimeException("Failed to send 2FA email via Yandex", e)
                throw e
            }
        }
    }

    suspend fun findLatestTwoFactorCode(timeoutSeconds: Int = 30): String? {
        return withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
            }
            val session = Session.getDefaultInstance(props, null)
            var store: Store? = null
            var inbox: Folder? = null

            try {
                store = session.getStore("imaps")
                store.connect(imapHost, username, appPassword)

                inbox = store.getFolder("INBOX")
                // Открываем папку, чтобы иметь возможность удалять письма
                inbox.open(Folder.READ_WRITE)

                // Проверяем почту каждую секунду в течение timeoutSeconds
                for (i in 0 until timeoutSeconds) {
                    println("Searching for UNREAD 2FA email... Attempt ${i + 1}/$timeoutSeconds")

                    // ищем все непрочитанные письма
                    val unseenMessages = inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))

                    if (unseenMessages.isNotEmpty()) {
                        // фильтр по теме и дате
                        val ourMessage = unseenMessages
                            .filter { it.subject.equals("Your 2FA Code", ignoreCase = true) }
                            .maxByOrNull { it.receivedDate }

                        if (ourMessage != null) {
                            println("Found a candidate email. Parsing content...")

                            // помечаем как прочитанное (вместо удаления)
                            // это предотвратит его повторное нахождение.
                            ourMessage.setFlag(Flags.Flag.SEEN, true)

                            // парсинг
                            val content = getTextFromMessage(ourMessage)
                            val code = Regex("\\b\\d{6}\\b").find(content)?.value

                            if (code != null) {
                                println("Successfully extracted code: $code")
                                return@withContext code
                            } else {
                                println("Warning: Found email but could not extract a 6-digit code. Content: $content")
                            }
                        }
                    }

                    Thread.sleep(1000)
                }

                // таймаут истек
                println("Error: Timed out waiting for 2FA email after $timeoutSeconds seconds.")
                return@withContext null
            } catch (e: Exception) {
                println("Error while finding 2FA email: ${e.message}")
                e.printStackTrace()
                return@withContext null
            } finally {
                // inbox?.close(true) -> `true` означает: применить все изменения
                // store?.close() -> закрывает соединение с почтовым сервером
                try {
                    inbox?.close(true)
                    store?.close()
                } catch (e: MessagingException) {
                    e.printStackTrace()
                }
            }
        }
    }
    private fun getTextFromMessage(message: Message): String {
        if (message.isMimeType("text/plain")) {
            return message.content.toString()
        }
        if (message.isMimeType("multipart/*")) {
            val mimeMultipart = message.content as MimeMultipart
            for (i in 0 until mimeMultipart.count) {
                val bodyParser = mimeMultipart.getBodyPart(i)
                if (bodyParser.isMimeType("text/plain")) {
                    return bodyParser.content.toString()
                }
            }
        }
        // Если не нашли text/plain, возвращаем сырое содержимое
        return message.content.toString()
    }
}