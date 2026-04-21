package com.example.eventnote.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 密码管理器
 * 用于设置和验证清除数据的密码
 */
@Singleton
class PasswordManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "eventnote_secure_prefs"
        private const val KEY_PASSWORD_HASH = "clear_password_hash"
        private const val KEY_PASSWORD_SET = "password_is_set"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 是否已设置密码
     */
    fun isPasswordSet(): Boolean {
        return prefs.getBoolean(KEY_PASSWORD_SET, false)
    }
    
    /**
     * 设置密码
     */
    fun setPassword(password: String): Boolean {
        if (password.length < 4) {
            return false // 密码至少4位
        }
        
        val hash = hashPassword(password)
        prefs.edit()
            .putString(KEY_PASSWORD_HASH, hash)
            .putBoolean(KEY_PASSWORD_SET, true)
            .apply()
        
        return true
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(password: String): Boolean {
        if (!isPasswordSet()) {
            return true // 没有设置密码时，任何密码都可以
        }
        
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val inputHash = hashPassword(password)
        
        return storedHash == inputHash
    }
    
    /**
     * 清除密码
     */
    fun clearPassword() {
        prefs.edit()
            .remove(KEY_PASSWORD_HASH)
            .putBoolean(KEY_PASSWORD_SET, false)
            .apply()
    }
    
    /**
     * 验证密码并返回结果
     */
    fun validatePassword(password: String): PasswordValidationResult {
        return when {
            !isPasswordSet() -> PasswordValidationResult.NO_PASSWORD_SET
            password.isEmpty() -> PasswordValidationResult.EMPTY_PASSWORD
            verifyPassword(password) -> PasswordValidationResult.SUCCESS
            else -> PasswordValidationResult.WRONG_PASSWORD
        }
    }
    
    /**
     * MD5 哈希 (实际应用中应该使用更安全的哈希算法)
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * 密码验证结果
 */
enum class PasswordValidationResult {
    SUCCESS,           // 验证成功
    WRONG_PASSWORD,   // 密码错误
    NO_PASSWORD_SET,  // 未设置密码
    EMPTY_PASSWORD    // 密码为空
}
