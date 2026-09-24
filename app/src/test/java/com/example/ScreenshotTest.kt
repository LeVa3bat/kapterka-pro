package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.data.model.InventoryItem
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders key screens with sample data so UI changes can be reviewed as images.
 * CI stores the PNGs under app/build/outputs/roborazzi.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h1600dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val profile = UserProfile(
        callsign = "Лева",
        unitName = "МинБат",
        unitKey = "kapt_1111",
        isLoggedIn = true,
        isProActive = true,
        proDaysLeft = 9
    )

    private val points = listOf(
        WarehousePoint(id = "base_sklad", name = "Базовый склад (КЗ)", description = "", isBase = true, orderIndex = 0, createdAt = 1L),
        WarehousePoint(id = "p2", name = "1 взвод", description = "", isBase = false, orderIndex = 1, createdAt = 2L),
        WarehousePoint(id = "p3", name = "Склад ГСМ", description = "", isBase = false, orderIndex = 2, createdAt = 3L)
    )

    private val items = listOf(
        item("rav_01", "Патрон 5,45х39 ПС", "Служба РАВ", "шт."),
        item("rav_02", "Граната РГД-5", "Служба РАВ", "шт."),
        item("med_01", "Аптечка индивидуальная АИ-4", "Медицинская служба", "шт."),
        item("vesh_01", "Бронежилет 6Б45", "Вещевая служба и СИБЗ", "компл."),
        item("gsm_01", "Дизельное топливо ДТ", "Служба ГСМ", "л."),
        item("svyaz_01", "Радиостанция Р-187П1", "Служба связи и РЭБ", "шт.")
    )

    private val stocks = listOf(
        StockRecord(pointId = "base_sklad", itemId = "rav_01", quantity = 1200, incomeTotal = 2000, expenseTotal = 800, lastUpdated = 1L),
        StockRecord(pointId = "base_sklad", itemId = "rav_02", quantity = 18, incomeTotal = 30, expenseTotal = 12, lastUpdated = 1L),
        StockRecord(pointId = "base_sklad", itemId = "med_01", quantity = 0, incomeTotal = 10, expenseTotal = 10, lastUpdated = 1L),
        StockRecord(pointId = "base_sklad", itemId = "vesh_01", quantity = 7, incomeTotal = 7, expenseTotal = 0, lastUpdated = 1L),
        StockRecord(pointId = "p2", itemId = "svyaz_01", quantity = 3, incomeTotal = 4, expenseTotal = 1, lastUpdated = 1L),
        StockRecord(pointId = "p3", itemId = "gsm_01", quantity = 450, incomeTotal = 600, expenseTotal = 150, lastUpdated = 1L)
    )

    private fun item(id: String, name: String, service: String, unit: String) = InventoryItem(
        id = id,
        name = name,
        serviceCategory = service,
        subType = "Снабжение",
        unit = unit,
        categoryClass = "Кат. 1",
        standardCode = "",
        isCustom = false
    )

    private fun dashboard(dark: Boolean) {
        compose.setContent {
            MyApplicationTheme(darkTheme = dark) {
                MainDashboardScreen(
                    profile = profile,
                    points = points,
                    catalogItems = items,
                    stockRecords = stocks,
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
                    onAdjustStock = { _, _, _, _, _ -> },
                    onSyncClick = {},
                    onExportClick = {},
                    onBannerClick = {},
                    onProfileClick = {},
                    isDarkTheme = dark
                )
            }
        }
    }

    @Test
    fun dashboard_dark() {
        dashboard(dark = true)
        compose.onRoot().captureRoboImage("screenshots/dashboard_dark.png")
    }

    @Test
    fun dashboard_dark_point_expanded() {
        dashboard(dark = true)
        compose.onAllNodesWithText("Базовый склад (КЗ)").onLast().performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/dashboard_dark_expanded.png")
    }

    @Test
    fun command_center_dark() {
        val now = System.currentTimeMillis()
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.components.CommandCenterContent(
                    stats = com.example.data.admin.CommandCenterStats(
                        generatedAt = now,
                        devicesOnline = 7,
                        unitsOnline = 4,
                        devicesToday = 23,
                        devicesWeek = 61,
                        usersTotal = 148,
                        usersOn36 = 37,
                        usersNewToday = 3,
                        usersNewWeek = 19,
                        unitsTotal = 52,
                        licensesVerified = 12,
                        licensesLegacy = 9,
                        licensesExpiring7d = 4,
                        paidThisMonth = 8,
                        revenueMonthRub = 3920,
                        registrations14d = listOf(2, 4, 1, 0, 3, 5, 2, 6, 1, 3, 4, 2, 5, 3),
                        onlineList = listOf(
                            com.example.data.admin.OnlineDevice("Лева", "МинБат", "Samsung A52", now - 60_000),
                            com.example.data.admin.OnlineDevice("Сокол", "2 рота", "Xiaomi Redmi 12", now - 5 * 60_000),
                            com.example.data.admin.OnlineDevice("Кедр", "Взвод связи", "Pixel 7", now - 11 * 60_000)
                        )
                    ),
                    errorMessage = "",
                    onRefresh = {},
                    onOpenRegistry = {},
                    onDismiss = {}
                )
            }
        }
        compose.onRoot().captureRoboImage("screenshots/command_center_dark.png")
    }

    @Test
    fun splash_dark() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.screens.SplashScreen(onInitializationComplete = {})
            }
        }
        compose.mainClock.advanceTimeBy(3_900)
        compose.onRoot().captureRoboImage("screenshots/splash_dark.png")
    }

    private val ops = listOf(
        com.example.data.model.OperationRecord("op1", com.example.data.model.OperationType.INCOME, "Служба РАВ / Тыл", "Базовый склад (КЗ)", "№ 12", "Лева", "Накладная 45", 1_727_160_000_000L, "Патрон 5,45х39 ПС — 2000 шт., Граната РГД-5 — 30 шт."),
        com.example.data.model.OperationRecord("op2", com.example.data.model.OperationType.ISSUE, "Базовый склад (КЗ)", "Сокол", "", "Лева", "", 1_727_170_000_000L, "Бронежилет 6Б45 — 2 компл."),
        com.example.data.model.OperationRecord("op3", com.example.data.model.OperationType.TRANSFER, "Базовый склад (КЗ)", "1 взвод", "", "Лева", "", 1_727_180_000_000L, "Радиостанция Р-187П1 — 4 шт.")
    )

    @Test
    fun history_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.screens.HistoryScreen(
                    operations = ops,
                    filterType = null,
                    searchQuery = "",
                    catalogItems = items,
                    availableCategories = items.map { it.serviceCategory }.distinct(),
                    onFilterChange = {},
                    onSearchChange = {},
                    parseItems = { emptyList() }
                )
            }
        }
        compose.onRoot().captureRoboImage("screenshots/history_dark.png")
    }

    @Test
    fun requests_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.screens.RequestsScreen(
                    profile = profile,
                    points = points,
                    catalogItems = items,
                    stockRecords = stocks,
                    requisitions = listOf(
                        com.example.data.model.RequisitionRequest("r1", "1 взвод", "Сокол", comment = "Срочно", timestamp = 1_727_160_000_000L, itemsSummary = "Аптечка АИ-4 — 10 шт."),
                        com.example.data.model.RequisitionRequest("r2", "Склад ГСМ", "Кедр", status = com.example.data.model.RequestStatus.COLLECTED, timestamp = 1_727_150_000_000L, itemsSummary = "Дизельное топливо — 200 л.")
                    ),
                    onCreateRequisition = { _, _, _, _ -> },
                    onUpdateStatus = { _, _ -> },
                    onDeleteRequisition = {},
                    parseItems = { emptyList() }
                )
            }
        }
        compose.onRoot().captureRoboImage("screenshots/requests_dark.png")
    }

    @Test
    fun inventory_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.screens.InventoryCatalogScreen(
                    items = items,
                    availableCategories = items.map { it.serviceCategory }.distinct(),
                    onAddNewItemClick = {}
                )
            }
        }
        compose.onRoot().captureRoboImage("screenshots/inventory_dark.png")
    }

    @Test
    fun more_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.screens.MoreSettingsScreen(
                    profile = profile,
                    availableCategories = items.map { it.serviceCategory }.distinct(),
                    onDeleteCategory = {},
                    onAddCategory = {},
                    onResetCategories = {},
                    onSyncClick = {},
                    onOpenConnectCodeDialog = {},
                    onOpenPaymentPro = {},
                    onExportForm8Click = {},
                    onExportForm18Click = {},
                    onLogoutClick = {},
                    onResetDataClick = {},
                    isDarkTheme = true
                )
            }
        }
        compose.onRoot().captureRoboImage("screenshots/more_dark.png")
    }

    @Test
    fun income_dialog_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.components.IncomeOperationDialog(
                    profile = profile,
                    points = points,
                    catalogItems = items,
                    stockRecords = stocks,
                    initialPointId = "base_sklad",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _ -> }
                )
            }
        }
        compose.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("screenshots/income_dialog_dark.png")
    }

    private val reportOps = listOf(
        com.example.data.model.OperationRecord("r1", com.example.data.model.OperationType.INCOME, "Служба РАВ", "Базовый склад (КЗ)", "12", "Лева", "", 1_727_160_000_000L, "", "in1"),
        com.example.data.model.OperationRecord("r2", com.example.data.model.OperationType.ISSUE, "Базовый склад (КЗ)", "Сокол", "3", "Лева", "", 1_727_170_000_000L, "", "is1"),
        com.example.data.model.OperationRecord("r3", com.example.data.model.OperationType.ISSUE, "Базовый склад (КЗ)", "Кедр", "4", "Лева", "", 1_727_180_000_000L, "", "is2"),
        com.example.data.model.OperationRecord("r4", com.example.data.model.OperationType.EXPENDITURE, "Базовый склад (КЗ)", "", "5", "Лева", "", 1_727_190_000_000L, "", "ex1")
    )

    private fun reportItems(json: String): List<com.example.data.model.OperationItemEntry> = when (json) {
        "in1" -> listOf(
            com.example.data.model.OperationItemEntry("vesh_01", "Бронежилет 6Б45", "компл.", 10),
            com.example.data.model.OperationItemEntry("med_01", "Аптечка АИ-4", "шт.", 30)
        )
        "is1" -> listOf(
            com.example.data.model.OperationItemEntry("vesh_01", "Бронежилет 6Б45", "компл.", 2),
            com.example.data.model.OperationItemEntry("med_01", "Аптечка АИ-4", "шт.", 5)
        )
        "is2" -> listOf(com.example.data.model.OperationItemEntry("med_01", "Аптечка АИ-4", "шт.", 3))
        "ex1" -> listOf(com.example.data.model.OperationItemEntry("med_01", "Аптечка АИ-4", "шт.", 1, reason = "Применена"))
        else -> emptyList()
    }

    private fun report(tab: Int, name: String) {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.components.ExcelReportPreviewDialog(
                    operations = reportOps,
                    stockRecords = stocks,
                    points = emptyList(),
                    catalogItems = items,
                    unitName = "МинБат",
                    initialFormIndex = tab,
                    parseItems = { reportItems(it) },
                    onDismiss = {}
                )
            }
        }
        compose.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("screenshots/$name.png")
    }

    @Test
    fun form8_dark() = report(1, "form8")

    @Test
    fun form18_dark() = report(2, "form18")

    @Test
    fun manual_dark() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.components.UserManualDialog(onDismiss = {})
            }
        }
        compose.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("screenshots/manual.png")
    }

    @Test
    fun dashboard_light() {
        dashboard(dark = false)
        compose.onRoot().captureRoboImage("screenshots/dashboard_light.png")
    }
}
