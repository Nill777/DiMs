package com.distributed_messenger.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import java.security.MessageDigest

object Signature {
    /**
     * Получает и логирует SHA-256 хэш подписи приложения
     * Использует современный API для Android 9 (API 28) и выше,
     * и сохраняет обратную совместимость со старыми версиями
     */
    fun getAppSignatureHashes(context: Context): Array<String>? {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Для Android 9 (API 28) и выше используем GET_SIGNING_CERTIFICATES
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                // Для старых версий используем устаревший флаг, обернув его для подавления предупреждения
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            // Получаем подписи из правильного поля в зависимости от версии API
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            // null-безопасность
            if (signatures.isNullOrEmpty()) {
                Logger.log("Signature", "getAppSignatureHash No signatures found!", LogLevel.ERROR)
                return null
            }
            Logger.log("Signature", "getAppSignatureHash App has Signature", LogLevel.INFO)
            return signatures.map { signature ->
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                md.digest().joinToString("") { "%02x".format(it) }
            }.toTypedArray()
        } catch (e: Exception) {
            Logger.log("Signature", "getAppSignatureHash Error getting signature", LogLevel.ERROR, e)
            return null
        }
    }
}