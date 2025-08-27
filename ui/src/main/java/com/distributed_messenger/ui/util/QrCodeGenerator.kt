package com.distributed_messenger.ui.util

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.set
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {

    /**
     * Генерирует QR-код из строки и возвращает его как ImageBitmap для Compose.
     * @param content Строка, которую нужно закодировать.
     * @param size Размер изображения в пикселях.
     * @return ImageBitmap или null в случае ошибки.
     */
    fun generateQrCode(content: String, size: Int): ImageBitmap? {
        Logger.log("QrCodeGenerator", "generating QRCode")
        var result: ImageBitmap?
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            result = bitmap.asImageBitmap()
        } catch (e: Exception) {
            Logger.log("QrCodeGenerator", "error generating QRCode", LogLevel.ERROR, e)
            result = null
        }
        return result
    }
}