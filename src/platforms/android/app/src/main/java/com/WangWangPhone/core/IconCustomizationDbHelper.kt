package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * 图标自定义记录数据类
 */
data class IconCustomizationRecord(
    val appId: String,
    val customIconPath: String,
    val updatedAt: Long = 0
)

/**
 * 图标自定义数据库帮助类
 * 负责桌面图标自定义图片的持久化存储
 */
class IconCustomizationDbHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "wangwang_icon_customization.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_ICON_CUSTOMIZATION = "icon_customization"
        private const val COLUMN_ID = "id"
        private const val COLUMN_APP_ID = "app_id"
        private const val COLUMN_CUSTOM_ICON_PATH = "custom_icon_path"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        /** 图标文件持久化存储目录名 */
        private const val ICON_DIR = "custom_icons"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_ICON_CUSTOMIZATION (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_APP_ID TEXT NOT NULL UNIQUE,
                $COLUMN_CUSTOM_ICON_PATH TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                $COLUMN_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 未来版本升级时的迁移逻辑
    }

    /**
     * 获取图标持久化存储目录
     */
    private fun getIconDir(): File {
        val dir = File(context.filesDir, ICON_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 将用户选择的图片从 Uri 复制到持久化目录
     * @param uri 用户选择的图片 Uri
     * @return 持久化后的文件名，失败返回 null
     */
    fun copyImageToStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            val fileName = "icon_${UUID.randomUUID()}.jpg"
            val destFile = File(getIconDir(), fileName)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()

            fileName
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 保存图标自定义记录（同时会删除旧的图标文件）
     * @param appId 应用ID
     * @param fileName 持久化存储的文件名
     */
    fun saveCustomIcon(appId: String, fileName: String): Boolean {
        return try {
            // 先删除旧图标文件
            val oldRecord = getCustomIcon(appId)
            if (oldRecord != null) {
                val oldFile = File(getIconDir(), oldRecord.customIconPath)
                if (oldFile.exists()) oldFile.delete()
            }

            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_APP_ID, appId)
                put(COLUMN_CUSTOM_ICON_PATH, fileName)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.insertWithOnConflict(
                TABLE_ICON_CUSTOMIZATION, null, values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取指定应用的自定义图标记录
     */
    fun getCustomIcon(appId: String): IconCustomizationRecord? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_ICON_CUSTOMIZATION,
                arrayOf(COLUMN_APP_ID, COLUMN_CUSTOM_ICON_PATH, COLUMN_UPDATED_AT),
                "$COLUMN_APP_ID = ?",
                arrayOf(appId),
                null, null, null, "1"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    IconCustomizationRecord(
                        appId = it.getString(0),
                        customIconPath = it.getString(1),
                        updatedAt = it.getLong(2)
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取自定义图标文件的绝对路径
     * @return 文件路径，如果图标不存在返回 null
     */
    fun getCustomIconFilePath(appId: String): String? {
        val record = getCustomIcon(appId) ?: return null
        val file = File(getIconDir(), record.customIconPath)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * 获取所有自定义图标记录
     */
    fun getAllCustomIcons(): List<IconCustomizationRecord> {
        val records = mutableListOf<IconCustomizationRecord>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_ICON_CUSTOMIZATION,
                arrayOf(COLUMN_APP_ID, COLUMN_CUSTOM_ICON_PATH, COLUMN_UPDATED_AT),
                null, null, null, null, null
            )
            cursor.use {
                while (it.moveToNext()) {
                    records.add(
                        IconCustomizationRecord(
                            appId = it.getString(0),
                            customIconPath = it.getString(1),
                            updatedAt = it.getLong(2)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return records
    }

    /**
     * 清除指定应用的自定义图标（同时删除文件）
     */
    fun clearCustomIcon(appId: String): Boolean {
        return try {
            val record = getCustomIcon(appId)
            if (record != null) {
                val file = File(getIconDir(), record.customIconPath)
                if (file.exists()) file.delete()
            }
            val db = writableDatabase
            db.delete(TABLE_ICON_CUSTOMIZATION, "$COLUMN_APP_ID = ?", arrayOf(appId))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清除所有自定义图标记录并删除对应的文件
     */
    fun clearAllCustomIcons(): Boolean {
        return try {
            val db = writableDatabase
            val cursor = db.query(TABLE_ICON_CUSTOMIZATION, arrayOf(COLUMN_CUSTOM_ICON_PATH), null, null, null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    val fileName = it.getString(0)
                    val file = File(getIconDir(), fileName)
                    if (file.exists()) file.delete()
                }
            }
            db.delete(TABLE_ICON_CUSTOMIZATION, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
