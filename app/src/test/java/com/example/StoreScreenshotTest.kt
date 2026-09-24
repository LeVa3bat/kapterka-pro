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
import com.example.data.model.InventoryItem
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionItemEntry
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.ui.components.BottomTab
import com.example.ui.components.ModernBottomBar
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalBg
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Store / website screenshots at a real phone size (1080×2340). Screens are
 * drawn with the bottom bar, just like in the app, using demo data.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class StoreScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val now = System.currentTimeMillis()
    private val hour = 3_600_000L

    private val profile = UserProfile(
        callsign = "Старшина",
        unitName = "2 рота",
        unitKey = "kapt_7f3k2",
        email = "sklad@example.ru",
        isLoggedIn = true,
        isProActive = true,
        proDaysLeft = 27
    )

    private val points = listOf(
        WarehousePoint(id = "base", name = "Базовый склад", description = "", isBase = true, orderIndex = 0, createdAt = 1L),
        WarehousePoint(id = "p1", name = "1 взвод", description = "", isBase = false, orderIndex = 1, createdAt = 2L),
        WarehousePoint(id = "p2", name = "2 взвод", description = "", isBase = false, orderIndex = 2, createdAt = 3L),
        WarehousePoint(id = "p3", name = "Склад ГСМ", description = "", isBase = false, orderIndex = 3, createdAt = 4L)
    )

    private fun item(id: String, name: String, service: String, unit: String) =
        InventoryItem(id = id, name = name, serviceCategory = service, subType = "Снабжение", unit = unit, categoryClass = "Кат. 1", standardCode = "", isCustom = false)

    private val items = listOf(
        item("rav_01", "Патрон 5,45х39 ПС", "Служба РАВ", "шт."),
        item("rav_02", "Граната РГД-5", "Служба РАВ", "шт."),
        item("med_01", "Аптечка индивидуальная АИ-4", "Медицинская служба", "шт."),
        item("med_02", "Жгут кровоостанавливающий", "Медицинская служба", "шт."),
        item("vesh_01", "Бронежилет 6Б45", "Вещевая служба и СИБЗ", "компл."),
        item("vesh_02", "Шлем 6Б47", "Вещевая служба и СИБЗ", "шт."),
        item("gsm_01", "Дизельное топливо ДТ", "Служба ГСМ", "л."),
        item("svyaz_01", "Радиостанция Р-187П1", "Служба связи и РЭБ", "шт."),
        item("prod_01", "Сухой паёк ИРП-Б", "Продовольственная служба", "шт.")
    )

    private val stocks = listOf(
        StockRecord(pointId = "base", itemId = "rav_01", quantity = 2400, incomeTotal = 3000, expenseTotal = 600, lastUpdated = 1L),
        StockRecord(pointId = "base", itemId = "rav_02", quantity = 36, incomeTotal = 50, expenseTotal = 14, lastUpdated = 1L),
        StockRecord(pointId = "base", itemId = "med_01", quantity = 42, incomeTotal = 60, expenseTotal = 18, lastUpdated = 1L),
        StockRecord(pointId = "base", itemId = "med_02", quantity = 25, incomeTotal = 40, expenseTotal = 15, lastUpdated = 1L),
        StockRecord(pointId = "base", itemId = "vesh_01", quantity = 12, incomeTotal = 20, expenseTotal = 8, lastUpdated = 1L),
        StockRecord(pointId = "base", itemId = "vesh_02", quantity = 14, incomeTotal = 20, expenseTotal = 6, lastUpdated = 1L),
        StockRecord(pointId = "base", itemId = "prod_01", quantity = 180, incomeTotal = 300, expenseTotal = 120, lastUpdated = 1L),
        StockRecord(pointId = "p1", itemId = "vesh_01", quantity = 8, incomeTotal = 8, expenseTotal = 0, lastUpdated = 1L),
        StockRecord(pointId = "p1", itemId = "svyaz_01", quantity = 4, incomeTotal = 4, expenseTotal = 0, lastUpdated = 1L),
        StockRecord(pointId = "p2", itemId = "med_01", quantity = 18, incomeTotal = 18, expenseTotal = 0, lastUpdated = 1L),
        StockRecord(pointId = "p3", itemId = "gsm_01", quantity = 1450, incomeTotal = 2000, expenseTotal = 550, lastUpdated = 1L)
    )

    private val ops = listOf(
        OperationRecord("o1", OperationType.INCOME, "Служба РАВ", "Базовый склад", "118", "Старшина", "", now - 1 * hour, "Патрон 5,45х39 ПС — 1000 шт., Граната РГД-5 — 20 шт."),
        OperationRecord("o2", OperationType.ISSUE, "Базовый склад", "1 взвод", "", "Старшина", "По заявке", now - 3 * hour, "Бронежилет 6Б45 — 8 компл."),
        OperationRecord("o3", OperationType.TRANSFER, "Базовый склад", "2 взвод", "", "Старшина", "", now - 5 * hour, "Аптечка индивидуальная АИ-4 — 18 шт."),
        OperationRecord("o4", OperationType.EXPENDITURE, "Склад ГСМ", "Списание (ф. 8)", "7", "Старшина", "Заправка техники", now - 26 * hour, "Дизельное топливо ДТ — 550 л."),
        OperationRecord("o5", OperationType.INCOME, "Медслужба", "Базовый склад", "117", "Старшина", "", now - 28 * hour, "Жгут кровоостанавливающий — 40 шт."),
        OperationRecord("o6", OperationType.ISSUE, "Базовый склад", "Сокол", "", "Старшина", "", now - 50 * hour, "Сухой паёк ИРП-Б — 12 шт.")
    )

    private val requests = listOf(
        RequisitionRequest("q1", "Базовый склад", "1 взвод", comment = "[СРОЧНО] к выходу в 18:00", timestamp = now - 2 * hour, itemsJson = "q1"),
        RequisitionRequest("q2", "Базовый склад", "Сокол", status = RequestStatus.PENDING, timestamp = now - 4 * hour, itemsJson = "q2"),
        RequisitionRequest("q3", "Базовый склад", "2 взвод", status = RequestStatus.ASSEMBLING, timestamp = now - 6 * hour, itemsJson = "q3"),
        RequisitionRequest("q4", "Склад ГСМ", "Автовзвод", status = RequestStatus.ISSUED, timestamp = now - 30 * hour, itemsJson = "q4")
    )

    private fun requestItems(json: String) = when (json) {
        "q1" -> listOf(RequisitionItemEntry("Граната РГД-5", 10, "шт."), RequisitionItemEntry("Аптечка индивидуальная АИ-4", 6, "шт."), RequisitionItemEntry("Сухой паёк ИРП-Б", 30, "шт."))
        "q2" -> listOf(RequisitionItemEntry("Шлем 6Б47", 2, "шт."), RequisitionItemEntry("Жгут кровоостанавливающий", 4, "шт."))
        "q3" -> listOf(RequisitionItemEntry("Патрон 5,45х39 ПС", 600, "шт."))
        else -> listOf(RequisitionItemEntry("Дизельное топливо ДТ", 200, "л."))
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

    @Composable
    private fun Dashboard() {
        com.example.ui.screens.MainDashboardScreen(
            profile = profile,
            points = points,
            catalogItems = items,
            stockRecords = stocks,
            operations = ops,
            searchQuery = "",
            onSearchChange = {},
            selectedCategory = "Все виды",
            onSelectCategory = {},
            availableCategories = items.map { it.serviceCategory }.distinct(),
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
            isDarkTheme = true
        )
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_000)
        captureScreenRoboImage("screenshots/store_$name.png")
    }

    @Test
    fun s1_splash() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.screens.SplashScreen(onInitializationComplete = {})
            }
        }
        compose.mainClock.advanceTimeBy(3_900)
        captureScreenRoboImage("screenshots/store_1_splash.png")
    }

    @Test
    fun s2_dashboard() {
        compose.setContent { MyApplicationTheme(darkTheme = true) { WithBar(AppDestination.HOME) { Dashboard() } } }
        shot("2_dashboard")
    }

    @Test
    fun s3_dashboard_expanded() {
        compose.setContent { MyApplicationTheme(darkTheme = true) { WithBar(AppDestination.HOME) { Dashboard() } } }
        compose.onAllNodesWithText("Базовый склад").onFirst().performClick()
        shot("3_stock_table")
    }

    @Test
    fun s4_requests() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.REQUESTS) {
                    com.example.ui.screens.RequestsScreen(
                        profile = profile,
                        points = points,
                        catalogItems = items,
                        stockRecords = stocks,
                        requisitions = requests,
                        onCreateRequisition = { _, _, _, _ -> },
                        onUpdateStatus = { _, _ -> },
                        onDeleteRequisition = {},
                        parseItems = { requestItems(it) }
                    )
                }
            }
        }
        shot("4_requests")
    }

    @Test
    fun s5_history() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.HISTORY) {
                    com.example.ui.screens.HistoryScreen(
                        operations = ops,
                        filterType = null,
                        searchQuery = "",
                        catalogItems = items,
                        availableCategories = items.map { it.serviceCategory }.distinct(),
                        onFilterChange = {},
                        onSearchChange = {},
                        parseItems = { emptyList<OperationItemEntry>() }
                    )
                }
            }
        }
        shot("5_journal")
    }

    @Test
    fun s6_unit_qr() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.HOME) { Dashboard() }
                com.example.ui.components.UnitKeySyncDialog(profile = profile, onRegenerateKey = {}, onForceSync = {}, onDismiss = {})
            }
        }
        shot("6_unit_qr")
    }

    @Test
    fun s7_licence() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                WithBar(AppDestination.HOME) { Dashboard() }
                com.example.ui.components.PersonalLicenseDialog(
                    profile = profile.copy(isProActive = false),
                    licenseStatus = com.example.data.license.FighterLicenseStatus(isDemoActive = true, demoDaysLeft = 3),
                    yooKassaConfig = com.example.data.payment.YooKassaConfig(),
                    onPayYooKassaClick = {},
                    onActivateLicenseKey = {},
                    onTestPaymentConfirm = {},
                    onDismiss = {}
                )
            }
        }
        shot("7_pro")
    }

    @Test
    fun s8_form8() {
        val issueItems = mapOf(
            "a" to listOf(OperationItemEntry("vesh_01", "Бронежилет 6Б45", "компл.", 1), OperationItemEntry("vesh_02", "Шлем 6Б47", "шт.", 1)),
            "b" to listOf(OperationItemEntry("vesh_01", "Бронежилет 6Б45", "компл.", 1), OperationItemEntry("med_01", "Аптечка АИ-4", "шт.", 1)),
            "c" to listOf(OperationItemEntry("vesh_02", "Шлем 6Б47", "шт.", 1), OperationItemEntry("med_01", "Аптечка АИ-4", "шт.", 2))
        )
        val issues = listOf(
            OperationRecord("f1", OperationType.ISSUE, "Базовый склад", "ряд. Иванов И.И.", "", "Старшина", "", now - 3 * hour, "", "a"),
            OperationRecord("f2", OperationType.ISSUE, "Базовый склад", "ефр. Петров П.П.", "", "Старшина", "", now - 2 * hour, "", "b"),
            OperationRecord("f3", OperationType.ISSUE, "Базовый склад", "мл. с-т Сидоров С.С.", "", "Старшина", "", now - 1 * hour, "", "c")
        )
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.components.ExcelReportPreviewDialog(
                    operations = issues,
                    stockRecords = stocks,
                    points = emptyList(),
                    catalogItems = items,
                    unitName = "2 рота",
                    initialFormIndex = 1,
                    parseItems = { issueItems[it].orEmpty() },
                    onDismiss = {}
                )
            }
        }
        shot("8_form8")
    }
}
