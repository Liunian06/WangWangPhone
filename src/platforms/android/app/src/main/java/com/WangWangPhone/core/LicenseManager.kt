package com.WangWangPhone.core

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 授权管理器 - Android 平台封装
 * 
 * 该类封装了与 C++ Core LicenseManager 的交互。
 * 在完整实现中，应通过 JNI 调用 C++ 代码。
 * 当前版本使用 SQLite 进行本地持久化。
 */
class LicenseManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var instance: LicenseManager? = null
        
        fun getInstance(context: Context): LicenseManager {
            return instance ?: synchronized(this) {
                instance ?: LicenseManager(context.applicationContext).also { instance = it }
            }
        }
        
        // JNI 加载（当 C++ 库就绪时启用）
        // init {
        //     System.loadLibrary("wwj_core")
        // }
    }
    
    private val dbHelper = LicenseDbHelper(context)
    private var cachedLicense: LicenseRecord? = null
    private var lastCheckTime: Long = 0
    
    /**
     * 初始化授权管理器
     */
    fun initialize(): Boolean {
        // 尝试从数据库恢复授权
        cachedLicense = dbHelper.getLicenseRecord()
        
        // 启动时执行一次检查
        if (cachedLicense != null) {
            checkLicenseDaily()
        }
        
        return true
    }
    
    /**
     * 获取设备机器码
     */
    fun getMachineId(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return "AND-$androidId"
    }
    
    /**
     * 验证并激活授权码
     */
    suspend fun verifyLicense(licenseKey: String): LicenseResult = withContext(Dispatchers.IO) {
        try {
            // 检查格式
            if (!licenseKey.startsWith("WANGWANG-")) {
                return@withContext LicenseResult.Error("激活码格式无效")
            }
            
            // 解析激活码
            val payload = parseLicenseKey(licenseKey)
                ?: return@withContext LicenseResult.Error("激活码解析失败")
            
            // 验证机器码
            val currentMachineId = getMachineId()
            if (payload.machineId != currentMachineId) {
                return@withContext LicenseResult.Error("机器码不匹配")
            }
            
            // 验证过期时间
            val now = System.currentTimeMillis() / 1000
            if (payload.expirationTime < now) {
                return@withContext LicenseResult.Error("授权已过期")
            }
            
            // 保存到数据库
            val record = LicenseRecord(
                licenseKey = licenseKey,
                machineId = payload.machineId,
                expirationTime = payload.expirationTime,
                licenseType = payload.type,
                activationTime = now
            )
            
            if (dbHelper.saveLicenseRecord(record)) {
                cachedLicense = record
                LicenseResult.Success(record)
            } else {
                LicenseResult.Error("保存授权信息失败")
            }
        } catch (e: Exception) {
            LicenseResult.Error("激活失败: ${e.message}")
        }
    }
    
    /**
     * 检查是否已激活
     */
    fun isActivated(): Boolean {
        val license = cachedLicense ?: dbHelper.getLicenseRecord() ?: return false
        val now = System.currentTimeMillis() / 1000
        return license.expirationTime > now
    }
    
    /**
     * 检查授权是否过期
     */
    fun isExpired(): Boolean {
        val license = cachedLicense ?: return true
        val now = System.currentTimeMillis() / 1000
        return license.expirationTime <= now
    }
    
    /**
     * 获取剩余天数
     */
    fun getRemainingDays(): Int {
        val license = cachedLicense ?: return 0
        val now = System.currentTimeMillis() / 1000
        val remaining = license.expirationTime - now
        return if (remaining > 0) (remaining / (24 * 60 * 60)).toInt() else 0
    }
    
    /**
     * 获取过期日期字符串
     */
    fun getExpirationDateString(): String {
        val license = cachedLicense ?: return "未激活"
        val date = Date(license.expirationTime * 1000)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * 获取授权类型
     */
    fun getLicenseType(): String {
        return cachedLicense?.licenseType ?: "free"
    }
    
    /**
     * 清除授权信息
     */
    fun clearLicense(): Boolean {
        cachedLicense = null
        return dbHelper.clearLicenseRecord()
    }

    /**
     * 每日检查授权逻辑
     * 如果过期或验证失败，将重置授权状态
     */
    fun checkLicenseDaily(): Boolean {
        val license = cachedLicense ?: return false
        val now = System.currentTimeMillis() / 1000

        // 每天只检查一次 (86400秒)
        if (lastCheckTime > 0 && (now - lastCheckTime) < 86400) {
            return true
        }
        
        // 1. 验证机器码
        val currentMachineId = getMachineId()
        if (license.machineId != currentMachineId) {
            clearLicense()
            return false
        }
        
        // 2. 验证过期时间
        if (license.expirationTime < now) {
            clearLicense()
            return false
        }
        
        // 3. 验证签名 (重新解析)
        // 在实际生产中，这里应该调用 C++ 层或原生 RSA 验证
        // 这里仅做简单模拟
        if (parseLicenseKey(license.licenseKey) == null) {
             clearLicense()
             return false
        }
        
        lastCheckTime = now
        return true
    }
    
    /**
     * 解析激活码 (模拟实现)
     * TODO: 当 C++ Core 就绪后，通过 JNI 调用真正的 RSA 验签
     */
    private fun parseLicenseKey(licenseKey: String): LicensePayload? {
        try {
            // 格式: WANGWANG-[Payload-Base64].[Signature-Base64]
            val rest = licenseKey.removePrefix("WANGWANG-")
            val parts = rest.split(".")
            if (parts.size != 2) return null
            
            val payloadBase64 = parts[0]
            // val signatureBase64 = parts[1]
            
            // TODO: 验证签名
            // TODO: Base64 解码并解析 JSON
            
            // 模拟解析 - 使用当前时间 + 365天作为过期时间
            val now = System.currentTimeMillis() / 1000
            val expiration = now + (365 * 24 * 60 * 60)
            
            return LicensePayload(
                machineId = getMachineId(),
                expirationTime = expiration,
                type = "pro",
                salt = "generated"
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    // JNI 方法声明（当 C++ 库就绪时启用）
    // private external fun nativeInitialize(dbPath: String): Boolean
    // private external fun nativeVerifyLicense(licenseKey: String, machineId: String): Boolean
    // private external fun nativeRestoreLicense(machineId: String): Boolean
    // private external fun nativeIsActivated(): Boolean
    // private external fun nativeGetExpirationTime(): Long
}

/**
 * 授权记录数据类
 */
data class LicenseRecord(
    val licenseKey: String,
    val machineId: String,
    val expirationTime: Long,
    val licenseType: String,
    val activationTime: Long
)

/**
 * 授权载荷数据类
 */
data class LicensePayload(
    val machineId: String,
    val expirationTime: Long,
    val type: String,
    val salt: String
)

/**
 * 授权操作结果
 */
sealed class LicenseResult {
    data class Success(val record: LicenseRecord) : LicenseResult()
    data class Error(val message: String) : LicenseResult()
}