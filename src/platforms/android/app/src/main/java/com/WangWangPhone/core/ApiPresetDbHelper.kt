package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ApiPreset(
    val id: Long = 0,
    val name: String,
    val type: String, // "chat", "image", "voice"
    val provider: String, // "openai", "gemini", "minimax"
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val extraParams: String = "{}" // JSON格式的额外参数
)

class ApiPresetDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "api_preset.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "api_presets"
        
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_PROVIDER = "provider"
        private const val COLUMN_API_KEY = "api_key"
        private const val COLUMN_BASE_URL = "base_url"
        private const val COLUMN_MODEL = "model"
        private const val COLUMN_EXTRA_PARAMS = "extra_params"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_PROVIDER TEXT NOT NULL,
                $COLUMN_API_KEY TEXT NOT NULL,
                $COLUMN_BASE_URL TEXT NOT NULL,
                $COLUMN_MODEL TEXT NOT NULL,
                $COLUMN_EXTRA_PARAMS TEXT DEFAULT '{}'
            )
        """.trimIndent()
        db.execSQL(createTable)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
    
    fun savePreset(preset: ApiPreset): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, preset.name)
            put(COLUMN_TYPE, preset.type)
            put(COLUMN_PROVIDER, preset.provider)
            put(COLUMN_API_KEY, preset.apiKey)
            put(COLUMN_BASE_URL, preset.baseUrl)
            put(COLUMN_MODEL, preset.model)
            put(COLUMN_EXTRA_PARAMS, preset.extraParams)
        }
        
        return if (preset.id > 0) {
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(preset.id.toString()))
            preset.id
        } else {
            db.insert(TABLE_NAME, null, values)
        }
    }
    
    fun getPresetsByType(type: String): List<ApiPreset> {
        val presets = mutableListOf<ApiPreset>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_TYPE = ?",
            arrayOf(type),
            null,
            null,
            "$COLUMN_ID DESC"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                presets.add(
                    ApiPreset(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        type = it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE)),
                        provider = it.getString(it.getColumnIndexOrThrow(COLUMN_PROVIDER)),
                        apiKey = it.getString(it.getColumnIndexOrThrow(COLUMN_API_KEY)),
                        baseUrl = it.getString(it.getColumnIndexOrThrow(COLUMN_BASE_URL)),
                        model = it.getString(it.getColumnIndexOrThrow(COLUMN_MODEL)),
                        extraParams = it.getString(it.getColumnIndexOrThrow(COLUMN_EXTRA_PARAMS))
                    )
                )
            }
        }
        return presets
    }
    
    fun getPresetById(id: Long): ApiPreset? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        
        cursor.use {
            if (it.moveToFirst()) {
                return ApiPreset(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    type = it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE)),
                    provider = it.getString(it.getColumnIndexOrThrow(COLUMN_PROVIDER)),
                    apiKey = it.getString(it.getColumnIndexOrThrow(COLUMN_API_KEY)),
                    baseUrl = it.getString(it.getColumnIndexOrThrow(COLUMN_BASE_URL)),
                    model = it.getString(it.getColumnIndexOrThrow(COLUMN_MODEL)),
                    extraParams = it.getString(it.getColumnIndexOrThrow(COLUMN_EXTRA_PARAMS))
                )
            }
        }
        return null
    }
    
    fun deletePreset(id: Long): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString())) > 0
    }
    
    fun getAllPresets(): List<ApiPreset> {
        val presets = mutableListOf<ApiPreset>()
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, "$COLUMN_ID DESC")
        
        cursor.use {
            while (it.moveToNext()) {
                presets.add(
                    ApiPreset(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        type = it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE)),
                        provider = it.getString(it.getColumnIndexOrThrow(COLUMN_PROVIDER)),
                        apiKey = it.getString(it.getColumnIndexOrThrow(COLUMN_API_KEY)),
                        baseUrl = it.getString(it.getColumnIndexOrThrow(COLUMN_BASE_URL)),
                        model = it.getString(it.getColumnIndexOrThrow(COLUMN_MODEL)),
                        extraParams = it.getString(it.getColumnIndexOrThrow(COLUMN_EXTRA_PARAMS))
                    )
                )
            }
        }
        return presets
    }
}
