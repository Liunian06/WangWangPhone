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
 * 用户资料数据类
 */
data class UserProfile(
    val nickname: String = "我的昵称",
    val signature: String = "游荡的孤高灵魂不需要栖身之地",
    val avatarFileName: String = "",
    val coverFileName: String = "",
    val updatedAt: Long = 0
)

/**
 * 用户资料数据库帮助类
 * 负责用户昵称、签名、头像、朋友圈封面的持久化存储
 * 图片文件会被复制到应用内部存储目录，确保持久化不丢失
 */
class UserProfileDbHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "wangwang_user_profile.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_PROFILE = "user_profile"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NICKNAME = "nickname"
        private const val COLUMN_SIGNATURE = "signature"
        private const val COLUMN_AVATAR_FILE = "avatar_file"
        private const val COLUMN_COVER_FILE = "cover_file"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        /** 用户资料图片持久化存储目录名 */
        private const val PROFILE_IMAGES_DIR = "profile_images"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_PROFILE (
                $COLUMN_ID INTEGER PRIMARY KEY CHECK ($COLUMN_ID = 1),
                $COLUMN_NICKNAME TEXT NOT NULL DEFAULT '我的昵称',
                $COLUMN_SIGNATURE TEXT NOT NULL DEFAULT '游荡的孤高灵魂不需要栖身之地',
                $COLUMN_AVATAR_FILE TEXT DEFAULT '',
                $COLUMN_COVER_FILE TEXT DEFAULT '',
                $COLUMN_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                $COLUMN_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()
        db.execSQL(createTableSQL)

        // 插入默认记录
        val defaultValues = ContentValues().apply {
            put(COLUMN_ID, 1)
            put(COLUMN_NICKNAME, "我的昵称")
            put(COLUMN_SIGNATURE, "游荡的孤高灵魂不需要栖身之地")
            put(COLUMN_AVATAR_FILE, "")
            put(COLUMN_COVER_FILE, "")
        }
        db.insert(TABLE_PROFILE, null, defaultValues)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 未来版本升级时的迁移逻辑
    }

    /**
     * 获取资料图片持久化存储目录
     */
    private fun getProfileImagesDir(): File {
        val dir = File(context.filesDir, PROFILE_IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 将用户选择的图片从 Uri 复制到持久化目录
     * @param uri 用户选择的图片 Uri（可能是临时路径）
     * @param prefix 文件名前缀（如 "avatar_" 或 "cover_"）
     * @return 持久化后的文件名，失败返回 null
     */
    fun copyImageToStorage(uri: Uri, prefix: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            val fileName = "${prefix}${UUID.randomUUID()}.jpg"
            val destFile = File(getProfileImagesDir(), fileName)

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
     * 获取用户资料
     */
    fun getUserProfile(): UserProfile {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_PROFILE,
                arrayOf(COLUMN_NICKNAME, COLUMN_SIGNATURE, COLUMN_AVATAR_FILE, COLUMN_COVER_FILE, COLUMN_UPDATED_AT),
                "$COLUMN_ID = 1",
                null, null, null, null, "1"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    UserProfile(
                        nickname = it.getString(0) ?: "我的昵称",
                        signature = it.getString(1) ?: "游荡的孤高灵魂不需要栖身之地",
                        avatarFileName = it.getString(2) ?: "",
                        coverFileName = it.getString(3) ?: "",
                        updatedAt = it.getLong(4)
                    )
                } else {
                    UserProfile()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UserProfile()
        }
    }

    /**
     * 更新昵称
     */
    fun updateNickname(nickname: String): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_NICKNAME, nickname)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.update(TABLE_PROFILE, values, "$COLUMN_ID = 1", null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 更新签名
     */
    fun updateSignature(signature: String): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_SIGNATURE, signature)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.update(TABLE_PROFILE, values, "$COLUMN_ID = 1", null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 更新头像（同时删除旧的头像文件）
     * @param uri 用户选择的图片 Uri
     * @return 成功返回新文件名，失败返回 null
     */
    fun updateAvatar(uri: Uri): String? {
        return try {
            // 删除旧头像文件
            val oldProfile = getUserProfile()
            if (oldProfile.avatarFileName.isNotEmpty()) {
                val oldFile = File(getProfileImagesDir(), oldProfile.avatarFileName)
                if (oldFile.exists()) oldFile.delete()
            }

            // 复制新图片
            val newFileName = copyImageToStorage(uri, "avatar_") ?: return null

            // 更新数据库
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_AVATAR_FILE, newFileName)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.update(TABLE_PROFILE, values, "$COLUMN_ID = 1", null)

            newFileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 更新朋友圈封面（同时删除旧的封面文件）
     * @param uri 用户选择的图片 Uri
     * @return 成功返回新文件名，失败返回 null
     */
    fun updateCover(uri: Uri): String? {
        return try {
            // 删除旧封面文件
            val oldProfile = getUserProfile()
            if (oldProfile.coverFileName.isNotEmpty()) {
                val oldFile = File(getProfileImagesDir(), oldProfile.coverFileName)
                if (oldFile.exists()) oldFile.delete()
            }

            // 复制新图片
            val newFileName = copyImageToStorage(uri, "cover_") ?: return null

            // 更新数据库
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_COVER_FILE, newFileName)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.update(TABLE_PROFILE, values, "$COLUMN_ID = 1", null)

            newFileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取头像文件的绝对路径
     * @return 文件路径，如果头像不存在返回 null
     */
    fun getAvatarFilePath(): String? {
        val profile = getUserProfile()
        if (profile.avatarFileName.isEmpty()) return null
        val file = File(getProfileImagesDir(), profile.avatarFileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * 获取封面文件的绝对路径
     * @return 文件路径，如果封面不存在返回 null
     */
    fun getCoverFilePath(): String? {
        val profile = getUserProfile()
        if (profile.coverFileName.isEmpty()) return null
        val file = File(getProfileImagesDir(), profile.coverFileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * 重置资料为默认值（并删除已上传的头像和封面文件）
     */
    fun resetToDefault(): Boolean {
        return try {
            // 删除旧头像和封面文件
            val profile = getUserProfile()
            val imagesDir = getProfileImagesDir()
            if (profile.avatarFileName.isNotEmpty()) {
                val file = File(imagesDir, profile.avatarFileName)
                if (file.exists()) file.delete()
            }
            if (profile.coverFileName.isNotEmpty()) {
                val file = File(imagesDir, profile.coverFileName)
                if (file.exists()) file.delete()
            }

            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_NICKNAME, "我的昵称")
                put(COLUMN_SIGNATURE, "游荡的孤高灵魂不需要栖身之地")
                put(COLUMN_AVATAR_FILE, "")
                put(COLUMN_COVER_FILE, "")
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.update(TABLE_PROFILE, values, "$COLUMN_ID = 1", null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}