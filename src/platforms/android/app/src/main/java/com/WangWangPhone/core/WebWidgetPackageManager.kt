package com.WangWangPhone.core

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class WebWidgetImportResult(
    val success: Boolean,
    val message: String,
    val widget: WebWidgetRecord? = null
)

class WebWidgetPackageManager(
    private val context: Context,
    private val dbHelper: WebWidgetDbHelper
) {

    fun exportWidget(widgetId: String, targetUri: Uri): Boolean {
        val widget = dbHelper.getWidget(widgetId) ?: return false
        val assets = dbHelper.getWidgetAssets(widgetId)
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                ZipOutputStream(output).use { zip ->
                    val manifest = JSONObject().apply {
                        put("version", 1)
                        put(
                            "widget",
                            JSONObject().apply {
                                put("name", widget.name)
                                put("htmlCode", widget.htmlCode)
                                put("cssCode", widget.cssCode)
                                put("jsCode", widget.jsCode)
                                put("spanX", widget.spanX)
                                put("spanY", widget.spanY)
                            }
                        )
                        put(
                            "assets",
                            JSONArray().apply {
                                assets.forEach { asset ->
                                    put(
                                        JSONObject().apply {
                                            put("originalName", asset.originalName)
                                            put("storedName", asset.storedName)
                                            put("mimeType", asset.mimeType)
                                        }
                                    )
                                }
                            }
                        )
                    }

                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    assets.forEach { asset ->
                        val file = dbHelper.getAssetFile(asset.widgetId, asset.storedName)
                        if (!file.exists()) return@forEach
                        zip.putNextEntry(ZipEntry("assets/${asset.storedName}"))
                        FileInputStream(file).use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importWidget(sourceUri: Uri): WebWidgetImportResult {
        return try {
            val manifestText = StringBuilder()
            val assetBytes = mutableMapOf<String, ByteArray>()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            when {
                                entry.name == "manifest.json" -> {
                                    manifestText.append(String(readCurrentEntry(zip), Charsets.UTF_8))
                                }
                                entry.name.startsWith("assets/") -> {
                                    val key = entry.name.removePrefix("assets/")
                                    assetBytes[key] = readCurrentEntry(zip)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            if (manifestText.isBlank()) {
                return WebWidgetImportResult(false, "导入失败：组件包缺少 manifest.json")
            }

            val manifest = JSONObject(manifestText.toString())
            val widgetObject = manifest.optJSONObject("widget")
                ?: return WebWidgetImportResult(false, "导入失败：组件数据格式不正确")

            val sourceName = widgetObject.optString("name", "导入组件")
            val targetName = buildUniqueWidgetName(sourceName)
            val widget = dbHelper.createWidget(
                name = targetName,
                htmlCode = widgetObject.optString("htmlCode", ""),
                cssCode = widgetObject.optString("cssCode", ""),
                jsCode = widgetObject.optString("jsCode", ""),
                spanX = widgetObject.optInt("spanX", 2).coerceAtLeast(1),
                spanY = widgetObject.optInt("spanY", 2).coerceAtLeast(1)
            ) ?: return WebWidgetImportResult(false, "导入失败：无法创建组件记录")

            val assets = manifest.optJSONArray("assets") ?: JSONArray()
            for (index in 0 until assets.length()) {
                val assetObject = assets.optJSONObject(index) ?: continue
                val storedName = assetObject.optString("storedName")
                if (storedName.isBlank()) continue
                val bytes = assetBytes[storedName] ?: continue
                val originalName = assetObject.optString("originalName", storedName)
                val mimeType = assetObject.optString("mimeType", "application/octet-stream")
                dbHelper.importAssetBytes(
                    widgetId = widget.id,
                    originalName = originalName,
                    mimeType = mimeType,
                    bytes = bytes
                )
            }

            WebWidgetImportResult(true, "导入成功", widget)
        } catch (e: Exception) {
            e.printStackTrace()
            WebWidgetImportResult(false, "导入失败：${e.message ?: "未知错误"}")
        }
    }

    private fun buildUniqueWidgetName(baseName: String): String {
        val trimmed = baseName.trim().ifBlank { "导入组件" }
        val allNames = dbHelper.getAllWidgets().map { it.name }.toSet()
        if (trimmed !in allNames) return trimmed
        var index = 1
        while (true) {
            val candidate = "$trimmed（导入$index）"
            if (candidate !in allNames) return candidate
            index++
        }
    }

    private fun readCurrentEntry(zipInputStream: ZipInputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var count = zipInputStream.read(buffer)
        while (count != -1) {
            output.write(buffer, 0, count)
            count = zipInputStream.read(buffer)
        }
        return output.toByteArray()
    }
}
