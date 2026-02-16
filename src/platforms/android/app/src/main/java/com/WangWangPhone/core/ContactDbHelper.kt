package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.icu.text.Transliterator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

data class ContactInfo(
    val id: String,
    val nickname: String,
    val wechatId: String = "",
    val region: String = "",
    val persona: String = "",
    val avatarFileName: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    fun getPinyinInitial(): String {
        val firstChar = nickname.firstOrNull() ?: return "#"
        
        // 如果是英文字母，直接返回大写
        if (firstChar.isLetter() && firstChar.code < 128) {
            return firstChar.uppercaseChar().toString()
        }
        
        // 使用 Android ICU Transliterator 转换中文到拼音
        try {
            val transliterator = Transliterator.getInstance("Han-Latin")
            val pinyin = transliterator.transliterate(firstChar.toString())
            val initial = pinyin.firstOrNull()?.uppercaseChar()
            if (initial != null && initial.isLetter()) {
                return initial.toString()
            }
        } catch (e: Exception) {
            // 如果转换失败，返回 #
        }
        
        return "#"
    }
}

class ContactDbHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "wangwang_contacts.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_CONTACTS = "contacts"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NICKNAME = "nickname"
        private const val COLUMN_WECHAT_ID = "wechat_id"
        private const val COLUMN_REGION = "region"
        private const val COLUMN_PERSONA = "persona"
        private const val COLUMN_AVATAR_FILE = "avatar_file"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        private const val CONTACT_IMAGES_DIR = "contact_images"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_CONTACTS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NICKNAME TEXT NOT NULL,
                $COLUMN_WECHAT_ID TEXT DEFAULT '',
                $COLUMN_REGION TEXT DEFAULT '',
                $COLUMN_PERSONA TEXT NOT NULL,
                $COLUMN_AVATAR_FILE TEXT DEFAULT '',
                $COLUMN_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                $COLUMN_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 未来版本升级逻辑
    }

    private fun getContactImagesDir(): File {
        val dir = File(context.filesDir, CONTACT_IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun copyImageToStorage(uri: Uri, prefix: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            val fileName = "${prefix}${UUID.randomUUID()}.jpg"
            val destFile = File(getContactImagesDir(), fileName)

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

    fun addContact(nickname: String, wechatId: String, region: String, persona: String, avatarUri: Uri?): String? {
        return try {
            val contactId = UUID.randomUUID().toString()
            val avatarFileName = avatarUri?.let { copyImageToStorage(it, "contact_") } ?: ""

            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_ID, contactId)
                put(COLUMN_NICKNAME, nickname)
                put(COLUMN_WECHAT_ID, wechatId)
                put(COLUMN_REGION, region)
                put(COLUMN_PERSONA, persona)
                put(COLUMN_AVATAR_FILE, avatarFileName)
                put(COLUMN_CREATED_AT, System.currentTimeMillis() / 1000)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.insert(TABLE_CONTACTS, null, values)
            contactId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAllContacts(): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CONTACTS,
                null,
                null,
                null,
                null,
                null,
                "$COLUMN_NICKNAME ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    contacts.add(
                        ContactInfo(
                            id = it.getString(it.getColumnIndexOrThrow(COLUMN_ID)),
                            nickname = it.getString(it.getColumnIndexOrThrow(COLUMN_NICKNAME)),
                            wechatId = it.getString(it.getColumnIndexOrThrow(COLUMN_WECHAT_ID)) ?: "",
                            region = it.getString(it.getColumnIndexOrThrow(COLUMN_REGION)) ?: "",
                            persona = it.getString(it.getColumnIndexOrThrow(COLUMN_PERSONA)) ?: "",
                            avatarFileName = it.getString(it.getColumnIndexOrThrow(COLUMN_AVATAR_FILE)) ?: "",
                            createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                            updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // 按拼音首字母分组排序
        return contacts.sortedWith(compareBy({ it.getPinyinInitial() }, { it.nickname }))
    }

    fun getContactById(id: String): ContactInfo? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CONTACTS,
                null,
                "$COLUMN_ID = ?",
                arrayOf(id),
                null,
                null,
                null,
                "1"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    ContactInfo(
                        id = it.getString(it.getColumnIndexOrThrow(COLUMN_ID)),
                        nickname = it.getString(it.getColumnIndexOrThrow(COLUMN_NICKNAME)),
                        wechatId = it.getString(it.getColumnIndexOrThrow(COLUMN_WECHAT_ID)) ?: "",
                        region = it.getString(it.getColumnIndexOrThrow(COLUMN_REGION)) ?: "",
                        persona = it.getString(it.getColumnIndexOrThrow(COLUMN_PERSONA)) ?: "",
                        avatarFileName = it.getString(it.getColumnIndexOrThrow(COLUMN_AVATAR_FILE)) ?: "",
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAvatarFilePath(avatarFileName: String): String? {
        if (avatarFileName.isEmpty()) return null
        val file = File(getContactImagesDir(), avatarFileName)
        return if (file.exists()) file.absolutePath else null
    }

    fun updateContact(id: String, nickname: String, wechatId: String, region: String, persona: String, avatarUri: Uri?): Boolean {
        return try {
            val contact = getContactById(id) ?: return false
            var avatarFileName = contact.avatarFileName
            
            if (avatarUri != null) {
                if (avatarFileName.isNotEmpty()) {
                    val oldFile = File(getContactImagesDir(), avatarFileName)
                    if (oldFile.exists()) oldFile.delete()
                }
                avatarFileName = copyImageToStorage(avatarUri, "contact_") ?: avatarFileName
            }
            
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_NICKNAME, nickname)
                put(COLUMN_WECHAT_ID, wechatId)
                put(COLUMN_REGION, region)
                put(COLUMN_PERSONA, persona)
                put(COLUMN_AVATAR_FILE, avatarFileName)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.update(TABLE_CONTACTS, values, "$COLUMN_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteContact(id: String): Boolean {
        return try {
            val contact = getContactById(id) ?: return false
            if (contact.avatarFileName.isNotEmpty()) {
                val file = File(getContactImagesDir(), contact.avatarFileName)
                if (file.exists()) file.delete()
            }
            val db = writableDatabase
            db.delete(TABLE_CONTACTS, "$COLUMN_ID = ?", arrayOf(id))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
