package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID

const val WEB_WIDGET_LAYOUT_PREFIX = "web_widget:"

fun webWidgetLayoutId(widgetId: String): String = "$WEB_WIDGET_LAYOUT_PREFIX$widgetId"

fun widgetIdFromLayoutId(layoutId: String): String? {
    return if (layoutId.startsWith(WEB_WIDGET_LAYOUT_PREFIX)) {
        layoutId.removePrefix(WEB_WIDGET_LAYOUT_PREFIX).ifBlank { null }
    } else {
        null
    }
}

data class WebWidgetRecord(
    val id: String,
    val name: String,
    val htmlCode: String,
    val cssCode: String,
    val jsCode: String,
    val spanX: Int,
    val spanY: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class WebWidgetAssetRecord(
    val id: String,
    val widgetId: String,
    val originalName: String,
    val storedName: String,
    val mimeType: String,
    val createdAt: Long
)

class WebWidgetDbHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "wangwang_web_widgets.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_WIDGET = "web_widget"
        private const val TABLE_ASSET = "web_widget_asset"

        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_HTML = "html_code"
        private const val COLUMN_CSS = "css_code"
        private const val COLUMN_JS = "js_code"
        private const val COLUMN_SPAN_X = "span_x"
        private const val COLUMN_SPAN_Y = "span_y"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        private const val COLUMN_WIDGET_ID = "widget_id"
        private const val COLUMN_ORIGINAL_NAME = "original_name"
        private const val COLUMN_STORED_NAME = "stored_name"
        private const val COLUMN_MIME_TYPE = "mime_type"

        private const val WEB_WIDGET_DIR = "web_widgets"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_WIDGET (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_HTML TEXT NOT NULL DEFAULT '',
                $COLUMN_CSS TEXT NOT NULL DEFAULT '',
                $COLUMN_JS TEXT NOT NULL DEFAULT '',
                $COLUMN_SPAN_X INTEGER NOT NULL DEFAULT 2,
                $COLUMN_SPAN_Y INTEGER NOT NULL DEFAULT 2,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_ASSET (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_WIDGET_ID TEXT NOT NULL,
                $COLUMN_ORIGINAL_NAME TEXT NOT NULL,
                $COLUMN_STORED_NAME TEXT NOT NULL,
                $COLUMN_MIME_TYPE TEXT NOT NULL DEFAULT 'application/octet-stream',
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_web_widget_asset_widget ON $TABLE_ASSET($COLUMN_WIDGET_ID)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_web_widget_asset_name ON $TABLE_ASSET($COLUMN_WIDGET_ID, $COLUMN_STORED_NAME)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    fun createWidget(
        name: String,
        htmlCode: String,
        cssCode: String,
        jsCode: String,
        spanX: Int,
        spanY: Int
    ): WebWidgetRecord? {
        val widgetId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis() / 1000
        val record = WebWidgetRecord(
            id = widgetId,
            name = name.trim().ifBlank { "未命名组件" },
            htmlCode = htmlCode,
            cssCode = cssCode,
            jsCode = jsCode,
            spanX = spanX.coerceAtLeast(1),
            spanY = spanY.coerceAtLeast(1),
            createdAt = now,
            updatedAt = now
        )
        return if (saveWidget(record)) record else null
    }

    fun saveWidget(record: WebWidgetRecord): Boolean {
        return try {
            val now = System.currentTimeMillis() / 1000
            val existing = getWidget(record.id)
            val values = ContentValues().apply {
                put(COLUMN_ID, record.id)
                put(COLUMN_NAME, record.name.trim().ifBlank { "未命名组件" })
                put(COLUMN_HTML, record.htmlCode)
                put(COLUMN_CSS, record.cssCode)
                put(COLUMN_JS, record.jsCode)
                put(COLUMN_SPAN_X, record.spanX.coerceAtLeast(1))
                put(COLUMN_SPAN_Y, record.spanY.coerceAtLeast(1))
                put(COLUMN_CREATED_AT, existing?.createdAt ?: record.createdAt.takeIf { it > 0 } ?: now)
                put(COLUMN_UPDATED_AT, now)
            }
            writableDatabase.insertWithOnConflict(
                TABLE_WIDGET,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            ) != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getWidget(widgetId: String): WebWidgetRecord? {
        return try {
            val cursor = readableDatabase.query(
                TABLE_WIDGET,
                arrayOf(
                    COLUMN_ID,
                    COLUMN_NAME,
                    COLUMN_HTML,
                    COLUMN_CSS,
                    COLUMN_JS,
                    COLUMN_SPAN_X,
                    COLUMN_SPAN_Y,
                    COLUMN_CREATED_AT,
                    COLUMN_UPDATED_AT
                ),
                "$COLUMN_ID = ?",
                arrayOf(widgetId),
                null,
                null,
                null,
                "1"
            )
            cursor.use {
                if (!it.moveToFirst()) return null
                WebWidgetRecord(
                    id = it.getString(0),
                    name = it.getString(1),
                    htmlCode = it.getString(2),
                    cssCode = it.getString(3),
                    jsCode = it.getString(4),
                    spanX = it.getInt(5),
                    spanY = it.getInt(6),
                    createdAt = it.getLong(7),
                    updatedAt = it.getLong(8)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAllWidgets(): List<WebWidgetRecord> {
        val records = mutableListOf<WebWidgetRecord>()
        try {
            val cursor = readableDatabase.query(
                TABLE_WIDGET,
                arrayOf(
                    COLUMN_ID,
                    COLUMN_NAME,
                    COLUMN_HTML,
                    COLUMN_CSS,
                    COLUMN_JS,
                    COLUMN_SPAN_X,
                    COLUMN_SPAN_Y,
                    COLUMN_CREATED_AT,
                    COLUMN_UPDATED_AT
                ),
                null,
                null,
                null,
                null,
                "$COLUMN_UPDATED_AT DESC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    records.add(
                        WebWidgetRecord(
                            id = it.getString(0),
                            name = it.getString(1),
                            htmlCode = it.getString(2),
                            cssCode = it.getString(3),
                            jsCode = it.getString(4),
                            spanX = it.getInt(5),
                            spanY = it.getInt(6),
                            createdAt = it.getLong(7),
                            updatedAt = it.getLong(8)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return records
    }

    fun deleteWidget(widgetId: String): Boolean {
        return try {
            val database = writableDatabase
            database.beginTransaction()
            try {
                database.delete(TABLE_ASSET, "$COLUMN_WIDGET_ID = ?", arrayOf(widgetId))
                database.delete(TABLE_WIDGET, "$COLUMN_ID = ?", arrayOf(widgetId))
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
            deleteRecursively(getWidgetDir(widgetId))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearAllWidgets(): Boolean {
        return try {
            val database = writableDatabase
            database.beginTransaction()
            try {
                database.delete(TABLE_ASSET, null, null)
                database.delete(TABLE_WIDGET, null, null)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
            deleteRecursively(File(context.filesDir, WEB_WIDGET_DIR))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getWidgetAssets(widgetId: String): List<WebWidgetAssetRecord> {
        val assets = mutableListOf<WebWidgetAssetRecord>()
        try {
            val cursor = readableDatabase.query(
                TABLE_ASSET,
                arrayOf(
                    COLUMN_ID,
                    COLUMN_WIDGET_ID,
                    COLUMN_ORIGINAL_NAME,
                    COLUMN_STORED_NAME,
                    COLUMN_MIME_TYPE,
                    COLUMN_CREATED_AT
                ),
                "$COLUMN_WIDGET_ID = ?",
                arrayOf(widgetId),
                null,
                null,
                "$COLUMN_CREATED_AT ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    assets.add(
                        WebWidgetAssetRecord(
                            id = it.getString(0),
                            widgetId = it.getString(1),
                            originalName = it.getString(2),
                            storedName = it.getString(3),
                            mimeType = it.getString(4),
                            createdAt = it.getLong(5)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return assets
    }

    fun getAsset(widgetId: String, assetId: String): WebWidgetAssetRecord? {
        return try {
            val cursor = readableDatabase.query(
                TABLE_ASSET,
                arrayOf(
                    COLUMN_ID,
                    COLUMN_WIDGET_ID,
                    COLUMN_ORIGINAL_NAME,
                    COLUMN_STORED_NAME,
                    COLUMN_MIME_TYPE,
                    COLUMN_CREATED_AT
                ),
                "$COLUMN_WIDGET_ID = ? AND $COLUMN_ID = ?",
                arrayOf(widgetId, assetId),
                null,
                null,
                null,
                "1"
            )
            cursor.use {
                if (!it.moveToFirst()) return null
                WebWidgetAssetRecord(
                    id = it.getString(0),
                    widgetId = it.getString(1),
                    originalName = it.getString(2),
                    storedName = it.getString(3),
                    mimeType = it.getString(4),
                    createdAt = it.getLong(5)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resolveAssetByStoredName(widgetId: String, storedName: String): WebWidgetAssetRecord? {
        return try {
            val cursor = readableDatabase.query(
                TABLE_ASSET,
                arrayOf(
                    COLUMN_ID,
                    COLUMN_WIDGET_ID,
                    COLUMN_ORIGINAL_NAME,
                    COLUMN_STORED_NAME,
                    COLUMN_MIME_TYPE,
                    COLUMN_CREATED_AT
                ),
                "$COLUMN_WIDGET_ID = ? AND $COLUMN_STORED_NAME = ?",
                arrayOf(widgetId, storedName),
                null,
                null,
                null,
                "1"
            )
            cursor.use {
                if (!it.moveToFirst()) return null
                WebWidgetAssetRecord(
                    id = it.getString(0),
                    widgetId = it.getString(1),
                    originalName = it.getString(2),
                    storedName = it.getString(3),
                    mimeType = it.getString(4),
                    createdAt = it.getLong(5)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteAsset(widgetId: String, assetId: String): Boolean {
        return try {
            val asset = getAsset(widgetId, assetId) ?: return false
            val file = File(getAssetDir(widgetId), asset.storedName)
            if (file.exists()) file.delete()
            writableDatabase.delete(
                TABLE_ASSET,
                "$COLUMN_WIDGET_ID = ? AND $COLUMN_ID = ?",
                arrayOf(widgetId, assetId)
            ) >= 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importAssetFromUri(widgetId: String, uri: Uri): WebWidgetAssetRecord? {
        val originalName = queryDisplayName(uri) ?: "asset"
        val mimeType = context.contentResolver.getType(uri) ?: guessMimeType(originalName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                importAssetBytes(widgetId, originalName, mimeType, bytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importAssetBytes(
        widgetId: String,
        originalName: String,
        mimeType: String,
        bytes: ByteArray
    ): WebWidgetAssetRecord? {
        return try {
            val safeOriginalName = originalName.ifBlank { "asset" }
            val extension = safeOriginalName.substringAfterLast('.', "")
            val storedName = buildStoredAssetName(extension)
            val assetDir = getAssetDir(widgetId)
            val assetFile = File(assetDir, storedName)
            FileOutputStream(assetFile).use { it.write(bytes) }

            val record = WebWidgetAssetRecord(
                id = UUID.randomUUID().toString(),
                widgetId = widgetId,
                originalName = safeOriginalName,
                storedName = storedName,
                mimeType = mimeType.ifBlank { "application/octet-stream" },
                createdAt = System.currentTimeMillis() / 1000
            )
            if (saveAssetRecord(record)) record else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAssetFile(widgetId: String, storedName: String): File {
        return File(getAssetDir(widgetId), storedName)
    }

    fun getAssetUrl(widgetId: String, storedName: String): String {
        return "widget://asset/$widgetId/$storedName"
    }

    fun getWidgetDir(widgetId: String): File {
        val dir = File(File(context.filesDir, WEB_WIDGET_DIR), widgetId)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAssetDir(widgetId: String): File {
        val dir = File(getWidgetDir(widgetId), "assets")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun saveAssetRecord(record: WebWidgetAssetRecord): Boolean {
        return try {
            val values = ContentValues().apply {
                put(COLUMN_ID, record.id)
                put(COLUMN_WIDGET_ID, record.widgetId)
                put(COLUMN_ORIGINAL_NAME, record.originalName)
                put(COLUMN_STORED_NAME, record.storedName)
                put(COLUMN_MIME_TYPE, record.mimeType)
                put(COLUMN_CREATED_AT, record.createdAt)
            }
            writableDatabase.insertWithOnConflict(
                TABLE_ASSET,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            ) != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildStoredAssetName(extension: String): String {
        val normalizedExt = extension.lowercase(Locale.getDefault()).trim().trimStart('.')
        return if (normalizedExt.isBlank()) {
            "asset_${UUID.randomUUID()}"
        } else {
            "asset_${UUID.randomUUID()}.$normalizedExt"
        }
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }

    private fun deleteRecursively(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> deleteRecursively(child) }
        }
        try {
            file.delete()
        } catch (_: IOException) {
        }
    }
}
