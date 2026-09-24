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
    fun dashboard_light() {
        dashboard(dark = false)
        compose.onRoot().captureRoboImage("screenshots/dashboard_light.png")
    }
}
