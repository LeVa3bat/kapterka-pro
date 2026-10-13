package com.example

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KapterkaDatabase
import com.example.data.model.InventoryItem
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.data.repository.KapterkaRepository
import com.example.ui.components.BottomTab
import com.example.ui.components.ExcelReportPreviewDialog
import com.example.ui.components.IssueOperationDialog
import com.example.ui.components.ModernBottomBar
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalBg
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO

/**
 * Captures production Compose components, never a redrawn interface. All data
 * comes from a temporary Room database and real repository operations. The
 * output is a rendered demonstration, not a recording of a physical device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h780dp-xxhdpi")
class GrowthScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val profile = UserProfile(
        callsign = "Кладовщик",
        unitName = "Учебная мастерская",
        isLoggedIn = true,
        isProActive = true,
        isOnline = false,
        proDaysLeft = 27
    )
    private val points = listOf(
        WarehousePoint("workshop_store", "Кладовая", "Инструмент мастерской", true, 0),
        WarehousePoint("workshop_bench", "Рабочее место", "Учебный верстак", false, 1)
    )
    private val items = listOf(
        InventoryItem("workshop_screwdriver", "Отвёртка", "Инструмент", "Ручной инструмент", "шт.", isCustom = true),
        InventoryItem("workshop_wrench", "Гаечный ключ", "Инструмент", "Ручной инструмент", "шт.", isCustom = true),
        InventoryItem("workshop_drill", "Дрель", "Инструмент", "Электроинструмент", "шт.", isCustom = true)
    )
    private data class Snapshot(val stocks: List<StockRecord>, val operations: List<OperationRecord>)
    private data class SubmittedIssue(
        val fromId: String,
        val fromName: String,
        val toId: String,
        val toName: String,
        val entries: List<OperationItemEntry>,
        val comment: String
    )
    private lateinit var repository: KapterkaRepository
    private var snapshot by mutableStateOf(Snapshot(emptyList(), emptyList()))
    private var destination by mutableStateOf(AppDestination.HOME)
    private var showIssue by mutableStateOf(false)
    private var showReport by mutableStateOf(false)
    private var submittedIssue: SubmittedIssue? = null
    private val captures = JSONArray()
    private val output = File("build/outputs/growth-capture")

    @Test
    fun captureWorkshopScreensAndDemonstrationFrames() = runBlocking {
        assumeTrue("Capture runs only with -Proborazzi.test.record=true", System.getProperty("roborazzi.test.record") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, KapterkaDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            output.mkdirs()
            val dao = database.kapterkaDao()
            dao.saveUserProfile(profile)
            dao.insertPoints(points)
            dao.insertItems(items)
            repository = KapterkaRepository(dao, null)
            repository.recordIncome(points[0].id, points[0].name, "Учебная поставка", listOf(
                OperationItemEntry(items[0].id, items[0].name, "шт.", 12),
                OperationItemEntry(items[1].id, items[1].name, "шт.", 8),
                OperationItemEntry(items[2].id, items[2].name, "шт.", 2)
            ), "Только вымышленные данные", profile.callsign)
            snapshot = readSnapshot()
            assertEquals(12, screwdriverQuantity(points[0].id))
            compose.setContent { WorkshopInterface() }
            capture("01-dashboard-before")

            compose.onAllNodesWithText("Кладовая").onFirst().performClick()
            capture("02-stock-before")
            compose.onNodeWithTag("op_issue_button").performClick()
            capture("03-issue-empty")
            compose.onNode(hasSetTextAction() and hasText("Введите название")).performTextInput("Отвёртка")
            compose.onAllNodesWithText("Отвёртка").onLast().performClick()
            compose.onNodeWithText("+").performClick()
            compose.onNodeWithText("+").performClick()
            capture("04-issue-ready")
            compose.onNodeWithTag("submit_issue_button").performClick()
            val issue = requireNotNull(submittedIssue)
            assertEquals(points[0].id, issue.fromId)
            assertEquals(points[1].id, issue.toId)
            assertEquals(3, issue.entries.single().quantity)
            repository.recordIssue(issue.fromId, issue.fromName, issue.toId, issue.toName, issue.entries, issue.comment, profile.callsign)
            val updated = readSnapshot()
            compose.runOnIdle { snapshot = updated }
            assertEquals(9, screwdriverQuantity(points[0].id))
            assertEquals(3, screwdriverQuantity(points[1].id))
            assertEquals(12, snapshot.stocks.filter { it.itemId == items[0].id }.sumOf { it.quantity })
            capture("05-stock-after")
            compose.onAllNodesWithText("Кладовая").onFirst().performClick()
            capture("06-dashboard-after")

            compose.onNodeWithTag("nav_history").performClick()
            capture("07-journal")
            val savedIssue = snapshot.operations.single { it.type == OperationType.ISSUE }
            assertEquals(3, repository.parseOperationItems(savedIssue.itemsJson).single().quantity)
            compose.onNodeWithTag("operation_card_${savedIssue.id}").performClick()
            capture("08-journal-expanded")
            compose.onNodeWithTag("nav_home").performClick()
            compose.onAllNodesWithText("Кладовая").onFirst().performClick()
            capture("09-local-stock")

            val downloads = DemoDownloadsProvider(File(context.filesDir, "growth-demo-download.xlsx"))
            ShadowContentResolver.registerProviderInternal("media", downloads)
            compose.onNodeWithContentDescription("Отчёты").performClick()
            capture("10-report-preview")
            compose.onNodeWithText("Скачать Excel").performClick()
            compose.waitForIdle()
            val exported = requireNotNull(File(context.cacheDir, "exports").listFiles()?.singleOrNull { it.name.endsWith(".xlsx") })
            assertTrue(exported.length() > 0)
            val sheet = worksheetXml(exported)
            assertTrue(sheet.contains("Отвёртка"))
            assertTrue(sheet.contains(">9<"))
            val proof = File(output, "demo-point-stock.xlsx")
            proof.writeBytes(exported.readBytes())
            capture("11-report-exported")

            val manifest = JSONObject()
                .put("kind", "Rendered production Compose demonstration on fictitious data; not physical-device recording")
                .put("appVersionName", BuildConfig.VERSION_NAME)
                .put("appVersionCode", BuildConfig.VERSION_CODE)
                .put("captureDateMoscow", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }.format(Date()))
                .put("androidSdk", 34)
                .put("source", "app/src/test/java/com/example/GrowthScreenshotTest.kt")
                .put("renderer", "Robolectric native graphics + Roborazzi; real production Compose components")
                .put("dataOrigin", "Temporary in-memory Room; KapterkaRepository.recordIncome/recordIssue; syncManager=null; no keys/QR")
                .put("verified", JSONObject().put("before", 12).put("issued", 3).put("storeAfter", 9).put("benchAfter", 3).put("conservedTotal", 12).put("xlsxContainsStoreQuantity", 9))
                .put("frames", captures)
                .put("export", JSONObject().put("file", proof.name).put("sha256", sha256(proof)))
            File(output, "capture-source.json").writeText(manifest.toString(2))
        } finally {
            database.close()
        }
    }

    private suspend fun readSnapshot() = Snapshot(repository.allStockRecords.first(), repository.allOperations.first())
    private fun screwdriverQuantity(pointId: String) = snapshot.stocks.single { it.pointId == pointId && it.itemId == items[0].id }.quantity

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        val file = File(output, "$name.png").absoluteFile
        captureScreenRoboImage(file.path)
        assertTrue("Captured frame missing: $file", file.isFile && file.length() > 0)
        val pixels = ImageIO.read(file)
        captures.put(JSONObject().put("file", file.name).put("sha256", sha256(file)).put("width", pixels.width).put("height", pixels.height))
    }

    @Composable
    private fun WorkshopInterface() {
        MyApplicationTheme(darkTheme = true) {
            Column(modifier = Modifier.fillMaxSize().background(TacticalBg)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (destination == AppDestination.HISTORY) {
                        HistoryScreen(snapshot.operations, null, "", items, listOf("Инструмент"), {}, {}, repository::parseOperationItems)
                    } else {
                        MainDashboardScreen(
                            profile = profile, points = points, catalogItems = items,
                            stockRecords = snapshot.stocks, operations = snapshot.operations,
                            searchQuery = "", onSearchChange = {}, selectedCategory = "Все виды", onSelectCategory = {},
                            availableCategories = listOf("Инструмент"), onIncomeClick = {}, onTransferClick = {},
                            onIssueClick = { showIssue = true }, onExpenditureClick = {}, onAddPointClick = {},
                            onEditPointClick = {}, onAdjustStock = { _, _, _, _, _, _, _ -> }, onSyncClick = {},
                            onExportClick = { showReport = true }, onBannerClick = {}, onProfileClick = {}, isDarkTheme = true
                        )
                    }
                }
                ModernBottomBar(AppDestination.values().map { BottomTab(it.title, it.icon, it.selectedIcon, it.tag) }, destination.ordinal) {
                    destination = AppDestination.values()[it]
                }
            }
            if (showIssue) {
                IssueOperationDialog(points, items, snapshot.stocks, points[0].id, { showIssue = false }) { fromId, fromName, toId, toName, entries, comment ->
                    submittedIssue = SubmittedIssue(fromId, fromName, toId, toName, entries, comment)
                }
            }
            if (showReport) {
                ExcelReportPreviewDialog(snapshot.operations, snapshot.stocks, points, items,
                    unitName = profile.unitName, initialFormIndex = 1, parseItems = repository::parseOperationItems, onDismiss = { showReport = false })
            }
        }
    }

    private fun worksheetXml(file: File): String {
        ZipInputStream(file.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "xl/worksheets/sheet1.xml") return zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        error("Generated XLSX has no worksheet")
    }

    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    /** A test Downloads provider writes the real export bytes into a temporary file. */
    private class DemoDownloadsProvider(private val file: File) : ContentProvider() {
        override fun onCreate() = true
        override fun insert(uri: Uri, values: ContentValues?) = Uri.parse("content://media/downloads/growth-demo")
        override fun openFile(uri: Uri, mode: String) = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE)
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 1
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = if (file.delete()) 1 else 0
        override fun getType(uri: Uri) = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    }
}
