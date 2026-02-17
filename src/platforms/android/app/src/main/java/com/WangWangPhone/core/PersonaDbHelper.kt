package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class PersonaRecord(
    val name: String = "",
    val gender: String = "",
    val age: String = "",
    val personality: String = "",
    val background: String = "",
    val appearance: String = "",
    val occupation: String = "",
    val hobbies: String = "",
    val relationships: String = "",
    val goals: String = "",
    val speechStyle: String = "",
    val specialTraits: String = ""
)

class PersonaDbHelper(context: Context) : SQLiteOpenHelper(context, "persona.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE persona (
                id INTEGER PRIMARY KEY,
                name TEXT,
                gender TEXT,
                age TEXT,
                personality TEXT,
                background TEXT,
                appearance TEXT,
                occupation TEXT,
                hobbies TEXT,
                relationships TEXT,
                goals TEXT,
                speechStyle TEXT,
                specialTraits TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun savePersona(persona: PersonaRecord) {
        writableDatabase.use { db ->
            db.delete("persona", null, null)
            db.insert("persona", null, ContentValues().apply {
                put("name", persona.name)
                put("gender", persona.gender)
                put("age", persona.age)
                put("personality", persona.personality)
                put("background", persona.background)
                put("appearance", persona.appearance)
                put("occupation", persona.occupation)
                put("hobbies", persona.hobbies)
                put("relationships", persona.relationships)
                put("goals", persona.goals)
                put("speechStyle", persona.speechStyle)
                put("specialTraits", persona.specialTraits)
            })
        }
    }

    fun getPersona(): PersonaRecord? {
        readableDatabase.use { db ->
            db.rawQuery("SELECT * FROM persona LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    return PersonaRecord(
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: "",
                        gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")) ?: "",
                        age = cursor.getString(cursor.getColumnIndexOrThrow("age")) ?: "",
                        personality = cursor.getString(cursor.getColumnIndexOrThrow("personality")) ?: "",
                        background = cursor.getString(cursor.getColumnIndexOrThrow("background")) ?: "",
                        appearance = cursor.getString(cursor.getColumnIndexOrThrow("appearance")) ?: "",
                        occupation = cursor.getString(cursor.getColumnIndexOrThrow("occupation")) ?: "",
                        hobbies = cursor.getString(cursor.getColumnIndexOrThrow("hobbies")) ?: "",
                        relationships = cursor.getString(cursor.getColumnIndexOrThrow("relationships")) ?: "",
                        goals = cursor.getString(cursor.getColumnIndexOrThrow("goals")) ?: "",
                        speechStyle = cursor.getString(cursor.getColumnIndexOrThrow("speechStyle")) ?: "",
                        specialTraits = cursor.getString(cursor.getColumnIndexOrThrow("specialTraits")) ?: ""
                    )
                }
            }
        }
        return null
    }
}
