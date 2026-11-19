
package com.example.applocker.auth

import android.content.Context
import androidx.biometric.BiometricManager
import com.example.applocker.security.SecureStorage

class AuthenticationManager(context: Context) {

    private val appContext = context.applicationContext
    private val secureStorage = SecureStorage(appContext)

    private val preferences = appContext.getSharedPreferences(
        "app_locker_settings",
        Context.MODE_PRIVATE
    )

    enum class AuthType {
        PIN,
        PASSWORD,
        PATTERN,
        BIOMETRIC
    }

    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val COOLDOWN_MILLIS = 30_000L
    }

    fun setupPin(pin: String): Boolean {
        if (!pin.matches(Regex("\\d{4,8}"))) return false

        secureStorage.saveSecret("pin", pin)
        return true
    }

    fun verifyPin(pin: String): Boolean {
        return secureStorage.verifySecret("pin", pin)
    }

    fun setupPassword(password: String): Boolean {
        if (password.length < 6 || password.length > 64) return false

        secureStorage.saveSecret("password", password)
        return true
    }

    fun verifyPassword(password: String): Boolean {
        return secureStorage.verifySecret("password", password)
    }

    fun setupPattern(pattern: String): Boolean {
        val dots = pattern.split("-")

        if (dots.size < 4 || dots.size > 9) return false
        if (dots.any { it.toIntOrNull() !in 1..9 }) return false
        if (dots.distinct().size != dots.size) return false

        secureStorage.saveSecret("pattern", pattern)
        return true
    }

    fun verifyPattern(pattern: String): Boolean {
        return secureStorage.verifySecret("pattern", pattern)
    }

    fun hasPin(): Boolean = secureStorage.hasSecret("pin")

    fun hasPassword(): Boolean = secureStorage.hasSecret("password")

    fun hasPattern(): Boolean = secureStorage.hasSecret("pattern")

    fun setPreferredMethod(type: AuthType) {
        preferences.edit()
            .putString("preferred_auth", type.name)
            .apply()
    }

    fun getPreferredMethod(): AuthType {
        val saved = preferences.getString("preferred_auth", null)

        return runCatching {
            AuthType.valueOf(saved ?: "PIN")
        }.getOrDefault(AuthType.PIN)
    }

    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(appContext)

        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Returns remaining cooldown in seconds.
    fun getCooldownRemainingSeconds(): Long {
        val lockoutUntil = preferences.getLong("lockout_until", 0L)
        val remaining = lockoutUntil - System.currentTimeMillis()

        if (remaining <= 0L && lockoutUntil != 0L) {
            preferences.edit()
                .remove("lockout_until")
                .putInt("failed_attempts", 0)
                .apply()
        }

        return if (remaining > 0L) {
            (remaining + 999L) / 1000L
        } else {
            0L
        }
    }

    // Call after an incorrect PIN or password.
    // Returns remaining cooldown seconds, or 0 if no cooldown started.
    fun recordFailedAttempt(): Long {

        val remaining = getCooldownRemainingSeconds()
        if (remaining > 0L) return remaining

        val attempts = preferences.getInt("failed_attempts", 0) + 1

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            preferences.edit()
                .putLong(
                    "lockout_until",
                    System.currentTimeMillis() + COOLDOWN_MILLIS
                )
                .putInt("failed_attempts", 0)
                .apply()

            return COOLDOWN_MILLIS / 1000L
        }

        preferences.edit()
            .putInt("failed_attempts", attempts)
            .apply()

        return 0L
    }

    fun resetFailedAttempts() {
        preferences.edit()
            .putInt("failed_attempts", 0)
            .remove("lockout_until")
            .apply()
    }
}
