package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 天气缓存数据类
 */
data class WeatherCacheRecord(
    val city: String,           // 城市名
    val temp: String,           // 温度
    val description: String,    // 天气描述
    val icon: String,           // 天气图标
    val range: String,          // 温度范围
    val requestDate: String,    // 请求日期 (yyyy-MM-dd)
    val updatedAt: Long = 0     // 更新时间戳
)

/**
 * 天气缓存数据库帮助类
 * 用于缓存天气数据，每天只请求一次
 */
class WeatherCacheDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "wangwang_weather_cache.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_WEATHER = "weather_cache"
        private const val COLUMN_ID = "id"
        private const val COLUMN_CITY = "city"
        private const val COLUMN_TEMP = "temp"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_ICON = "icon"
        private const val COLUMN_RANGE = "range_info"
        private const val COLUMN_REQUEST_DATE = "request_date"
        private const val COLUMN_UPDATED_AT = "updated_at"

        /**
         * 获取今天的日期字符串 (yyyy-MM-dd)
         */
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_WEATHER (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CITY TEXT NOT NULL,
                $COLUMN_TEMP TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_ICON TEXT NOT NULL,
                $COLUMN_RANGE TEXT NOT NULL,
                $COLUMN_REQUEST_DATE TEXT NOT NULL,
                $COLUMN_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                UNIQUE($COLUMN_CITY, $COLUMN_REQUEST_DATE)
            )
        """.trimIndent()
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WEATHER")
        onCreate(db)
    }

    /**
     * 保存天气缓存
     */
    fun saveWeatherCache(record: WeatherCacheRecord): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_CITY, record.city)
                put(COLUMN_TEMP, record.temp)
                put(COLUMN_DESCRIPTION, record.description)
                put(COLUMN_ICON, record.icon)
                put(COLUMN_RANGE, record.range)
                put(COLUMN_REQUEST_DATE, record.requestDate)
                put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.insertWithOnConflict(
                TABLE_WEATHER, null, values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取今天的天气缓存
     * @param city 城市名
     * @return 如果今天已经请求过，返回缓存数据；否则返回 null
     */
    fun getTodayWeatherCache(city: String): WeatherCacheRecord? {
        return try {
            val db = readableDatabase
            val today = getTodayDateString()
            val cursor = db.query(
                TABLE_WEATHER,
                arrayOf(COLUMN_CITY, COLUMN_TEMP, COLUMN_DESCRIPTION, COLUMN_ICON, COLUMN_RANGE, COLUMN_REQUEST_DATE, COLUMN_UPDATED_AT),
                "$COLUMN_CITY = ? AND $COLUMN_REQUEST_DATE = ?",
                arrayOf(city, today),
                null, null, null, "1"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    WeatherCacheRecord(
                        city = it.getString(0),
                        temp = it.getString(1),
                        description = it.getString(2),
                        icon = it.getString(3),
                        range = it.getString(4),
                        requestDate = it.getString(5),
                        updatedAt = it.getLong(6)
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 清除过期缓存（非今天的数据）
     */
    fun clearExpiredCache(): Boolean {
        return try {
            val db = writableDatabase
            val today = getTodayDateString()
            db.delete(TABLE_WEATHER, "$COLUMN_REQUEST_DATE != ?", arrayOf(today))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清除所有缓存
     */
    fun clearAllCache(): Boolean {
        return try {
            val db = writableDatabase
            db.delete(TABLE_WEATHER, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清除所有天气缓存
     */
    fun clearAllWeatherCache(): Boolean {
        return clearAllCache()
    }
}