package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import com.example.data.license.FighterLicenseStatus
import com.example.data.local.InitialData
import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionRequest
import com.example.data.model.UserProfile
import com.example.data.model.WorkspaceProfileMode
import com.example.data.payment.YooKassaConfig
import com.example.data.sync.SyncState
import com.example.ui.components.PersonalLicenseDialog
import com.example.ui.screens.InventoryCatalogScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.MoreSettingsScreen
import com.example.ui.screens.RequestsScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w412dp-h915dp-xxhdpi")
class UiCrashAndSyncRegressionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleProfile = UserProfile(
        callsign = "Алексей",
        unitName = "Основной склад",
        unitKey = "kapt_1234567890abcdef1234",
        email = "user@example.com",
        isLoggedIn = true,
        isProActive = false,
        demoDaysLeft = 3
    )

    private val demoLicense = FighterLicenseStatus(
        fighterId = "FGT-TEST",
        isProActive = false,
        isDemoActive = true,
        demoDaysLeft = 3
    )

    @Test
    fun paymentDialogPressReleaseSpringNeverCrashesAndSelectsPlan() {
        var paidDays = -1
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                PersonalLicenseDialog(
                    profile = sampleProfile,
                    licenseStatus = demoLicense,
                    yooKassaConfig = YooKassaConfig(priceRubles = 490),
                    onPayYooKassaPlanClick = { paidDays = it },
                    onActivateLicenseKey = {},
                    onTestPaymentConfirm = {},
                    onDismiss = {},
                    profileMode = WorkspaceProfileMode.CIVILIAN
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()

        // Switch plan to 90 days and press/release the 3D pay button stepping through the full spring curve
        composeRule.onNodeWithText("90 дней").performClick()
        composeRule.mainClock.advanceTimeBy(200L)

        composeRule.onNodeWithTag("yookassa_pay_button").performTouchInput {
            down(center)
        }
        repeat(10) {
            composeRule.mainClock.advanceTimeBy(32L)
        }
        composeRule.onNodeWithTag("yookassa_pay_button").performTouchInput {
            up()
        }
        repeat(40) {
            composeRule.mainClock.advanceTimeBy(32L)
        }
        composeRule.waitForIdle()

        assertEquals(90, paidDays)
    }

    @Test
    fun moreScreenOpenProButtonPressReleaseSpringNeverCrashes() {
        var openedPro = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                MoreSettingsScreen(
                    profile = sampleProfile,
                    availableCategories = InitialData.getCivilianCategories(),
                    syncState = SyncState(isOnline = true, connectedDevicesCount = 2),
                    backupFileName = "backup.json",
                    lastAutoBackupAt = 0L,
                    onSaveBackup = {},
                    onRestoreBackup = {},
                    onDeleteCategory = {},
                    onAddCategory = {},
                    onResetCategories = {},
                    onSyncClick = {},
                    onOpenConnectCodeDialog = {},
                    onOpenPaymentPro = { openedPro = true },
                    onExportFullConsolidatedClick = {},
                    onExportPointSummaryClick = {},
                    onExportForm8Click = {},
                    onExportForm18Click = {},
                    onLogoutClick = {},
                    onUpdateProfile = {},
                    onResetDataClick = {},
                    onOpenManualClick = {},
                    onOpenDeveloperBackdoor = {},
                    workspaceProfileMode = WorkspaceProfileMode.CIVILIAN
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("subscribe_pro_button").performTouchInput {
            down(center)
        }
        repeat(8) { composeRule.mainClock.advanceTimeBy(32L) }
        composeRule.onNodeWithTag("subscribe_pro_button").performTouchInput {
            up()
        }
        repeat(35) { composeRule.mainClock.advanceTimeBy(32L) }
        composeRule.waitForIdle()

        assertTrue(openedPro)
    }

    @Test
    fun requestsScreenHeaderIsNotCrushedAndShowsTabsAndAddSheet() {
        val points = InitialData.getCivilianPoints()
        val items = InitialData.getCivilianItems()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                RequestsScreen(
                    profile = sampleProfile,
                    points = points,
                    catalogItems = items,
                    stockRecords = emptyList(),
                    requisitions = listOf(
                        RequisitionRequest(
                            id = "req-1",
                            pointName = points.first().name,
                            applicantName = "Бригада №2",
                            status = RequestStatus.PENDING,
                            comment = "",
                            timestamp = 1_700_000_000_000L,
                            itemsSummary = "Перфоратор - 1 шт.",
                            itemsJson = "[]"
                        )
                    ),
                    onCreateRequisition = { _, _, _, _ -> },
                    onUpdateStatus = { _, _ -> },
                    onDeleteRequisition = {},
                    parseItems = { emptyList() },
                    profileMode = WorkspaceProfileMode.CIVILIAN
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()

        val titleBounds = composeRule.onNodeWithTag("requests_header_title").getUnclippedBoundsInRoot()
        val buttonBounds = composeRule.onNodeWithTag("requests_add_button").getUnclippedBoundsInRoot()

        // Title must NEVER collapse to 0dp width or stretch the header row to full screen height
        val titleWidth = titleBounds.right - titleBounds.left
        val buttonWidth = buttonBounds.right - buttonBounds.left
        assertTrue("Expected requests header title width > 60dp, got $titleWidth", titleWidth > 60.dp)
        assertTrue("Expected inline +Заявка button width < 220dp, got $buttonWidth", buttonWidth < 220.dp)
        assertTrue("Expected header near top of screen (< 120dp), got top=${buttonBounds.top}", buttonBounds.top < 120.dp)
        assertTrue("Expected title left of button without overlap", titleBounds.right <= buttonBounds.left)

        // All 4 pipeline tabs + requisition card must be visible right away
        composeRule.onNodeWithText("Новые").assertIsDisplayed()
        composeRule.onNodeWithText("Собираются").assertIsDisplayed()
        composeRule.onNodeWithText("Бригада №2").assertIsDisplayed()

        // Pressing & releasing + Заявка must not crash and must open NewRequestSheet
        composeRule.onNodeWithTag("requests_add_button").performTouchInput { down(center) }
        repeat(8) { composeRule.mainClock.advanceTimeBy(32L) }
        composeRule.onNodeWithTag("requests_add_button").performTouchInput { up() }
        repeat(35) { composeRule.mainClock.advanceTimeBy(32L) }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Новая заявка").assertIsDisplayed()
    }

    @Test
    fun emptyRequestsScreenShowsHeaderTabsAndEmptyStateProperly() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                RequestsScreen(
                    profile = sampleProfile,
                    points = InitialData.getCivilianPoints(),
                    catalogItems = InitialData.getCivilianItems(),
                    stockRecords = emptyList(),
                    requisitions = emptyList(),
                    onCreateRequisition = { _, _, _, _ -> },
                    onUpdateStatus = { _, _ -> },
                    onDeleteRequisition = {},
                    parseItems = { emptyList() },
                    profileMode = WorkspaceProfileMode.CIVILIAN
                )
            }
        }
        composeRule.waitForIdle()

        val titleBounds = composeRule.onNodeWithTag("requests_header_title").getUnclippedBoundsInRoot()
        val buttonBounds = composeRule.onNodeWithTag("requests_add_button").getUnclippedBoundsInRoot()
        assertTrue((titleBounds.right - titleBounds.left) > 60.dp)
        assertTrue((buttonBounds.right - buttonBounds.left) < 220.dp)
        assertTrue(buttonBounds.top < 120.dp)
        composeRule.onNodeWithText("Заявки").assertIsDisplayed()
        composeRule.onNodeWithText("Новые").assertIsDisplayed()
        composeRule.onNodeWithText("Новых заявок нет").assertIsDisplayed()
    }

    @Test
    fun catalogHeaderInline3DButtonDoesNotCrushTitleAndNeverCrashes() {
        var clickedAdd = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                InventoryCatalogScreen(
                    items = InitialData.getCivilianItems(),
                    availableCategories = InitialData.getCivilianCategories(),
                    stockRecords = emptyList(),
                    profileMode = WorkspaceProfileMode.CIVILIAN,
                    onAddNewItemClick = { clickedAdd = true },
                    onUpdateItem = {},
                    onDeleteItem = { _, _ -> }
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()

        val titleBounds = composeRule.onNodeWithTag("catalog_header_title").getUnclippedBoundsInRoot()
        val buttonBounds = composeRule.onNodeWithTag("catalog_add_button").getUnclippedBoundsInRoot()
        assertTrue((titleBounds.right - titleBounds.left) > 80.dp)
        assertTrue((buttonBounds.right - buttonBounds.left) < 220.dp)
        assertTrue(titleBounds.right <= buttonBounds.left)

        composeRule.onNodeWithTag("catalog_add_button").performTouchInput { down(center) }
        repeat(8) { composeRule.mainClock.advanceTimeBy(32L) }
        composeRule.onNodeWithTag("catalog_add_button").performTouchInput { up() }
        repeat(35) { composeRule.mainClock.advanceTimeBy(32L) }
        composeRule.waitForIdle()

        assertTrue(clickedAdd)
    }

    @Test
    fun dashboardVolumetric3DActionTilesPressReleaseNeverCrashes() {
        var incomeClicks = 0
        var issueClicks = 0
        var returnClicks = 0
        var expClicks = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                MainDashboardScreen(
                    profile = sampleProfile,
                    syncState = SyncState(isOnline = true, connectedDevicesCount = 2),
                    points = InitialData.getCivilianPoints(),
                    catalogItems = InitialData.getCivilianItems(),
                    stockRecords = emptyList(),
                    operations = emptyList(),
                    availableCategories = InitialData.getCivilianCategories(),
                    selectedCategory = "Все виды",
                    searchQuery = "",
                    onSelectCategory = {},
                    onSearchChange = {},
                    onIncomeClick = { incomeClicks++ },
                    onTransferClick = {},
                    onIssueClick = { issueClicks++ },
                    onExpenditureClick = { expClicks++ },
                    onAddPointClick = {},
                    onEditPointClick = {},
                    onAddCustomItemClick = {},
                    onAdjustStock = { _, _, _, _, _, _, _ -> },
                    onSyncClick = {},
                    onSecondPhoneClick = {},
                    onExportClick = {},
                    onBannerClick = {},
                    onProfileClick = {},
                    onHelpClick = {},
                    isDarkTheme = true,
                    onToggleTheme = {},
                    profileMode = WorkspaceProfileMode.CIVILIAN,
                    onReturnClick = { returnClicks++ }
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()

        listOf("op_income_button", "op_issue_button", "op_return_button", "op_expenditure_button").forEach { tag ->
            composeRule.onNodeWithTag(tag).performTouchInput { down(center) }
            repeat(8) { composeRule.mainClock.advanceTimeBy(32L) }
            composeRule.onNodeWithTag(tag).performTouchInput { up() }
            repeat(35) { composeRule.mainClock.advanceTimeBy(32L) }
            composeRule.waitForIdle()
        }

        assertEquals(1, incomeClicks)
        assertEquals(1, issueClicks)
        assertEquals(1, returnClicks)
        assertEquals(1, expClicks)
    }

    @Test
    fun paymentDialogInlineEmailInputWorksWhenProfileEmailIsEmpty() {
        var capturedDays = -1
        var capturedEmail = ""
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                PersonalLicenseDialog(
                    profile = sampleProfile.copy(email = ""),
                    licenseStatus = demoLicense,
                    yooKassaConfig = YooKassaConfig(priceRubles = 490),
                    onPayYooKassaWithEmailClick = { days, email ->
                        capturedDays = days
                        capturedEmail = email
                    },
                    onActivateLicenseKey = {},
                    onTestPaymentConfirm = {},
                    onDismiss = {},
                    profileMode = WorkspaceProfileMode.CIVILIAN
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("checkout_email_input").assertIsDisplayed()
        composeRule.onNodeWithTag("yookassa_pay_button").assertIsDisplayed()
        composeRule.onNodeWithTag("checkout_email_input").performTextInput("buyer@kapterka.ru")
        composeRule.onNodeWithText("365 дней").performClick()
        composeRule.onNodeWithTag("yookassa_pay_button").performClick()
        composeRule.waitForIdle()

        assertEquals(365, capturedDays)
        assertEquals("buyer@kapterka.ru", capturedEmail)
    }

    @Test
    fun incomeDialogSearchableDropdownInsideVerticalScrollNeverCrashes() {
        val points = InitialData.getCivilianPoints()
        val items = InitialData.getCivilianItems()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                com.example.ui.components.IncomeOperationDialog(
                    profile = sampleProfile,
                    points = points,
                    catalogItems = items,
                    stockRecords = listOf(
                        com.example.data.model.StockRecord(points.first().id, items[0].id, 12, 12, 0),
                        com.example.data.model.StockRecord(points.first().id, items[6].id, 25, 25, 0)
                    ),
                    initialPointId = points.first().id,
                    profileMode = WorkspaceProfileMode.CIVILIAN,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Приход").assertIsDisplayed()
        // Expand point dropdown & item dropdown inside verticalScroll without any infinite-height crash
        composeRule.onNodeWithText(points.first().name).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Введите название").performClick()
        composeRule.waitForIdle()
    }
}
