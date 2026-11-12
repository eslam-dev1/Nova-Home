package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackupManager
import com.example.data.SettingsManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BackupRestoreTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
        backupManager = BackupManager(context, settingsManager)
    }

    @Test
    fun roundTripExportImportPreservesSettings() = runTest {
        settingsManager.setGridSize("4x4")
        settingsManager.setIconShape("square")
        settingsManager.setShowLabels(false)
        settingsManager.setEdgeBackGesture(false)
        settingsManager.hideApp("com.example.hidden")

        val tempFile = File.createTempFile("backup_test", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        val uri = android.net.Uri.fromFile(tempFile)

        val exportResult = backupManager.exportSettings(uri)
        assertThat(exportResult.isSuccess).isTrue()

        settingsManager.setGridSize("5x5")
        settingsManager.setIconShape("circle")
        settingsManager.setShowLabels(true)
        settingsManager.setEdgeBackGesture(true)
        settingsManager.unhideApp("com.example.hidden")

        val importResult = backupManager.importSettings(uri)
        assertThat(importResult.isSuccess).isTrue()

        assertThat(settingsManager.gridSize.first()).isEqualTo("4x4")
        assertThat(settingsManager.iconShape.first()).isEqualTo("square")
        assertThat(settingsManager.showLabels.first()).isFalse()
        assertThat(settingsManager.edgeBackGesture.first()).isFalse()
        assertThat(settingsManager.hiddenApps.first()).contains("com.example.hidden")
    }

    @Test
    fun rejectCorruptedJson() = runTest {
        val tempFile = File.createTempFile("corrupt_test", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.writeText("{ invalid json }")
        val uri = android.net.Uri.fromFile(tempFile)

        val result = backupManager.importSettings(uri)
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun rejectUnsupportedSchemaVersion() = runTest {
        val tempFile = File.createTempFile("schema_test", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.writeText("""{"schemaVersion": 99, "exportedAt": 123456789}""")
        val uri = android.net.Uri.fromFile(tempFile)

        val result = backupManager.importSettings(uri)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Unsupported backup schema version")
    }

    @Test
    fun shouldRejectMissingRequiredFields() = runTest {
        val tempFile = File.createTempFile("missing_field", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.writeText("""{"schemaVersion": 1, "exportedAt": 123456789, "appVersion": "1.0", "showLabels": true, "iconShape": "circle"}""")
        val uri = android.net.Uri.fromFile(tempFile)

        val result = backupManager.importSettings(uri)
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun shouldHandleEmptyHiddenAppsList() = runTest {
        settingsManager.hideApp("com.temp.hidden")
        assertThat(settingsManager.hiddenApps.first()).isNotEmpty()

        val tempFile = File.createTempFile("empty_hidden", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.writeText("""{"schemaVersion": 1, "exportedAt": 123456789, "appVersion": "1.0", "showLabels": true, "iconShape": "circle", "gridSize": "5x5", "deviceProfile": "Pixel", "edgeBackGesture": true, "hiddenApps": []}""")
        val uri = android.net.Uri.fromFile(tempFile)

        val result = backupManager.importSettings(uri)
        assertThat(result.isSuccess).isTrue()
        assertThat(settingsManager.hiddenApps.first()).isEmpty()
    }

    @Test
    fun shouldHandleNullOptionalFields() = runTest {
        val tempFile = File.createTempFile("null_optional", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.writeText("""{"schemaVersion": 1, "exportedAt": 123456789, "appVersion": "1.0", "showLabels": true, "iconShape": "circle", "gridSize": "5x5", "deviceProfile": "Pixel", "edgeBackGesture": true, "iconPack": null, "hiddenApps": []}""")
        val uri = android.net.Uri.fromFile(tempFile)

        val result = backupManager.importSettings(uri)
        assertThat(result.isSuccess).isTrue()
        assertThat(settingsManager.iconPack.first()).isNull()
    }

    @Test
    fun shouldHandleVeryLargeHiddenAppsList() = runTest {
        val largeAppsList = (1..1500).map { "com.example.app$it" }
        val jsonArray = largeAppsList.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

        val tempFile = File.createTempFile("large_hidden", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.writeText("""{"schemaVersion": 1, "exportedAt": 123456789, "appVersion": "1.0", "showLabels": true, "iconShape": "circle", "gridSize": "5x5", "deviceProfile": "Pixel", "edgeBackGesture": true, "hiddenApps": $jsonArray}""")
        val uri = android.net.Uri.fromFile(tempFile)

        val result = backupManager.importSettings(uri)
        assertThat(result.isSuccess).isTrue()
        val restoredHidden = settingsManager.hiddenApps.first()
        assertThat(restoredHidden).hasSize(1500)
        assertThat(restoredHidden).contains("com.example.app1500")
    }

    @Test
    fun shouldAcceptCurrentSchemaVersion_asExactMatch() = runTest {
        val tempFile = File.createTempFile("current_schema", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.writeText("""{"schemaVersion": 1, "exportedAt": 123456789, "appVersion": "1.0", "showLabels": true, "iconShape": "circle", "gridSize": "5x5", "deviceProfile": "Pixel", "edgeBackGesture": true, "hiddenApps": []}""")
        val uri = android.net.Uri.fromFile(tempFile)

        val result = backupManager.importSettings(uri)
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun shouldRejectNegativeSchemaVersion() = runTest {
        val tempFile = File.createTempFile("negative_schema", ".json", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.writeText("""{"schemaVersion": -1, "exportedAt": 123456789, "appVersion": "1.0", "showLabels": true, "iconShape": "circle", "gridSize": "5x5", "deviceProfile": "Pixel", "edgeBackGesture": true, "hiddenApps": []}""")
        val uri = android.net.Uri.fromFile(tempFile)

        val result = backupManager.importSettings(uri)
        assertThat(result.isFailure).isTrue()
    }
}
