package com.distributed_messenger.domain.util

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

object PasswordHasher {
    private const val ITERATIONS = 5
    private const val MEMORY_COST_KB = 40 * 1024
    private const val PARALLELISM = 1
    private const val SALT_LENGTH = 256
    private const val HASH_LENGTH = 512

    fun hashPassword(password: String, pepper: String): String {
        val salt = generateSalt()
        val passwordWithPepper = (password + pepper).toByteArray()

        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(ITERATIONS)
            .withMemoryAsKB(MEMORY_COST_KB)
            .withParallelism(PARALLELISM)
            .withSalt(salt)

        val generator = Argon2BytesGenerator()
        generator.init(builder.build())

        val hash = ByteArray(HASH_LENGTH)
        generator.generateBytes(passwordWithPepper, hash, 0, hash.size)

        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(hash)
//        "\$argon2id\$i=$ITERATIONS\$m=$MEMORY_COST_KB\$p=$PARALLELISM$$saltB64$$hashB64"
        return "argon2id,i=$ITERATIONS,m=$MEMORY_COST_KB,p=$PARALLELISM,$saltB64,$hashB64"
    }

    fun verifyPassword(password: String, storedHash: String, pepper: String): Boolean {
        return try {
            val parts = storedHash.split(",")
            if (parts.size != 6) return false

            val iterations = parts[1].substringAfter('=').toInt()
            val memory = parts[2].substringAfter('=').toInt()
            val parallelism = parts[3].substringAfter('=').toInt()
            val saltB64 = parts[4]
            val hashB64 = parts[5]

            val salt = Base64.getDecoder().decode(saltB64)
            val storedHashBytes = Base64.getDecoder().decode(hashB64)
            val passwordWithPepper = (password + pepper).toByteArray()

            val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memory)
                .withParallelism(parallelism)
                .withSalt(salt)

            val generator = Argon2BytesGenerator()
            generator.init(builder.build())

            val newHash = ByteArray(storedHashBytes.size)
            generator.generateBytes(passwordWithPepper, newHash, 0, newHash.size)

            MessageDigest.isEqual(storedHashBytes, newHash)
        } catch (e: Exception) {
            // TODO Защита от неверного формата хэша
            false
        }
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
}