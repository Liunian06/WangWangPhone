package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 授权信息数据库帮助类
 * 用于持久化存储授权信息，确保应用更新后无需重新激活
 */
class LicenseDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "wangwang_license.db"
        private const val DATABASE_VERSION = 1
        
        // 表名
        private const val TABLE_LICENSE = "license"
        
        // 列名
        private const val COLUMN_ID = "id"
        private const val COLUMN_LICENSE_KEY = "license_key"
        private const val COLUMN_MACHINE_ID = "machine_id"
        private const val COLUMN_EXPIRATION_TIME = "expiration_time"
        private const val COLUMN_LICENSE_TYPE = "license_type"
        private const val COLUMN_ACTIVATION_TIME = "activation_time"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_LICENSE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LICENSE_KEY TEXT NOT NULL,
                $COLUMN_MACHINE_ID TEXT NOT NULL,
                $COLUMN_EXPIRATION_TIME INTEGER NOT NULL,
                $COLUMN_LICENSE_TYPE TEXT DEFAULT 'standard',
                $COLUMN_ACTIVATION_TIME INTEGER NOT NULL,
                $COLUMN_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                $COLUMN_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()
        
        db.execSQL(createTableSQL)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 未来版本升级时的迁移逻辑
        // 当前版本 1，暂无需迁移
    }
    
    /**
     * 保存授权记录
     * 会先清除旧记录，确保只有一条授权信息
     */
    fun saveLicenseRecord(record: LicenseRecord): Boolean {
        return try {
            val db = writableDatabase
            
            // 先清除旧记录
            db.delete(TABLE_LICENSE, null, null)
            
            // 插入新记录
            val values = ContentValues().apply {
                put(COLUMN_LICENSE_KEY, record.licenseKey)
                put(COLUMN_MACHINE_ID, record.machineId)
                put(COLUMN_EXPIRATION_TIME, record.expirationTime)
                put(COLUMN_LICENSE_TYPE, record.licenseType)
                put(COLUMN_ACTIVATION_TIME, record.activationTime)
            }
            
            val result = db.insert(TABLE_LICENSE, null, values)
            result != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取授权记录
     */
    fun getLicenseRecord(): LicenseRecord? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_LICENSE,
                arrayOf(
                    COLUMN_LICENSE_KEY,
                    COLUMN_MACHINE_ID,
                    COLUMN_EXPIRATION_TIME,
                    COLUMN_LICENSE_TYPE,
                    COLUMN_ACTIVATION_TIME
                ),
                null,
                null,
                null,
                null,
                "$COLUMN_ID DESC",
                "1"
            )
            
            cursor.use {
                if (it.moveToFirst()) {
                    LicenseRecord(
                        licenseKey = it.getString(it.getColumnIndexOrThrow(COLUMN_LICENSE_KEY)),
                        machineId = it.getString(it.getColumnIndexOrThrow(COLUMN_MACHINE_ID)),
                        expirationTime = it.getLong(it.getColumnIndexOrThrow(COLUMN_EXPIRATION_TIME)),
                        licenseType = it.getString(it.getColumnIndexOrThrow(COLUMN_LICENSE_TYPE)),
                        activationTime = it.getLong(it.getColumnIndexOrThrow(COLUMN_ACTIVATION_TIME))
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 清除授权记录
     */
    fun clearLicenseRecord(): Boolean {
        return try {
            val db = writableDatabase
            db.delete(TABLE_LICENSE, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 检查是否有有效的授权记录
     */
    fun hasValidLicense(): Boolean {
        val record = getLicenseRecord() ?: return false
        val now = System.currentTimeMillis() / 1000
        return record.expirationTime > now
    }
}