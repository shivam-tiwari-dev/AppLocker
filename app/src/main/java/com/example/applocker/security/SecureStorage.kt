
package com.example.applocker.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecureStorage(context: Context) {

    private val preferences = androidx.security.crypto.EncryptedSharedPreferences.create(
        "app_locker_secure",
        androidx.security.crypto.MasterKeys.getOrCreate(
            androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
        ),
        context,
        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSecret(name: String, secret: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val spec = PBEKeySpec(secret.toCharArray(), salt, 120000, 256)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded

        preferences.edit()
            .putString("${name}_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("${name}_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()

        spec.clearPassword()
    }

    fun verifySecret(name: String, secret: String): Boolean {
        val saltText = preferences.getString("${name}_salt", null) ?: return false
        val hashText = preferences.getString("${name}_hash", null) ?: return false

        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        val expectedHash = Base64.decode(hashText, Base64.NO_WRAP)

        val spec = PBEKeySpec(secret.toCharArray(), salt, 120000, 256)
        val actualHash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded

        spec.clearPassword()

        return java.security.MessageDigest.isEqual(expectedHash, actualHash)
    }

    fun hasSecret(name: String): Boolean {
        return preferences.contains("${name}_hash")
    }
}
