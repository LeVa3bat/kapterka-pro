package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WarehousePoint
import com.example.ui.components.AddCustomItemDialog
import com.example.ui.components.AddPointDialog
import com.example.ui.components.EditPointDialog
import com.example.ui.components.ExcelReportPreviewDialog
import com.example.ui.components.ExpenditureOperationDialog
import com.example.ui.components.IncomeOperationDialog
import com.example.ui.components.IssueOperationDialog
import com.example.ui.components.PaymentProDialog
import com.example.ui.components.TransferOperationDialog
import com.example.ui.components.UnitKeySyncDialog
import com.example.ui.components.UserManualDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.TacticalNotificationHelper
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InventoryCatalogScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.MoreSettingsScreen
import com.example.ui.screens.RequestsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RuleFolder
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Tune
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.viewmodel.KapterkaViewModel
import kotlinx.coroutines.flow.collectLatest

enum class AppDestination(val title: String, val icon: ImageVector, val tag: String) {
    HOME("Главная", Icons.Default.SpaceDashboard, "nav_home"),
    HISTORY("Журнал", Icons.Default.ReceiptLong, "nav_history"),
    REQUESTS("Заявки", Icons.Default.RuleFolder, "nav_requests"),
    CATALOG("Каталог", Icons.Default.Inventory2, "nav_catalog"),
    MORE("Ещё", Icons.Default.Tune, "nav_more")
}

class MainActivity : ComponentActivity() {

    private val viewModel: KapterkaViewModel by viewModels()

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        TacticalNotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                KapterkaAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KapterkaAppRoot(viewModel: KapterkaViewModel) {
    var isSplashVisible by remember { mutableStateOf(true) }
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    val context = LocalContext.current

    // Request notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // State Collection
    val profile by viewModel.userProfile.collectAsState()
    val points by viewModel.allPoints.collectAsState()
    val selectedPointId by viewModel.selectedPointId.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val dashboardSearchQuery by viewModel.inventorySearchQuery.collectAsState()
    val stockRecords by viewModel.allStockRecords.collectAsState()
    val operations by viewModel.allOperations.collectAsState()
    val historyFilterType by viewModel.historyFilterType.collectAsState()
    val historySearchQuery by viewModel.historySearchQuery.collectAsState()
    val requisitions by viewModel.allRequisitions.collectAsState()
    val catalogItems by viewModel.allCatalogItems.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()

    // Dialog Control States
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showIssueDialog by remember { mutableStateOf(false) }
    var showExpenditureDialog by remember { mutableStateOf(false) }
    var showAddPointDialog by remember { mutableStateOf(false) }
    var editingPoint by remember { mutableStateOf<WarehousePoint?>(null) }
    var showAddCustomItemDialog by remember { mutableStateOf(false) }
    var showUnitKeySyncDialog by remember { mutableStateOf(false) }
    var showPaymentProDialog by remember { mutableStateOf(false) }
    var excelReportInitialTab by remember { mutableIntStateOf(0) }
    var showExcelReportDialog by remember { mutableStateOf(false) }
    var showUserManualDialog by remember { mutableStateOf(false) }

    // Toast Events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // SPLASH SCREEN DISPLAY
    if (isSplashVisible) {
        SplashScreen(
            onInitializationComplete = { isSplashVisible = false }
        )
        return
    }

    // AUTH SCREEN (if not logged in)
    if (profile?.isLoggedIn != true) {
        AuthScreen(
            currentProfile = profile,
            onSaveProfile = { newProfile -> viewModel.updateProfile(newProfile) },
            onContinue = {
                val p = profile ?: com.example.data.model.UserProfile(isLoggedIn = true)
                viewModel.updateProfile(p.copy(isLoggedIn = true))
            }
        )
        return
    }

    // MAIN SCAFFOLD WITH TACTICAL NAVIGATION BAR
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TacticalBg,
        bottomBar = {
            TacticalBottomNavigationBar(
                currentDestination = currentDestination,
                onNavigate = { currentDestination = it },
                pendingRequestsCount = requisitions.count { it.status == com.example.data.model.RequestStatus.PENDING }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppDestination.HOME -> {
                        MainDashboardScreen(
                            profile = profile,
                            points = points,
                            catalogItems = catalogItems,
                            stockRecords = stockRecords,
                            availableCategories = availableCategories,
                            selectedCategory = selectedCategory,
                            searchQuery = dashboardSearchQuery,
                            onSelectCategory = { viewModel.selectCategory(it) },
                            onSearchChange = { viewModel.setInventorySearchQuery(it) },
                            onIncomeClick = { showIncomeDialog = true },
                            onTransferClick = { showTransferDialog = true },
                            onIssueClick = { showIssueDialog = true },
                            onExpenditureClick = { showExpenditureDialog = true },
                            onAddPointClick = { showAddPointDialog = true },
                            onEditPointClick = { editingPoint = it },
                            onAddCustomItemClick = { showAddCustomItemDialog = true },
                            onAdjustStock = { pointId, pointName, itemId, itemName, newQty ->
                                viewModel.adjustPointStock(pointId, pointName, itemId, itemName, newQty)
                            },
                            onSyncClick = { viewModel.simulateCloudSync() },
                            onSecondPhoneClick = { showUnitKeySyncDialog = true },
                            onExportClick = {
                                excelReportInitialTab = 0
                                showExcelReportDialog = true
                            },
                            onBannerClick = { showPaymentProDialog = true },
                            onProfileClick = { currentDestination = AppDestination.MORE },
                            onHelpClick = { showUserManualDialog = true }
                        )
                    }

                    AppDestination.HISTORY -> {
                        HistoryScreen(
                            operations = operations,
                            filterType = historyFilterType,
                            searchQuery = historySearchQuery,
                            onFilterChange = { viewModel.setHistoryFilterType(it) },
                            onSearchChange = { viewModel.setHistorySearchQuery(it) },
                            parseItems = { viewModel.parseOperationItems(it) }
                        )
                    }

                    AppDestination.REQUESTS -> {
                        RequestsScreen(
                            profile = profile,
                            points = points,
                            catalogItems = catalogItems,
                            stockRecords = stockRecords,
                            requisitions = requisitions,
                            onCreateRequisition = { pName, applicant, items, comment ->
                                viewModel.createRequisition(pName, applicant, items, comment)
                            },
                            onUpdateStatus = { req, nextStatus ->
                                viewModel.updateRequisitionStatus(req, nextStatus)
                            },
                            onDeleteRequisition = { viewModel.deleteRequisition(it) },
                            parseItems = { viewModel.parseRequisitionItems(it) }
                        )
                    }

                    AppDestination.CATALOG -> {
                        InventoryCatalogScreen(
                            items = catalogItems,
                            availableCategories = availableCategories,
                            onAddNewItemClick = { showAddCustomItemDialog = true },
                            onUpdateItem = { viewModel.updateCatalogItem(it) },
                            onDeleteItem = { id, name -> viewModel.deleteCatalogItem(id, name) }
                        )
                    }

                    AppDestination.MORE -> {
                        MoreSettingsScreen(
                            profile = profile,
                            availableCategories = availableCategories,
                            onDeleteCategory = { viewModel.deleteCategory(it) },
                            onAddCategory = { viewModel.addCategory(it) },
                            onResetCategories = { viewModel.resetCategoriesToDefault() },
                            onSyncClick = { viewModel.simulateCloudSync() },
                            onOpenConnectCodeDialog = { showUnitKeySyncDialog = true },
                            onOpenPaymentPro = { showPaymentProDialog = true },
                            onExportFullConsolidatedClick = {
                                excelReportInitialTab = 0
                                showExcelReportDialog = true
                            },
                            onExportPointSummaryClick = {
                                excelReportInitialTab = 1.coerceAtMost(points.size)
                                showExcelReportDialog = true
                            },
                            onExportForm8Click = {
                                excelReportInitialTab = points.size + 1
                                showExcelReportDialog = true
                            },
                            onExportForm18Click = {
                                excelReportInitialTab = points.size + 2
                                showExcelReportDialog = true
                            },
                            onLogoutClick = {
                                val current = profile ?: com.example.data.model.UserProfile()
                                viewModel.updateProfile(current.copy(isLoggedIn = false))
                            },
                            onResetDataClick = { viewModel.clearAllData() },
                            onOpenManualClick = { showUserManualDialog = true }
                        )
                    }
                }
            }
        }
    }

    // MODAL DIALOGS
    if (showIncomeDialog) {
        IncomeOperationDialog(
            profile = profile,
            points = points,
            catalogItems = catalogItems,
            stockRecords = stockRecords,
            initialPointId = selectedPointId,
            onDismiss = { showIncomeDialog = false },
            onConfirm = { toPointId, toPointName, supplier, items, comment ->
                viewModel.recordIncome(toPointId, toPointName, supplier, items, comment)
            }
        )
    }

    if (showTransferDialog) {
        TransferOperationDialog(
            points = points,
            catalogItems = catalogItems,
            stockRecords = stockRecords,
            initialPointId = selectedPointId,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromPointId, fromPointName, toPointId, toPointName, items, comment ->
                viewModel.recordTransfer(fromPointId, fromPointName, toPointId, toPointName, items, comment)
            }
        )
    }

    if (showIssueDialog) {
        IssueOperationDialog(
            points = points,
            catalogItems = catalogItems,
            stockRecords = stockRecords,
            initialPointId = selectedPointId,
            onDismiss = { showIssueDialog = false },
            onConfirm = { fromPointId, fromPointName, toPointId, toPointName, items, comment ->
                viewModel.recordIssue(fromPointId, fromPointName, toPointId, toPointName, items, comment)
            }
        )
    }

    if (showExpenditureDialog) {
        ExpenditureOperationDialog(
            profile = profile,
            points = points,
            catalogItems = catalogItems,
            stockRecords = stockRecords,
            initialPointId = selectedPointId,
            onDismiss = { showExpenditureDialog = false },
            onConfirm = { fromPointId, pointName, docNumber, responsiblePerson, items, comment ->
                viewModel.recordExpenditure(fromPointId, pointName, docNumber, responsiblePerson, items, comment)
            }
        )
    }

    if (showAddPointDialog) {
        AddPointDialog(
            onDismiss = { showAddPointDialog = false },
            onConfirm = { name, desc ->
                viewModel.addWarehousePoint(name, desc)
            }
        )
    }

    editingPoint?.let { pt ->
        EditPointDialog(
            point = pt,
            onDismiss = { editingPoint = null },
            onSave = { updated ->
                viewModel.updateWarehousePoint(updated)
            },
            onDelete = { ptId ->
                viewModel.deleteWarehousePoint(ptId)
            }
        )
    }

    if (showAddCustomItemDialog) {
        AddCustomItemDialog(
            onDismiss = { showAddCustomItemDialog = false },
            onConfirm = { name, service, subType, unit ->
                viewModel.addCustomItem(name, service, subType, unit)
            }
        )
    }

    if (showUnitKeySyncDialog) {
        UnitKeySyncDialog(
            profile = profile,
            onRegenerateKey = { viewModel.regenerateUnitKey() },
            onForceSync = { viewModel.simulateCloudSync() },
            onDismiss = { showUnitKeySyncDialog = false }
        )
    }

    if (showPaymentProDialog) {
        PaymentProDialog(
            profile = profile,
            onActivatePro = { viewModel.activateProSubscription() },
            onDismiss = { showPaymentProDialog = false }
        )
    }

    if (showExcelReportDialog) {
        ExcelReportPreviewDialog(
            operations = operations,
            stockRecords = stockRecords,
            points = points,
            catalogItems = catalogItems,
            requisitions = requisitions,
            unitName = profile?.unitName ?: "1-е Подразделение",
            initialFormIndex = excelReportInitialTab,
            parseItems = { viewModel.parseOperationItems(it) },
            onDismiss = { showExcelReportDialog = false }
        )
    }

    if (showUserManualDialog) {
        UserManualDialog(
            onDismiss = { showUserManualDialog = false }
        )
    }
}

@Composable
private fun TacticalBottomNavigationBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    pendingRequestsCount: Int
) {
    NavigationBar(
        containerColor = TacticalSurface,
        contentColor = SageGreenPrimary,
        tonalElevation = 2.dp,
        modifier = Modifier
            .border(androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle))
            .height(64.dp)
    ) {
        AppDestination.values().forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(destination) },
                icon = {
                    if (destination == AppDestination.REQUESTS && pendingRequestsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = TacticalGold,
                                    contentColor = Color.White
                                ) {
                                    Text("$pendingRequestsCount", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SageGreenBright,
                    selectedTextColor = SageGreenBright,
                    unselectedIconColor = TacticalTextMuted,
                    unselectedTextColor = TacticalTextMuted,
                    indicatorColor = SageGreenDark
                ),
                modifier = Modifier.testTag(destination.tag)
            )
        }
    }
}
