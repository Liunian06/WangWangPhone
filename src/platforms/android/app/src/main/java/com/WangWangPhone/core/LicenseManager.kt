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
     * 每次启动都会从数据库恢复授权并强制验证 RSA 签名
     */
    fun initialize(): Boolean {
        // 尝试从数据库恢复授权记录
        val record = dbHelper.getLicenseRecord()
        
        if (record != null) {
            // 强制验证：每次启动都重新验证 RSA 签名
            val isValid = validateLicenseOnStartup(record)
            if (!isValid) {
                // RSA 签名验证失败，清除无效的授权记录
                android.util.Log.w("LicenseManager", "启动验证失败：RSA签名无效或授权已过期，清除授权")
                clearLicense()
            }
        } else {
            cachedLicense = null
        }
        
        return true
    }
    
    /**
     * 启动时验证授权记录的完整性
     * 包括：RSA 签名验证 + 机器码匹配 + 过期时间检查
     * 
     * @return true 验证通过，false 验证失败
     */
    private fun validateLicenseOnStartup(record: LicenseRecord): Boolean {
        // 1. 验证机器码
        val currentMachineId = getMachineId()
        if (record.machineId != currentMachineId) {
            android.util.Log.w("LicenseManager", "启动验证失败：机器码不匹配")
            return false
        }
        
        // 2. 验证过期时间
        val now = System.currentTimeMillis() / 1000
        if (record.expirationTime < now) {
            android.util.Log.w("LicenseManager", "启动验证失败：授权已过期")
            return false
        }
        
        // 3. 强制验证 RSA 签名（核心安全检查）
        val payload = parseLicenseKey(record.licenseKey)
        if (payload == null) {
            android.util.Log.w("LicenseManager", "启动验证失败：RSA签名验证无效")
            return false
        }
        
        // 4. 验证签名中的载荷与数据库记录是否一致
        if (payload.machineId != record.machineId || payload.expirationTime != record.expirationTime) {
            android.util.Log.w("LicenseManager", "启动验证失败：载荷数据与数据库记录不一致")
            return false
        }
        
        // 验证通过，更新缓存
        cachedLicense = record
        android.util.Log.i("LicenseManager", "启动验证通过，授权有效，剩余 ${getRemainingDays()} 天")
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
                LicenseResult.Success(record, needsRestart = true)
            } else {
                LicenseResult.Error("保存授权信息失败")
            }
        } catch (e: Exception) {
            LicenseResult.Error("激活失败: ${e.message}")
        }
    }
    
    /**
     * 检查是否已激活
     * 注意：仅依赖缓存状态，不直接读取数据库
     * 缓存仅在 initialize() 或 verifyLicense() 中通过 RSA 签名验证后设置
     * 这确保了更换公钥后旧授权无法通过验证，从而正确失效
     */
    fun isActivated(): Boolean {
        val license = cachedLicense ?: return false
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
     * RSA 公钥 (SPKI 格式 Base64)
     * 2026-02-10: 更新公钥
     */
    private val publicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxQxw4O380suUJS1ibRjKiX59SVqfUh4ao7/t+lXFaHEPDfL19vgmNaZGFY6pBkLuRZdGqkyiFmNFyWLH6VQf9kmhwL6HO3Qie//9jGIJMMohcPcNVz/cFOfnT1ojYrh+6Q2tODzLDm9EQG669ketzCdC3TynjtbzzyXY+JoL85L1MIhtsqAUFbBd4uAEG16z+OmT4BPi1UdPIKgVt7PdxqLtww2v7t60XwB1MiNo0GIDjhZHH9k1Mbu/IWZcW6pXgCaE+5rxG47gADN384n3zhLot/CbR5aYA0vnheQipjRG8oe4YTApGQ2rFvF+yUYXzcGOJFYkl8CvvPXXw8rFLQIDAQAB"

    /**
     * 解析并验证激活码
     * 使用 RSA 公钥验证签名，确保激活码的真实性
     */
    private fun parseLicenseKey(licenseKey: String): LicensePayload? {
        try {
            // 格式: WANGWANG-[Payload-Base64].[Signature-Base64]
            val rest = licenseKey.removePrefix("WANGWANG-")
            val parts = rest.split(".")
            if (parts.size != 2) return null
            
            val payloadBase64 = parts[0]
            val signatureBase64 = parts[1]
            
            // 1. Base64 解码 Payload
            val payloadJson = String(android.util.Base64.decode(payloadBase64, android.util.Base64.DEFAULT), Charsets.UTF_8)
            
            // 2. 验证 RSA 签名
            val publicKeyBytes = android.util.Base64.decode(publicKeyBase64, android.util.Base64.DEFAULT)
            val keySpec = java.security.spec.X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)
            
            val signature = java.security.Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(payloadJson.toByteArray(Charsets.UTF_8))
            
            val signatureBytes = android.util.Base64.decode(signatureBase64, android.util.Base64.DEFAULT)
            if (!signature.verify(signatureBytes)) {
                // 签名验证失败
                return null
            }
            
            // 3. 解析 JSON Payload
            val jsonObject = org.json.JSONObject(payloadJson)
            val machineId = jsonObject.getString("mid")
            val expirationTime = jsonObject.getLong("exp")
            val type = jsonObject.optString("type", "standard")
            val salt = jsonObject.optString("salt", "")
            
            return LicensePayload(
                machineId = machineId,
                expirationTime = expirationTime,
                type = type,
                salt = salt
            )
        } catch (e: Exception) {
            e.printStackTrace()
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
    data class Success(val record: LicenseRecord, val needsRestart: Boolean = true) : LicenseResult()
    data class Error(val message: String) : LicenseResult()
}