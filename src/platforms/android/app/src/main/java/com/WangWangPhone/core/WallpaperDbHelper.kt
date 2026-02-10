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
 * 壁纸类型常量
 */
object WallpaperType {
    const val LOCK = "lock"   // 锁屏壁纸
    const val HOME = "home"   // 桌面壁纸
}

/**
 * 壁纸记录数据类
 */
data class WallpaperRecord(
    val wallpaperType: String,
    val fileName: String,
    val updatedAt: Long = 0
)

/**
 * 壁纸数据库帮助类
 * 负责壁纸元数据的持久化存储，以及壁纸图片文件的持久化复制
 */
class WallpaperDbHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "wangwang_wallpaper.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_WALLPAPER = "wallpaper"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TYPE = "wallpaper_type"
        private const val COLUMN_FILE_NAME = "file_name"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        /** 壁纸文件持久化存储目录名 */
        private const val WALLPAPER_DIR = "wallpapers"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_WALLPAPER (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TYPE TEXT NOT NULL UNIQUE,
                $COLUMN_FILE_NAME TEXT NOT NULL,
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
     * 获取壁纸持久化存储目录
     */
    private fun getWallpaperDir(): File {
        val dir = File(context.filesDir, WALLPAPER_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 将用户选择的图片从 Uri 复制到持久化目录
     * @param uri 用户选择的图片 Uri（可能是临时路径）
     * @return 持久化后的文件名，失败返回 null
     */
    fun copyImageToStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            val fileName = "wp_${UUID.randomUUID()}.jpg"
            val destFile = File(getWallpaperDir(), fileName)

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
     * 保存壁纸记录（同时会删除旧的壁纸文件）
     * @param type 壁纸类型（WallpaperType.LOCK 或 WallpaperType.HOME）
     * @param fileName 持久化存储的文件名
     */
    fun saveWallpaper(type: String, fileName: String): Boolean {
        return try {
            // 先删除旧壁纸文件
            val oldRecord = getWallpaper(type)
            if (oldRecord != null) {
                val oldFile = File(getWallpaperDir(), oldRecord.fileName)
                if (oldFile.exists()) oldFile.delete()
            }

            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_TYPE, type)
                put(COLUMN_FILE_NAME, fileName)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.insertWithOnConflict(
                TABLE_WALLPAPER, null, values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取指定类型的壁纸记录
     */
    fun getWallpaper(type: String): WallpaperRecord? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_WALLPAPER,
                arrayOf(COLUMN_TYPE, COLUMN_FILE_NAME, COLUMN_UPDATED_AT),
                "$COLUMN_TYPE = ?",
                arrayOf(type),
                null, null, null, "1"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    WallpaperRecord(
                        wallpaperType = it.getString(0),
                        fileName = it.getString(1),
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
     * 获取壁纸文件的绝对路径
     * @return 文件路径，如果壁纸不存在返回 null
     */
    fun getWallpaperFilePath(type: String): String? {
        val record = getWallpaper(type) ?: return null
        val file = File(getWallpaperDir(), record.fileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * 清除指定类型的壁纸（同时删除文件）
     */
    fun clearWallpaper(type: String): Boolean {
        return try {
            val record = getWallpaper(type)
            if (record != null) {
                val file = File(getWallpaperDir(), record.fileName)
                if (file.exists()) file.delete()
            }
            val db = writableDatabase
            db.delete(TABLE_WALLPAPER, "$COLUMN_TYPE = ?", arrayOf(type))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清除所有壁纸记录并删除对应的文件
     */
    fun clearAllWallpapers(): Boolean {
        return try {
            val db = writableDatabase
            val cursor = db.query(TABLE_WALLPAPER, arrayOf(COLUMN_FILE_NAME), null, null, null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    val fileName = it.getString(0)
                    val file = File(getWallpaperDir(), fileName)
                    if (file.exists()) file.delete()
                }
            }
            db.delete(TABLE_WALLPAPER, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
