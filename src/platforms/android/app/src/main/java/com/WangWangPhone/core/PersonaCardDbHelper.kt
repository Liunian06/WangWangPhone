package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

data class PersonaCard(
    val id: Long = 0,
    val name: String,
    val apiPresetId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class PersonaMessage(
    val id: Long = 0,
    val cardId: Long,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PersonaCardDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "persona_cards.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_CARDS = "persona_cards"
        private const val TABLE_MESSAGES = "persona_messages"

        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_API_PRESET_ID = "api_preset_id"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        private const val COLUMN_CARD_ID = "card_id"
        private const val COLUMN_ROLE = "role"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCardsTable = """
            CREATE TABLE $TABLE_CARDS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_API_PRESET_ID INTEGER NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CARD_ID INTEGER NOT NULL,
                $COLUMN_ROLE TEXT NOT NULL,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_CARD_ID) REFERENCES $TABLE_CARDS($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createCardsTable)
        db.execSQL(createMessagesTable)
        db.execSQL("CREATE INDEX idx_messages_card_id ON $TABLE_MESSAGES($COLUMN_CARD_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CARDS")
        onCreate(db)
    }

    fun createCard(name: String, apiPresetId: Long): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_API_PRESET_ID, apiPresetId)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        return db.insert(TABLE_CARDS, null, values)
    }

    fun getAllCards(): List<PersonaCard> {
        val cards = mutableListOf<PersonaCard>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CARDS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_UPDATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                cards.add(
                    PersonaCard(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        apiPresetId = it.getLong(it.getColumnIndexOrThrow(COLUMN_API_PRESET_ID)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
                    )
                )
            }
        }
        return cards
    }

    fun getCard(cardId: Long): PersonaCard? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CARDS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(cardId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return PersonaCard(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    apiPresetId = it.getLong(it.getColumnIndexOrThrow(COLUMN_API_PRESET_ID)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
                )
            }
        }
        return null
    }

    fun updateCard(cardId: Long, name: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        db.update(TABLE_CARDS, values, "$COLUMN_ID = ?", arrayOf(cardId.toString()))
    }

    fun deleteCard(cardId: Long) {
        val db = writableDatabase
        db.delete(TABLE_CARDS, "$COLUMN_ID = ?", arrayOf(cardId.toString()))
    }

    fun saveMessage(cardId: Long, role: String, content: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CARD_ID, cardId)
            put(COLUMN_ROLE, role)
            put(COLUMN_CONTENT, content)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        
        // 更新卡片的更新时间
        val cardValues = ContentValues().apply {
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        db.update(TABLE_CARDS, cardValues, "$COLUMN_ID = ?", arrayOf(cardId.toString()))
        
        return db.insert(TABLE_MESSAGES, null, values)
    }

    fun getMessages(cardId: Long): List<PersonaMessage> {
        val messages = mutableListOf<PersonaMessage>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "$COLUMN_CARD_ID = ?",
            arrayOf(cardId.toString()),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                messages.add(
                    PersonaMessage(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        cardId = it.getLong(it.getColumnIndexOrThrow(COLUMN_CARD_ID)),
                        role = it.getString(it.getColumnIndexOrThrow(COLUMN_ROLE)),
                        content = it.getString(it.getColumnIndexOrThrow(COLUMN_CONTENT)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    )
                )
            }
        }
        return messages
    }

    fun clearMessages(cardId: Long) {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, "$COLUMN_CARD_ID = ?", arrayOf(cardId.toString()))
    }
}
