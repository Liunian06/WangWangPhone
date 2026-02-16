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
        return when {
            firstChar.isLetter() -> firstChar.uppercaseChar().toString()
            firstChar in '\u4e00'..'\u9fa5' -> getPinyinFirstLetter(firstChar).toString()
            else -> "#"
        }
    }
    
    companion object {
        private fun getPinyinFirstLetter(c: Char): Char {
            val code = c.code
            return when {
                code in 0x4e00..0x9fa5 -> {
                    when (code) {
                        in 0x963f..0x9fa5 -> 'A'
                        in 0x5df4..0x5e7f -> 'B'
                        in 0x5f69..0x64cd -> 'C'
                        in 0x5927..0x5df3 -> 'D'
                        in 0x5384..0x5592 -> 'E'
                        in 0x53d1..0x5926 -> 'F'
                        in 0x7518..0x8fc7 -> 'G'
                        in 0x54c8..0x9ed1 -> 'H'
                        in 0x4e0c..0x4e8c -> 'I'
                        in 0x5939..0x9e64 -> 'J'
                        in 0x5361..0x5fbd -> 'K'
                        in 0x5783..0x9f99 -> 'L'
                        in 0x5988..0x9ebb -> 'M'
                        in 0x54ea..0x8bb7 -> 'N'
                        in 0x5594..0x8bb4 -> 'O'
                        in 0x556a..0x9f50 -> 'P'
                        in 0x4e03..0x9f50 -> 'Q'
                        in 0x7136..0x8ba9 -> 'R'
                        in 0x4e09..0x9f3b -> 'S'
                        in 0x584c..0x9f4a -> 'T'
                        in 0x7a74..0x7a74 -> 'U'
                        in 0x6316..0x6316 -> 'V'
                        in 0x6316..0x9f9f -> 'W'
                        in 0x5915..0x9f99 -> 'X'
                        in 0x538b..0x9f50 -> 'Y'
                        in 0x531d..0x9f9f -> 'Z'
                        else -> getDetailedPinyin(c)
                    }
                }
                else -> '#'
            }
        }
        
        private fun getDetailedPinyin(c: Char): Char {
            val code = c.code
            return when (code) {
                in 0x963f..0x9f7f -> 'A'
                in 0x5df4..0x5e7f -> 'B'
                in 0x5f69..0x64cd -> 'C'
                in 0x5927..0x5df3 -> 'D'
                in 0x5384..0x5592 -> 'E'
                in 0x53d1..0x5926 -> 'F'
                in 0x7518..0x8fc7 -> 'G'
                in 0x54c8..0x9ed1 -> 'H'
                in 0x673a..0x9e64 -> 'J'
                in 0x5361..0x5fbd -> 'K'
                in 0x5783..0x9f99 -> 'L'
                in 0x5988..0x9ebb -> 'M'
                in 0x54ea..0x8bb7 -> 'N'
                in 0x556a..0x9f50 -> 'P'
                in 0x4e03..0x9f50 -> 'Q'
                in 0x7136..0x8ba9 -> 'R'
                in 0x4e09..0x9f3b -> 'S'
                in 0x584c..0x9f4a -> 'T'
                in 0x6316..0x9f9f -> 'W'
                in 0x5915..0x9f99 -> 'X'
                in 0x538b..0x9f50 -> 'Y'
                in 0x531d..0x9f9f -> 'Z'
                else -> '#'
            }
        }
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
        return contacts
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
