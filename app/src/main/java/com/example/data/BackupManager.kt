package com.example.data

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context, private val settingsManager: SettingsManager) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupPayload::class.java)

    suspend fun exportSettings(uri: Uri): Result<Unit> {
        return try {
            val payload = BackupPayload(
                schemaVersion = 1,
                exportedAt = System.currentTimeMillis(),
                appVersion = "1.0",
                showLabels = settingsManager.showLabels.first(),
                iconShape = settingsManager.iconShape.first(),
                gridSize = settingsManager.gridSize.first(),
                deviceProfile = settingsManager.deviceProfile.first(),
                edgeBackGesture = settingsManager.edgeBackGesture.first(),
                iconPack = settingsManager.iconPack.first(),
                hiddenApps = settingsManager.hiddenApps.first().toList()
            )

            val json = adapter.toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            } ?: return Result.failure(Exception("Cannot open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importSettings(uri: Uri): Result<String> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return Result.failure(Exception("Cannot open input stream"))

            val rawMap = (moshi.adapter(Map::class.java).fromJson(json) as? Map<*, *>)
                ?: return Result.failure(Exception("Invalid or corrupted backup JSON"))

            val schemaVersion = (rawMap["schemaVersion"] as? Number)?.toInt()
            if (schemaVersion == null || schemaVersion != 1) {
                return Result.failure(Exception("Unsupported backup schema version: $schemaVersion"))
            }

            val requiredKeys = listOf(
                "schemaVersion",
                "exportedAt",
                "gridSize",
                "iconShape",
                "showLabels",
                "deviceProfile",
                "edgeBackGesture",
                "hiddenApps"
            )
            for (key in requiredKeys) {
                if (!rawMap.containsKey(key)) {
                    return Result.failure(Exception("Missing required field: $key"))
                }
            }

            val payload = adapter.fromJson(json) ?: return Result.failure(Exception("Invalid or corrupted backup JSON"))

            // Apply settings
            settingsManager.setShowLabels(payload.showLabels)
            settingsManager.setIconShape(payload.iconShape)
            settingsManager.setGridSize(payload.gridSize)
            settingsManager.setDeviceProfile(payload.deviceProfile)
            settingsManager.setEdgeBackGesture(payload.edgeBackGesture)
            settingsManager.setIconPack(payload.iconPack)
            
            // Clear and set hidden apps
            val currentHidden = settingsManager.hiddenApps.first()
            currentHidden.forEach { settingsManager.unhideApp(it) }
            payload.hiddenApps.forEach { settingsManager.hideApp(it) }

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(payload.exportedAt))
            Result.success("Successfully imported backup from $dateStr")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse backup file: ${e.localizedMessage}"))
        }
    }
}
