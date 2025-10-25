package com.distributedMessenger.util

import android.content.Context
import kotlin.text.isEmpty

object CppBridge {
    init {
        System.loadLibrary("native-lib")
    }
    private external fun getPepperFromNdk(appSignatureHash: String): String

    fun getPepper(context: Context): String {
        val currentSignatureHashes = Signature.getAppSignatureHashes(context)
        if (currentSignatureHashes.isNullOrEmpty()) {
            throw SecurityException("Could not retrieve app signatures. The APK might be corrupted")
        }
        val sortedConcatenatedHash = currentSignatureHashes.sorted().joinToString("")
        val pepper = getPepperFromNdk(sortedConcatenatedHash)

        if (pepper.isEmpty()) {
            throw SecurityException("Security checks failed. Tampered, insecure, or unsigned environment detected")
        }
        return pepper
    }
}
