package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 布局项数据类
 * 记录每个应用图标在网格中的位置
 */
data class LayoutItem(
    val appId: String,      // 应用唯一标识（如 "phone", "settings"）
    val position: Int,       // 在网格中的位置索引（0-based）
    val area: String = "grid" // 区域标识: "grid" = 主网格, "dock" = 底部 Dock 栏
)

/**
 * 布局信息数据库帮助类
 * 用于持久化存储用户自定义的主屏幕布局
 */
class LayoutDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "wangwang_layout.db"
        private const val DATABASE_VERSION = 1

        // 表名
        private const val TABLE_LAYOUT = "app_layout"

        // 列名
        private const val COLUMN_ID = "id"
        private const val COLUMN_APP_ID = "app_id"
        private const val COLUMN_POSITION = "position"
        private const val COLUMN_AREA = "area"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_LAYOUT (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_APP_ID TEXT NOT NULL,
                $COLUMN_POSITION INTEGER NOT NULL,
                $COLUMN_AREA TEXT NOT NULL DEFAULT 'grid',
                $COLUMN_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                $COLUMN_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()

        val createIndexSQL = """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_layout_app ON $TABLE_LAYOUT($COLUMN_APP_ID, $COLUMN_AREA)
        """.trimIndent()

        db.execSQL(createTableSQL)
        db.execSQL(createIndexSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 未来版本升级时的迁移逻辑
    }

    /**
     * 保存整个布局（事务操作）
     * 会先清除旧布局，然后插入新布局
     */
    fun saveLayout(items: List<LayoutItem>): Boolean {
        return try {
            val db = writableDatabase
            db.beginTransaction()
            try {
                // 先清除旧布局
                db.delete(TABLE_LAYOUT, null, null)

                // 插入新布局
                for (item in items) {
                    val values = ContentValues().apply {
                        put(COLUMN_APP_ID, item.appId)
                        put(COLUMN_POSITION, item.position)
                        put(COLUMN_AREA, item.area)
                    }
                    val result = db.insert(TABLE_LAYOUT, null, values)
                    if (result == -1L) {
                        return false
                    }
                }

                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取保存的布局
     * 返回按区域和位置排序的布局项列表
     */
    fun getLayout(): List<LayoutItem> {
        val items = mutableListOf<LayoutItem>()
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_LAYOUT,
                arrayOf(COLUMN_APP_ID, COLUMN_POSITION, COLUMN_AREA),
                null,
                null,
                null,
                null,
                "$COLUMN_AREA ASC, $COLUMN_POSITION ASC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    items.add(
                        LayoutItem(
                            appId = it.getString(it.getColumnIndexOrThrow(COLUMN_APP_ID)),
                            position = it.getInt(it.getColumnIndexOrThrow(COLUMN_POSITION)),
                            area = it.getString(it.getColumnIndexOrThrow(COLUMN_AREA))
                        )
                    )
                }
            }
            items
        } catch (e: Exception) {
            e.printStackTrace()
            items
        }
    }

    /**
     * 清除所有布局
     */
    fun clearLayout(): Boolean {
        return try {
            val db = writableDatabase
            db.delete(TABLE_LAYOUT, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 检查是否有已保存的布局
     */
    fun hasLayout(): Boolean {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_LAYOUT", null)
            cursor.use {
                it.moveToFirst() && it.getInt(0) > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 恢复默认设置
     * 1. 清除布局
     * 2. 清除壁纸
     * 3. 清除天气缓存
     * 4. 重置用户资料
     * 5. 清除自定义图标
     */
    fun resetToDefaultSettings(
        wallpaperDbHelper: WallpaperDbHelper,
        weatherCacheDbHelper: WeatherCacheDbHelper,
        userProfileDbHelper: UserProfileDbHelper,
        iconCustomizationDbHelper: IconCustomizationDbHelper
    ): Boolean {
        return try {
            val db = writableDatabase
            db.beginTransaction()
            try {
                // 1. 清除布局
                db.delete(TABLE_LAYOUT, null, null)

                // 2. 清除壁纸
                wallpaperDbHelper.clearAllWallpapers()

                // 3. 清除天气缓存
                weatherCacheDbHelper.clearAllWeatherCache()
                weatherCacheDbHelper.saveManualLocation(null)

                // 4. 重置用户资料
                userProfileDbHelper.resetToDefault()

                // 5. 清除自定义图标
                iconCustomizationDbHelper.clearAllCustomIcons()

                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
