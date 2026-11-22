package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.example.data.license.FighterLicenseStatus
import com.example.data.local.InitialData
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionItemEntry
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WorkspaceProfileMode
import com.example.data.payment.YooKassaConfig
import com.example.ui.components.BottomTab
import com.example.ui.components.ExcelReportPreviewDialog
import com.example.ui.components.ModernBottomBar
import com.example.ui.components.PersonalLicenseDialog
import com.example.ui.components.UnitKeySyncDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InventoryCatalogScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.ProfileSelectionScreen
import com.example.ui.screens.RequestsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalBg
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.json.JSONArray
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures real high-resolution screenshots (1080x2340) of the redesigned
 * Cobalt-Porcelain Light & Midnight Sapphire Dark Compose UI.
 * Run with: -Proborazzi.test.record=true
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class RedesignShowcaseScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val parseOps: (String) -> List<OperationItemEntry> = { json ->
        if (json.isBlank()) emptyList()
        else try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                OperationItemEntry(
                    itemId = o.optString("itemId"),
                    itemName = o.optString("itemName"),
                    unit = o.optString("unit", "шт."),
                    quantity = o.optInt("quantity", 0),
                    categoryClass = o.optString("categoryClass", "Кат. 1"),
                    reason = o.optString("reason", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Before
    fun onlyOnRecord() {
        assumeTrue(
            "Runs only when recording redesign screenshots (-Proborazzi.test.record=true)",
            System.getProperty("roborazzi.test.record") == "true"
        )
    }

    private val now = 1_795_340_000_000L // 22.11.2026
    private val hour = 3_600_000L

    private val civProfile = UserProfile(
        callsign = "Алексей",
        unitName = "Мастерская № 1",
        unitKey = "kapt_m4k8p2",
        email = "master@kapterka-pro.ru",
        isLoggedIn = true,
        isProActive = true,
        proDaysLeft = 29,
        isOnline = true
    )

    private val milProfile = UserProfile(
        callsign = "ст. прапорщик Петров",
        unitName = "2 мср • КЗ",
        unitKey = "kapt_7f3k29",
        email = "sklad@kapterka-pro.ru",
        isLoggedIn = true,
        isProActive = true,
        proDaysLeft = 27,
        isOnline = true
    )

    private val civPoints = InitialData.getCivilianPoints()
    private val civItems = InitialData.getCivilianItems()
    private val civCategories = InitialData.getCivilianCategories()

    private val civStocks = listOf(
        StockRecord(pointId = "base_sklad", itemId = "civ_el_01", quantity = 8, incomeTotal = 12, expenseTotal = 4, lastUpdated = now),
        StockRecord(pointId = "base_sklad", itemId = "civ_el_02", quantity = 6, incomeTotal = 10, expenseTotal = 4, lastUpdated = now),
        StockRecord(pointId = "base_sklad", itemId = "civ_el_03", quantity = 14, incomeTotal = 18, expenseTotal = 4, lastUpdated = now),
        StockRecord(pointId = "base_sklad", itemId = "civ_con_01", quantity = 45, incomeTotal = 60, expenseTotal = 15, lastUpdated = now),
        StockRecord(pointId = "base_sklad", itemId = "civ_ppe_01", quantity = 28, incomeTotal = 40, expenseTotal = 12, lastUpdated = now),
        StockRecord(pointId = "base_sklad", itemId = "civ_chem_01", quantity = 80, incomeTotal = 120, expenseTotal = 40, lastUpdated = now),
        StockRecord(pointId = "pt_workshop", itemId = "civ_el_01", quantity = 4, incomeTotal = 4, expenseTotal = 0, lastUpdated = now),
        StockRecord(pointId = "pt_site_1", itemId = "civ_con_01", quantity = 15, incomeTotal = 15, expenseTotal = 0, lastUpdated = now),
        StockRecord(pointId = "pt_brigade", itemId = "civ_ppe_01", quantity = 12, incomeTotal = 12, expenseTotal = 0, lastUpdated = now)
    )

    private val civOps = listOf(
        OperationRecord(
            "co1", OperationType.INCOME, "ООО ИнструментТорг", "Основной склад", "УПД-412", "Алексей", "Плановая поставка",
            now - 1 * hour, "Перфоратор SDS-Plus — 6 шт., Диск отрезной 125 мм (металл) — 40 шт.",
            """[{"itemId":"civ_el_01","itemName":"Перфоратор SDS-Plus","unit":"шт.","quantity":6},{"itemId":"civ_con_01","itemName":"Диск отрезной 125 мм (металл)","unit":"шт.","quantity":40}]"""
        ),
        OperationRecord(
            "co2", OperationType.ISSUE, "Основной склад", "Бригадир Смирнов А.В.", "МБ7-18", "Алексей", "Выдача на руки",
            now - 2 * hour, "Перфоратор SDS-Plus — 2 шт., Краги сварочные — 4 пар",
            """[{"itemId":"civ_el_01","itemName":"Перфоратор SDS-Plus","unit":"шт.","quantity":2},{"itemId":"civ_ppe_02","itemName":"Краги сварочные","unit":"пар","quantity":4}]"""
        ),
        OperationRecord(
            "co3", OperationType.TRANSFER, "Основной склад", "Объект № 1", "М11-09", "Алексей", "Перемещение на объект",
            now - 4 * hour, "Диск отрезной 125 мм (металл) — 15 шт., УШМ (Болгарка) 125 мм — 2 шт.",
            """[{"itemId":"civ_con_01","itemName":"Диск отрезной 125 мм (металл)","unit":"шт.","quantity":15},{"itemId":"civ_el_02","itemName":"УШМ (Болгарка) 125 мм","unit":"шт.","quantity":2}]"""
        ),
        OperationRecord(
            "co4", OperationType.EXPENDITURE, "Основной склад", "Списание (МБ-8)", "АКТ-03", "Алексей", "Выработка расходников",
            now - 24 * hour, "Масло моторное 5W-40 — 20 л.",
            """[{"itemId":"civ_chem_01","itemName":"Масло моторное 5W-40","unit":"л.","quantity":20}]"""
        )
    )

    private val milPoints = InitialData.getDefaultPoints()
    private val milItems = InitialData.getDefaultItems().take(12)
    private val milStocks = listOf(
        StockRecord(pointId = "base_sklad", itemId = "rav_01", quantity = 2400, incomeTotal = 3000, expenseTotal = 600, lastUpdated = now),
        StockRecord(pointId = "base_sklad", itemId = "rav_02", quantity = 36, incomeTotal = 50, expenseTotal = 14, lastUpdated = now),
        StockRecord(pointId = "base_sklad", itemId = "rav_03", quantity = 48, incomeTotal = 60, expenseTotal = 12, lastUpdated = now),
        StockRecord(pointId = "pt_pvd", itemId = "rav_01", quantity = 600, incomeTotal = 600, expenseTotal = 0, lastUpdated = now)
    )
    private val milOps = listOf(
        OperationRecord(
            "mo1", OperationType.INCOME, "Служба РАВ", "Основной склад (КЗ)", "118", "Петров И.В.", "",
            now - 5 * hour, "Патрон 5,45х39 мм (ПС/ПП) — 1200 шт., Граната РГД-5 / Ф-1 — 30 шт.",
            """[{"itemId":"rav_01","itemName":"Патрон 5,45х39 мм (ПС/ПП)","unit":"шт.","quantity":1200},{"itemId":"rav_02","itemName":"Граната РГД-5 / Ф-1","unit":"шт.","quantity":30}]"""
        ),
        OperationRecord(
            "mo2", OperationType.ISSUE, "Основной склад (КЗ)", "Иванов А.С.", "Ф8-14", "Петров И.В.", "По раздаточной ведомости",
            now - 3 * hour, "Патрон 5,45х39 мм (ПС/ПП) — 300 шт., Граната РГД-5 / Ф-1 — 4 шт.",
            """[{"itemId":"rav_01","itemName":"Патрон 5,45х39 мм (ПС/ПП)","unit":"шт.","quantity":300},{"itemId":"rav_02","itemName":"Граната РГД-5 / Ф-1","unit":"шт.","quantity":4}]"""
        ),
        OperationRecord(
            "mo3", OperationType.ISSUE, "Основной склад (КЗ)", "Соколов Д.М.", "Ф8-15", "Петров И.В.", "",
            now - 2 * hour, "Патрон 5,45х39 мм (ПС/ПП) — 180 шт.",
            """[{"itemId":"rav_01","itemName":"Патрон 5,45х39 мм (ПС/ПП)","unit":"шт.","quantity":180}]"""
        ),
        OperationRecord(
            "mo4", OperationType.TRANSFER, "Основной склад (КЗ)", "ПВД (Пункт врем. дислокации)", "П-09", "Петров И.В.", "Внутр. перемещение",
            now - 1 * hour, "Патрон 5,45х39 мм (ПС/ПП) — 600 шт.",
            """[{"itemId":"rav_01","itemName":"Патрон 5,45х39 мм (ПС/ПП)","unit":"шт.","quantity":600}]"""
        )
    )

    private val civRequests = listOf(
        RequisitionRequest("rq1", "Основной склад", "Бригадир Смирнов", comment = "[СРОЧНО] на монтаж к 14:00", timestamp = now - 1 * hour, itemsJson = "rq1"),
        RequisitionRequest("rq2", "Основной склад", "Участок № 2", status = RequestStatus.PENDING, timestamp = now - 3 * hour, itemsJson = "rq2"),
        RequisitionRequest("rq3", "Основной склад", "Цех сборки", status = RequestStatus.ASSEMBLING, timestamp = now - 5 * hour, itemsJson = "rq3")
    )

    private fun parseReq(json: String) = when (json) {
        "rq1" -> listOf(
            RequisitionItemEntry("Перфоратор SDS-Plus (800 Вт)", 2, "шт."),
            RequisitionItemEntry("Диск отрезной 125х1,2 мм (металл)", 15, "шт.")
        )
        "rq2" -> listOf(RequisitionItemEntry("Перчатки рабочие х/б с ПВХ", 12, "пар"))
        else -> listOf(RequisitionItemEntry("Масло моторное 5W-40", 10, "л."))
    }

    @Composable
    private fun WithBar(tab: AppDestination, content: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxSize().background(TacticalBg)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
            ModernBottomBar(
                tabs = AppDestination.values().map {
                    BottomTab(it.title, it.icon, it.selectedIcon, it.tag, if (it == AppDestination.REQUESTS) 2 else 0)
                },
                selectedIndex = tab.ordinal,
                onSelect = {}
            )
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_200)
        captureScreenRoboImage("build/outputs/v4-showcase/$name.png")
    }

    @Test
    fun v4_01_auth_email() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                AuthScreen(
                    currentProfile = null,
                    onCompleteAuth = {}
                )
            }
        }
        capture("01_auth_email")
    }

    @Test
    fun v4_02_profile_picker() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                ProfileSelectionScreen(onConfirmProfile = {})
            }
        }
        capture("02_profile_picker")
    }

    @Test
    fun v4_03_dashboard_civilian_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.HOME) {
                    MainDashboardScreen(
                        profile = civProfile,
                        points = civPoints,
                        catalogItems = civItems,
                        stockRecords = civStocks,
                        operations = civOps,
                        searchQuery = "",
                        onSearchChange = {},
                        selectedCategory = "Все виды",
                        onSelectCategory = {},
                        availableCategories = civCategories,
                        onIncomeClick = {},
                        onTransferClick = {},
                        onIssueClick = {},
                        onExpenditureClick = {},
                        onAddPointClick = {},
                        onEditPointClick = {},
                        onAdjustStock = { _, _, _, _, _, _, _ -> },
                        onSyncClick = {},
                        onExportClick = {},
                        onBannerClick = {},
                        onProfileClick = {},
                        isDarkTheme = true,
                        onToggleTheme = {},
                        profileMode = WorkspaceProfileMode.CIVILIAN
                    )
                }
            }
        }
        capture("03_dashboard_civilian_dark")
    }

    @Test
    fun v4_04_stock_table_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.HOME) {
                    MainDashboardScreen(
                        profile = civProfile,
                        points = civPoints,
                        catalogItems = civItems,
                        stockRecords = civStocks,
                        operations = civOps,
                        searchQuery = "",
                        onSearchChange = {},
                        selectedCategory = "Все виды",
                        onSelectCategory = {},
                        availableCategories = civCategories,
                        onIncomeClick = {},
                        onTransferClick = {},
                        onIssueClick = {},
                        onExpenditureClick = {},
                        onAddPointClick = {},
                        onEditPointClick = {},
                        onAdjustStock = { _, _, _, _, _, _, _ -> },
                        onSyncClick = {},
                        onExportClick = {},
                        onBannerClick = {},
                        onProfileClick = {},
                        isDarkTheme = true,
                        onToggleTheme = {},
                        profileMode = WorkspaceProfileMode.CIVILIAN
                    )
                }
            }
        }
        compose.onAllNodesWithText("Основной склад").onFirst().performClick()
        capture("04_stock_table_dark")
    }

    @Test
    fun v4_05_dashboard_military_light() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                WithBar(AppDestination.HOME) {
                    MainDashboardScreen(
                        profile = milProfile,
                        points = milPoints,
                        catalogItems = milItems,
                        stockRecords = milStocks,
                        operations = milOps,
                        searchQuery = "",
                        onSearchChange = {},
                        selectedCategory = "Все виды",
                        onSelectCategory = {},
                        availableCategories = InitialData.getDefaultCategories(),
                        onIncomeClick = {},
                        onTransferClick = {},
                        onIssueClick = {},
                        onExpenditureClick = {},
                        onAddPointClick = {},
                        onEditPointClick = {},
                        onAdjustStock = { _, _, _, _, _, _, _ -> },
                        onSyncClick = {},
                        onExportClick = {},
                        onBannerClick = {},
                        onProfileClick = {},
                        isDarkTheme = false,
                        onToggleTheme = {},
                        profileMode = WorkspaceProfileMode.MILITARY
                    )
                }
            }
        }
        capture("05_dashboard_military_light")
    }

    @Test
    fun v4_06_catalog_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.CATALOG) {
                    InventoryCatalogScreen(
                        items = civItems,
                        availableCategories = civCategories,
                        stockRecords = civStocks,
                        profileMode = WorkspaceProfileMode.CIVILIAN,
                        onAddNewItemClick = {}
                    )
                }
            }
        }
        capture("06_catalog_dark")
    }

    @Test
    fun v4_07_requests_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.REQUESTS) {
                    RequestsScreen(
                        profile = civProfile,
                        points = civPoints,
                        catalogItems = civItems,
                        stockRecords = civStocks,
                        requisitions = civRequests,
                        onCreateRequisition = { _, _, _, _ -> },
                        onUpdateStatus = { _, _ -> },
                        onDeleteRequisition = {},
                        parseItems = ::parseReq
                    )
                }
            }
        }
        capture("07_requests_dark")
    }

    @Test
    fun v4_08_journal_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.HISTORY) {
                    HistoryScreen(
                        operations = civOps,
                        filterType = null,
                        searchQuery = "",
                        catalogItems = civItems,
                        availableCategories = civCategories,
                        onFilterChange = {},
                        onSearchChange = {},
                        parseItems = parseOps
                    )
                }
            }
        }
        capture("08_journal_dark")
    }

    @Test
    fun v4_09_unit_qr_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize().background(TacticalBg)) {
                    UnitKeySyncDialog(
                        profile = civProfile,
                        onRegenerateKey = {},
                        onForceSync = {},
                        onDismiss = {}
                    )
                }
            }
        }
        capture("09_unit_qr_dark")
    }

    @Test
    fun v4_10_pro_payment_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize().background(TacticalBg)) {
                    PersonalLicenseDialog(
                        profile = civProfile.copy(isProActive = false, demoDaysLeft = 3),
                        licenseStatus = FighterLicenseStatus(
                            isProActive = false,
                            isDemoActive = true,
                            demoDaysLeft = 3
                        ),
                        yooKassaConfig = YooKassaConfig(),
                        onActivateLicenseKey = {},
                        onTestPaymentConfirm = {},
                        onDismiss = {}
                    )
                }
            }
        }
        capture("10_pro_payment_dark")
    }

    @Test
    fun v4_11_report_form8_military() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize().background(TacticalBg)) {
                    ExcelReportPreviewDialog(
                        operations = milOps,
                        stockRecords = milStocks,
                        points = emptyList(),
                        catalogItems = milItems,
                        requisitions = emptyList(),
                        unitName = milProfile.unitName,
                        initialFormIndex = 1,
                        parseItems = parseOps,
                        profileMode = WorkspaceProfileMode.MILITARY,
                        responsiblePerson = milProfile.callsign,
                        onDismiss = {}
                    )
                }
            }
        }
        capture("11_report_form8_military")
    }

    @Test
    fun v4_12_report_form18_military() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize().background(TacticalBg)) {
                    ExcelReportPreviewDialog(
                        operations = milOps,
                        stockRecords = milStocks,
                        points = emptyList(),
                        catalogItems = milItems,
                        requisitions = emptyList(),
                        unitName = milProfile.unitName,
                        initialFormIndex = 2,
                        parseItems = parseOps,
                        profileMode = WorkspaceProfileMode.MILITARY,
                        responsiblePerson = milProfile.callsign,
                        onDismiss = {}
                    )
                }
            }
        }
        capture("12_report_form18_military")
    }

    @Test
    fun v4_13_report_m11_civilian() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize().background(TacticalBg)) {
                    ExcelReportPreviewDialog(
                        operations = civOps,
                        stockRecords = civStocks,
                        points = emptyList(),
                        catalogItems = civItems,
                        requisitions = civRequests,
                        unitName = civProfile.unitName,
                        initialFormIndex = 1,
                        parseItems = parseOps,
                        profileMode = WorkspaceProfileMode.CIVILIAN,
                        responsiblePerson = civProfile.callsign,
                        onDismiss = {}
                    )
                }
            }
        }
        capture("13_report_m11_civilian")
    }
}
