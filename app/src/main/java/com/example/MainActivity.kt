package com.example

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WarehousePoint
import com.example.ui.components.AddCustomItemDialog
import com.example.ui.components.AddPointDialog
import com.example.ui.components.DeveloperAccessPromptDialog
import com.example.ui.components.DeveloperAdminDialog
import com.example.ui.components.DeveloperDiagnosticsSnapshot
import com.example.ui.components.EditPointDialog
import com.example.ui.components.ExcelReportPreviewDialog
import com.example.ui.components.ExpenditureOperationDialog
import com.example.ui.components.IncomeOperationDialog
import com.example.ui.components.IssueOperationDialog
import com.example.ui.components.PersonalLicenseDialog
import com.example.ui.components.TransferOperationDialog
import com.example.ui.components.UnitKeySyncDialog
import com.example.ui.components.UserManualDialog
import com.example.ui.components.UniversalAddItemDialog
import com.example.ui.components.UniversalAddPointDialog
import com.example.ui.components.UniversalEditPointDialog
import com.example.ui.components.UniversalIncomeOperationDialog
import com.example.ui.components.UniversalTransferOperationDialog
import com.example.ui.components.UniversalIssueOperationDialog
import com.example.ui.components.UniversalExpenditureOperationDialog
import com.example.ui.components.UniversalProDialog
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
import com.example.ui.screens.WarehouseProfileSetupScreen
import com.example.ui.screens.UniversalMoreScreen
import com.example.ui.screens.UniversalWorkspaceSetupScreen
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
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.components.UniversalBottomNavigationBar
import com.example.ui.screens.UniversalAuthScreen
import com.example.ui.screens.UniversalDashboardScreen
import com.example.ui.screens.UniversalOperationsScreen
import com.example.ui.screens.UniversalCatalogScreen
import com.example.ui.screens.UniversalRequestsScreen
import com.example.ui.screens.UniversalSplashScreen
import com.example.universal.UniversalAuthResult
import com.example.universal.UniversalFirebaseAuth
import com.example.universal.UniversalBackendClient
import com.example.universal.UniversalBackendResult
import com.example.universal.UniversalEntitlement
import com.example.ui.viewmodel.KapterkaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow

enum class AppDestination(val title: String, val icon: ImageVector, val tag: String) {
    HOME("Сегодня", Icons.Default.SpaceDashboard, "nav_home"),
    HISTORY("Операции", Icons.Default.ReceiptLong, "nav_history"),
    REQUESTS("Заявки", Icons.Default.RuleFolder, "nav_requests"),
    CATALOG("Имущество", Icons.Default.Inventory2, "nav_catalog"),
    MORE("Профиль", Icons.Default.Tune, "nav_more")
}

class MainActivity : ComponentActivity() {

    private val viewModel: KapterkaViewModel by viewModels()
    private val universalPaymentReturnSignal = MutableStateFlow(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Catch any unhandled thread errors for emulator diagnostic logging
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("KapterkaApp", "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            prevHandler?.uncaughtException(thread, throwable)
        }

        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "enableEdgeToEdge skipped: ${e.message}")
        }

        if (!BuildConfig.IS_UNIVERSAL_APP) {
            try {
                TacticalNotificationHelper.createNotificationChannel(this)
            } catch (e: Throwable) {
                android.util.Log.w("MainActivity", "Notification channel skipped: ${e.message}")
            }
        }

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val paymentReturnSignal by universalPaymentReturnSignal.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                KapterkaAppRoot(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    universalPaymentReturnSignal = paymentReturnSignal
                )
            }
        }

        handlePaymentReturn(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentReturn(intent)
    }

    private fun handlePaymentReturn(sourceIntent: Intent?) {
        val data = sourceIntent?.data ?: return
        if (data.scheme == BuildConfig.PAYMENT_CALLBACK_SCHEME && data.host == "payment_success") {
            if (BuildConfig.IS_UNIVERSAL_APP) {
                universalPaymentReturnSignal.value = universalPaymentReturnSignal.value + 1L
            } else {
                viewModel.confirmPaymentAndActivateLicense()
            }
        }
    }
}

@Composable
fun KapterkaAppRoot(
    viewModel: KapterkaViewModel,
    isDarkTheme: Boolean = false,
    universalPaymentReturnSignal: Long = 0L
) {
    var isSplashVisible by remember { mutableStateOf(true) }
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    val context = LocalContext.current
    val setupPrefs = remember(context) {
        context.getSharedPreferences("sklad_pro_setup", android.content.Context.MODE_PRIVATE)
    }
    var warehouseProfileId by remember {
        mutableStateOf(setupPrefs.getString("warehouse_profile_id_v2", null))
    }
    val universalAuth = remember(context) { UniversalFirebaseAuth(context) }
    val universalBackend = remember(context) { UniversalBackendClient(context) }
    var universalEntitlement by remember {
        mutableStateOf<UniversalEntitlement?>(universalBackend.cachedEntitlement())
    }
    var universalSubscriptionLoading by remember { mutableStateOf(false) }
    var universalSubscriptionMessage by remember { mutableStateOf<String?>(null) }
    var universalAuthenticated by remember {
        mutableStateOf(universalAuth.currentVerifiedAccount() != null)
    }
    var pendingVerificationEmail by remember {
        mutableStateOf(universalAuth.currentUnverifiedEmail())
    }
    var universalWorkspaceReady by remember {
        mutableStateOf(setupPrefs.getBoolean("universal_workspace_ready_v2", false))
    }

    // Request notification permission for Android 13+
    if (!BuildConfig.IS_UNIVERSAL_APP && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            try {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } catch (e: Throwable) {
                android.util.Log.w("MainActivity", "Notification permission request skipped on emulator: ${e.message}")
            }
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
    val syncState by viewModel.syncState.collectAsState()
    val licenseStatus by viewModel.licenseStatus.collectAsState()
    val allFighters by viewModel.allFighters.collectAsState()
    val issuedPaymentKey by viewModel.issuedPaymentKey.collectAsState()

    val activeProfileId = warehouseProfileId ?: "universal"
    val profileCatalogItems = remember(catalogItems, activeProfileId) {
        if (!BuildConfig.IS_UNIVERSAL_APP) {
            catalogItems
        } else {
            catalogItems.filter { it.profileId == activeProfileId }
        }
    }
    val profileItemIds = remember(profileCatalogItems) {
        profileCatalogItems.map { it.id }.toSet()
    }
    val profileItemNames = remember(profileCatalogItems) {
        profileCatalogItems.map { it.name }.toSet()
    }
    val profileStockRecords = remember(stockRecords, profileItemIds) {
        if (!BuildConfig.IS_UNIVERSAL_APP) {
            stockRecords
        } else {
            stockRecords.filter { it.itemId in profileItemIds }
        }
    }
    val profileOperations = remember(operations, profileItemIds, profileItemNames) {
        if (!BuildConfig.IS_UNIVERSAL_APP) {
            operations
        } else {
            operations.filter { operation ->
                val entries = viewModel.parseOperationItems(operation.itemsJson)
                entries.any { it.itemId in profileItemIds } ||
                    (entries.isEmpty() && profileItemNames.any { name ->
                        operation.itemsSummary.contains(name, ignoreCase = true)
                    })
            }
        }
    }
    val profileRequisitions = remember(requisitions, profileItemNames) {
        if (!BuildConfig.IS_UNIVERSAL_APP) {
            requisitions
        } else {
            requisitions.filter { request ->
                val entries = viewModel.parseRequisitionItems(request.itemsJson)
                entries.any { it.itemName in profileItemNames } ||
                    (entries.isEmpty() && profileItemNames.any { name ->
                        request.itemsSummary.contains(name, ignoreCase = true)
                    })
            }
        }
    }

    LaunchedEffect(points) {
        if (BuildConfig.IS_UNIVERSAL_APP && points.isNotEmpty() && points.none { it.id == selectedPointId }) {
            viewModel.selectPoint(points.first().id)
        }
    }

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
    var showDevAuthPrompt by remember { mutableStateOf(false) }
    var showDevAdminDialog by remember { mutableStateOf(false) }
    var excelReportInitialTab by remember { mutableIntStateOf(0) }
    var showExcelReportDialog by remember { mutableStateOf(false) }
    var showUserManualDialog by remember { mutableStateOf(false) }

    // Toast Events from ViewModel with rich in-app tactical popup banner
    var inAppToastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            inAppToastMessage = msg
            delay(5000)
            if (inAppToastMessage == msg) {
                inAppToastMessage = null
            }
        }
    }

    // SPLASH / ONBOARDING
    if (isSplashVisible) {
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalSplashScreen(
                onInitializationComplete = { isSplashVisible = false }
            )
        } else {
            SplashScreen(
                onInitializationComplete = { isSplashVisible = false }
            )
        }
        return
    }

    if (BuildConfig.IS_UNIVERSAL_APP) {
        val finishUniversalAuth: (UniversalAuthResult.Authenticated) -> Unit = { account ->
            val current = profile ?: com.example.data.model.UserProfile()
            viewModel.saveUniversalProfile(
                current.copy(
                    callsign = account.displayName.ifBlank { current.callsign },
                    email = account.email,
                    unitKey = "",
                    isLoggedIn = true,
                    isOnline = false
                )
            )
            setupPrefs.edit().putBoolean("universal_authenticated_v2", true).apply()
            pendingVerificationEmail = null
            universalAuthenticated = true
        }

        if (!universalAuthenticated) {
            UniversalAuthScreen(
                savedEmail = universalAuth.savedEmail(),
                pendingVerificationEmail = pendingVerificationEmail,
                onRegister = { name, email, password, complete ->
                    universalAuth.register(name, email, password) { result ->
                        when (result) {
                            is UniversalAuthResult.Authenticated -> {
                                finishUniversalAuth(result)
                                complete(null)
                            }
                            is UniversalAuthResult.VerificationRequired -> {
                                pendingVerificationEmail = result.email
                                complete(null)
                            }
                            is UniversalAuthResult.Error -> complete(result.message)
                        }
                    }
                },
                onLogin = { email, password, complete ->
                    universalAuth.login(email, password) { result ->
                        when (result) {
                            is UniversalAuthResult.Authenticated -> {
                                finishUniversalAuth(result)
                                complete(null)
                            }
                            is UniversalAuthResult.VerificationRequired -> {
                                pendingVerificationEmail = result.email
                                complete(null)
                            }
                            is UniversalAuthResult.Error -> complete(result.message)
                        }
                    }
                },
                onCheckVerification = { complete ->
                    universalAuth.refreshVerification { result ->
                        when (result) {
                            is UniversalAuthResult.Authenticated -> {
                                finishUniversalAuth(result)
                                complete(null)
                            }
                            is UniversalAuthResult.VerificationRequired -> {
                                pendingVerificationEmail = result.email
                                complete("Почта ещё не подтверждена. Откройте ссылку из письма.")
                            }
                            is UniversalAuthResult.Error -> complete(result.message)
                        }
                    }
                },
                onResendVerification = { complete ->
                    universalAuth.resendVerification(complete)
                },
                onCancelVerification = {
                    universalAuth.signOut()
                    pendingVerificationEmail = null
                    setupPrefs.edit().putBoolean("universal_authenticated_v2", false).apply()
                },
                onResetPassword = { email, complete ->
                    universalAuth.sendPasswordReset(email, complete)
                }
            )
            return
        }

        if (warehouseProfileId.isNullOrBlank()) {
            WarehouseProfileSetupScreen(
                onProfileSelected = { profileId ->
                    setupPrefs.edit().putString("warehouse_profile_id_v2", profileId).apply()
                    viewModel.applyWarehouseProfile(profileId)
                    warehouseProfileId = profileId
                }
            )
            return
        }

        if (!universalWorkspaceReady) {
            UniversalWorkspaceSetupScreen(
                currentProfile = profile,
                warehouseProfileId = warehouseProfileId,
                onComplete = { newProfile ->
                    viewModel.saveUniversalProfile(newProfile.copy(unitKey = ""))
                    setupPrefs.edit().putBoolean("universal_workspace_ready_v2", true).apply()
                    universalWorkspaceReady = true
                }
            )
            return
        }

        LaunchedEffect(warehouseProfileId, universalWorkspaceReady) {
            val profileId = warehouseProfileId
            if (universalWorkspaceReady && !profileId.isNullOrBlank()) {
                viewModel.applyWarehouseProfile(profileId, announce = false)
                viewModel.ensureWarehouseProfileStarterCatalog(profileId)
            }
        }

        LaunchedEffect(universalEntitlement?.isProActive) {
            viewModel.setUniversalCloudSyncEnabled(
                universalEntitlement?.isProActive == true
            )
        }

        LaunchedEffect(universalAuthenticated, universalWorkspaceReady) {
            if (
                universalAuthenticated &&
                universalWorkspaceReady &&
                universalBackend.isConfigured
            ) {
                universalSubscriptionLoading = true
                universalBackend.bootstrap { result ->
                    universalSubscriptionLoading = false
                    when (result) {
                        is UniversalBackendResult.Success -> {
                            universalEntitlement = result.value
                            universalSubscriptionMessage = null

                            if (universalBackend.pendingPaymentId().isNotBlank()) {
                                universalBackend.checkPendingPayment { pendingResult ->
                                    when (pendingResult) {
                                        is UniversalBackendResult.Success -> {
                                            universalEntitlement = pendingResult.value
                                            if (pendingResult.value.isProActive) {
                                                universalSubscriptionMessage =
                                                    "Оплата подтверждена. PRO активирован."
                                            }
                                        }
                                        is UniversalBackendResult.Error -> {
                                            universalSubscriptionMessage = pendingResult.message
                                        }
                                    }
                                }
                            }
                        }
                        is UniversalBackendResult.Error -> {
                            universalSubscriptionMessage = result.message
                        }
                    }
                }
            }
        }

        LaunchedEffect(universalPaymentReturnSignal) {
            if (
                universalPaymentReturnSignal > 0L &&
                universalAuthenticated &&
                universalBackend.isConfigured
            ) {
                universalSubscriptionLoading = true
                universalBackend.checkPendingPayment { result ->
                    universalSubscriptionLoading = false
                    when (result) {
                        is UniversalBackendResult.Success -> {
                            universalEntitlement = result.value
                            universalSubscriptionMessage =
                                if (result.value.isProActive) "Оплата подтверждена. PRO активирован." else null
                        }
                        is UniversalBackendResult.Error -> {
                            universalSubscriptionMessage = result.message
                        }
                    }
                }
            }
        }
    } else if (profile?.isLoggedIn != true) {
        AuthScreen(
            currentProfile = profile,
            onCompleteAuth = { newProfile ->
                viewModel.registerOrLoginProfile(newProfile)
            }
        )
        return
    }

    // MAIN SCAFFOLD WITH TACTICAL NAVIGATION BAR
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (BuildConfig.IS_UNIVERSAL_APP) Color(0xFFF5F7FB) else TacticalBg,
        bottomBar = {
            if (BuildConfig.IS_UNIVERSAL_APP) {
                UniversalBottomNavigationBar(
                    currentDestination = currentDestination,
                    pendingRequestsCount = requisitions.count { it.status == com.example.data.model.RequestStatus.PENDING },
                    onNavigate = { destination ->
                        val canUseAdvancedRequests =
                            !universalBackend.isConfigured ||
                            universalEntitlement?.features?.advancedRequisitions == true
                        if (destination == AppDestination.REQUESTS && !canUseAdvancedRequests) {
                            universalSubscriptionMessage =
                                "Расширенные заявки доступны в PRO."
                            showPaymentProDialog = true
                        } else {
                            currentDestination = destination
                        }
                    }
                )
            } else {
                TacticalBottomNavigationBar(
                    currentDestination = currentDestination,
                    onNavigate = { currentDestination = it },
                    pendingRequestsCount = requisitions.count { it.status == com.example.data.model.RequestStatus.PENDING }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val context = LocalContext.current

            // In-app status banner. The universal app uses its own light visual language.
            val universalNotice = BuildConfig.IS_UNIVERSAL_APP
            AnimatedVisibility(
                visible = inAppToastMessage != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(300f)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                inAppToastMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inAppToastMessage = null },
                        colors = CardDefaults.cardColors(
                            containerColor = if (universalNotice) Color.White else TacticalSurfaceLight
                        ),
                        border = BorderStroke(
                            if (universalNotice) 1.dp else 1.5.dp,
                            if (universalNotice) Color(0xFFE2E6EE) else SageGreenPrimary
                        ),
                        shape = RoundedCornerShape(if (universalNotice) 20.dp else 10.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (universalNotice) Color(0xFFEEEEFF) else SageGreenDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (universalNotice) Color(0xFF5B5CE2) else SageGreenBright,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (universalNotice) "Готово" else "ОПЕРАЦИЯ ЗАФИКСИРОВАНА",
                                    color = if (universalNotice) Color(0xFF111827) else SageGreenBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg,
                                    color = if (universalNotice) Color(0xFF667085) else TacticalTextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(
                                onClick = { inAppToastMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = if (universalNotice) Color(0xFF98A2B3) else TacticalTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            val isProOrDemoActive =
                (profile?.isProActive == true) ||
                licenseStatus.isProActive ||
                ((profile?.demoDaysLeft ?: 0) > 0) ||
                licenseStatus.isDemoActive

            val checkProAccess: (String, () -> Unit) -> Unit = { actionName, onGranted ->
                if (isProOrDemoActive) {
                    onGranted()
                } else {
                    Toast.makeText(
                        context,
                        "Демо-период (3 дня) истёк. Для $actionName требуется лицензия PRO.",
                        Toast.LENGTH_LONG
                    ).show()
                    showPaymentProDialog = true
                }
            }

            val universalAccess: (Boolean, String, () -> Unit) -> Unit =
                { allowed, actionName, onGranted ->
                    if (!universalBackend.isConfigured || allowed) {
                        onGranted()
                    } else {
                        universalSubscriptionMessage =
                            if (universalEntitlement?.isExpired == true)
                                "Демо-период завершён. Данные сохранены, но для $actionName нужен PRO."
                            else
                                "Функция «$actionName» доступна в PRO."
                        showPaymentProDialog = true
                    }
                }

            val canUniversalOperate =
                universalEntitlement?.features?.localOperations == true
            val canUniversalCatalog =
                universalEntitlement?.features?.localCatalog == true
            val canUniversalWarehouses =
                universalEntitlement?.features?.localWarehouses == true
            val canUniversalRequests =
                universalEntitlement?.features?.advancedRequisitions == true

            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppDestination.HOME -> {
                        if (BuildConfig.IS_UNIVERSAL_APP) {
                            UniversalDashboardScreen(
                                userProfile = profile,
                                warehouseProfileId = warehouseProfileId,
                                points = points,
                                selectedPointId = selectedPointId,
                                catalogItems = profileCatalogItems,
                                stockRecords = profileStockRecords,
                                operations = profileOperations,
                                requisitions = profileRequisitions,
                                onSelectPoint = { viewModel.selectPoint(it) },
                                onIncomeClick = {
                                    universalAccess(canUniversalOperate, "операций склада") {
                                        showIncomeDialog = true
                                    }
                                },
                                onTransferClick = {
                                    universalAccess(canUniversalOperate, "перемещения") {
                                        showTransferDialog = true
                                    }
                                },
                                onIssueClick = {
                                    universalAccess(canUniversalOperate, "выдачи") {
                                        showIssueDialog = true
                                    }
                                },
                                onWriteOffClick = {
                                    universalAccess(canUniversalOperate, "списания") {
                                        showExpenditureDialog = true
                                    }
                                },
                                onAddItemClick = {
                                    universalAccess(canUniversalCatalog, "изменения каталога") {
                                        showAddCustomItemDialog = true
                                    }
                                },
                                onOpenCatalog = { currentDestination = AppDestination.CATALOG },
                                onOpenOperations = { currentDestination = AppDestination.HISTORY },
                                onOpenProfile = { currentDestination = AppDestination.MORE }
                            )
                        } else {
                            MainDashboardScreen(
                                profile = profile,
                                points = points,
                                catalogItems = catalogItems,
                                stockRecords = stockRecords,
                                operations = operations,
                                availableCategories = availableCategories,
                                selectedCategory = selectedCategory,
                                searchQuery = dashboardSearchQuery,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onSearchChange = { viewModel.setInventorySearchQuery(it) },
                                onIncomeClick = { showIncomeDialog = true },
                                onTransferClick = { showTransferDialog = true },
                                onIssueClick = { showIssueDialog = true },
                                onExpenditureClick = {
                                    checkProAccess("списания имущества") {
                                        showExpenditureDialog = true
                                    }
                                },
                                onAddPointClick = { showAddPointDialog = true },
                                onEditPointClick = { editingPoint = it },
                                onAddCustomItemClick = { showAddCustomItemDialog = true },
                                onAdjustStock = { pointId, pointName, itemId, itemName, newQty ->
                                    viewModel.adjustPointStock(pointId, pointName, itemId, itemName, newQty)
                                },
                                onSyncClick = { viewModel.simulateCloudSync() },
                                onSecondPhoneClick = { showUnitKeySyncDialog = true },
                                onExportClick = {
                                    checkProAccess("выгрузки отчетов в Excel") {
                                        excelReportInitialTab = 0
                                        showExcelReportDialog = true
                                    }
                                },
                                onBannerClick = { showPaymentProDialog = true },
                                onProfileClick = { currentDestination = AppDestination.MORE },
                                onHelpClick = { showUserManualDialog = true },
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { viewModel.toggleTheme() },
                                onReorderPoints = { viewModel.reorderWarehousePoints(it) }
                            )
                        }
                    }

                    AppDestination.HISTORY -> {
                        if (BuildConfig.IS_UNIVERSAL_APP) {
                            UniversalOperationsScreen(
                                warehouseProfileId = warehouseProfileId,
                                points = points,
                                selectedPointId = selectedPointId,
                                onSelectPoint = { viewModel.selectPoint(it) },
                                operations = profileOperations
                            )
                        } else {
                            HistoryScreen(
                                operations = operations,
                                filterType = historyFilterType,
                                searchQuery = historySearchQuery,
                                catalogItems = catalogItems,
                                availableCategories = availableCategories,
                                onFilterChange = { viewModel.setHistoryFilterType(it) },
                                onSearchChange = { viewModel.setHistorySearchQuery(it) },
                                parseItems = { viewModel.parseOperationItems(it) }
                            )
                        }
                    }

                    AppDestination.REQUESTS -> {
                        if (BuildConfig.IS_UNIVERSAL_APP) {
                            UniversalRequestsScreen(
                                profile = profile,
                                points = points,
                                catalogItems = profileCatalogItems,
                                stockRecords = profileStockRecords,
                                requisitions = profileRequisitions,
                                onCreateRequisition = { pName, applicant, items, comment ->
                                    universalAccess(canUniversalRequests, "расширенных заявок") {
                                        viewModel.createRequisition(pName, applicant, items, comment)
                                    }
                                },
                                onUpdateStatus = { req, nextStatus ->
                                    universalAccess(canUniversalRequests, "расширенных заявок") {
                                        viewModel.updateRequisitionStatus(req, nextStatus)
                                    }
                                },
                                onDeleteRequisition = { request ->
                                    universalAccess(canUniversalRequests, "расширенных заявок") {
                                        viewModel.deleteRequisition(request)
                                    }
                                },
                                parseItems = { viewModel.parseRequisitionItems(it) }
                            )
                        } else {
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
                    }

                    AppDestination.CATALOG -> {
                        if (BuildConfig.IS_UNIVERSAL_APP) {
                            UniversalCatalogScreen(
                                warehouseProfileId = warehouseProfileId,
                                points = points,
                                selectedPointId = selectedPointId,
                                items = profileCatalogItems,
                                stockRecords = profileStockRecords,
                                availableCategories = availableCategories,
                                onSelectPoint = { viewModel.selectPoint(it) },
                                onAddItem = {
                                    universalAccess(canUniversalCatalog, "изменения каталога") {
                                        showAddCustomItemDialog = true
                                    }
                                },
                                onUpdateItem = { item ->
                                    universalAccess(canUniversalCatalog, "изменения каталога") {
                                        viewModel.updateCatalogItem(item)
                                    }
                                },
                                onDeleteItem = { id, name ->
                                    universalAccess(canUniversalCatalog, "изменения каталога") {
                                        viewModel.deleteCatalogItem(id, name)
                                    }
                                }
                            )
                        } else {
                            InventoryCatalogScreen(
                                items = catalogItems,
                                availableCategories = availableCategories,
                                onAddNewItemClick = { showAddCustomItemDialog = true },
                                onUpdateItem = { viewModel.updateCatalogItem(it) },
                                onDeleteItem = { id, name -> viewModel.deleteCatalogItem(id, name) }
                            )
                        }
                    }

                    AppDestination.MORE -> {
                        if (BuildConfig.IS_UNIVERSAL_APP) {
                            val subscriptionTitle = when {
                                universalEntitlement?.isProActive == true ->
                                    "PRO • ${universalEntitlement?.daysRemaining() ?: 0} дн."
                                universalEntitlement?.isTrialActive == true ->
                                    "Демо • ${universalEntitlement?.daysRemaining() ?: 0} дн."
                                universalEntitlement?.isExpired == true ->
                                    "Демо завершено"
                                !universalBackend.isConfigured ->
                                    "PRO • 500 ₽ / 30 дней"
                                else ->
                                    "Проверка подписки"
                            }
                            val subscriptionSubtitle = when {
                                universalEntitlement?.isProActive == true ->
                                    "PRO активен • платёж подтверждён"
                                universalEntitlement?.isTrialActive == true ->
                                    "Базовый локальный учёт; PRO снимает ограничения подписки. Синхронизация и отчёты подключаются поэтапно в Alpha"
                                universalEntitlement?.isExpired == true ->
                                    "Данные сохранены; изменение данных доступно после оплаты"
                                !universalBackend.isConfigured ->
                                    "Демо 3 дня; сервер оплаты готовится к финальной привязке"
                                else ->
                                    "Нажмите, чтобы проверить статус"
                            }

                            val syncTitle = when {
                                universalEntitlement?.isProActive == true && syncState.isSyncing ->
                                    "Синхронизация..."
                                universalEntitlement?.isProActive == true && syncState.isOnline ->
                                    "Облако подключено"
                                universalEntitlement?.isProActive == true ->
                                    "Облачная синхронизация"
                                else ->
                                    "Облачная синхронизация • PRO"
                            }
                            val syncSubtitle = if (universalEntitlement?.isProActive == true) {
                                syncState.syncMessage +
                                    " • ключ склада не нужен: войдите на втором устройстве в тот же аккаунт"
                            } else {
                                "Доступна после активации PRO. Синхронизация привязана к аккаунту, локальные данные не удаляются."
                            }

                            UniversalMoreScreen(
                                userProfile = profile,
                                warehouseProfileId = warehouseProfileId,
                                points = points,
                                subscriptionTitle = subscriptionTitle,
                                subscriptionSubtitle = subscriptionSubtitle,
                                syncTitle = syncTitle,
                                syncSubtitle = syncSubtitle,
                                onSubscriptionClick = {
                                    universalSubscriptionMessage = null
                                    showPaymentProDialog = true
                                },
                                onSyncClick = {
                                    if (universalEntitlement?.isProActive == true) {
                                        viewModel.syncUniversalNow()
                                    } else {
                                        universalSubscriptionMessage =
                                            "Облачная синхронизация доступна в PRO."
                                        showPaymentProDialog = true
                                    }
                                },
                                onAddWarehouse = {
                                    universalAccess(canUniversalWarehouses, "управления складами") {
                                        showAddPointDialog = true
                                    }
                                },
                                onEditWarehouse = { point ->
                                    universalAccess(canUniversalWarehouses, "управления складами") {
                                        editingPoint = point
                                    }
                                },
                                onChangeProfile = {
                                    universalAccess(canUniversalWarehouses, "смены профиля склада") {
                                        setupPrefs.edit().remove("warehouse_profile_id_v2").apply()
                                        warehouseProfileId = null
                                        currentDestination = AppDestination.HOME
                                    }
                                },
                                onLogout = {
                                    viewModel.setUniversalCloudSyncEnabled(false)
                                    universalAuth.signOut()
                                    viewModel.saveUniversalProfile(
                                        (profile ?: com.example.data.model.UserProfile()).copy(
                                            unitKey = "",
                                            isLoggedIn = false,
                                            isOnline = false
                                        )
                                    )
                                    setupPrefs.edit()
                                        .putBoolean("universal_authenticated_v2", false)
                                        .apply()
                                    pendingVerificationEmail = null
                                    universalAuthenticated = false
                                    currentDestination = AppDestination.HOME
                                }
                            )
                        } else {
                            MoreSettingsScreen(
                                profile = profile,
                                availableCategories = availableCategories,
                                syncState = syncState,
                                onDeleteCategory = { viewModel.deleteCategory(it) },
                                onAddCategory = { viewModel.addCategory(it) },
                                onResetCategories = { viewModel.resetCategoriesToDefault() },
                                onSyncClick = { viewModel.simulateCloudSync() },
                                onOpenConnectCodeDialog = { showUnitKeySyncDialog = true },
                                onOpenPaymentPro = { showPaymentProDialog = true },
                                onExportFullConsolidatedClick = {
                                    checkProAccess("выгрузки сводной ведомости") {
                                        excelReportInitialTab = 0
                                        showExcelReportDialog = true
                                    }
                                },
                                onExportPointSummaryClick = {
                                    checkProAccess("выгрузки ведомости остатков") {
                                        excelReportInitialTab = 1.coerceAtMost(points.size)
                                        showExcelReportDialog = true
                                    }
                                },
                                onExportForm8Click = {
                                    checkProAccess("выгрузки Формы № 8") {
                                        excelReportInitialTab = points.size + 1
                                        showExcelReportDialog = true
                                    }
                                },
                                onExportForm18Click = {
                                    checkProAccess("выгрузки Формы № 18") {
                                        excelReportInitialTab = points.size + 2
                                        showExcelReportDialog = true
                                    }
                                },
                                onLogoutClick = {
                                    val current = profile ?: com.example.data.model.UserProfile()
                                    viewModel.updateProfile(current.copy(isLoggedIn = false))
                                },
                                onUpdateProfile = { updated -> viewModel.updateProfile(updated) },
                                onRestoreLicenseFromCloud = { viewModel.restoreLicenseFromCloud() },
                                onResetDataClick = { viewModel.clearAllData() },
                                onOpenManualClick = { showUserManualDialog = true },
                                onOpenDeveloperBackdoor = { showDevAuthPrompt = true },
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { viewModel.toggleTheme() }
                            )
                        }
                    }
                }
            }
        }
    }

    // MODAL DIALOGS
    if (showIncomeDialog) {
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalIncomeOperationDialog(
                profile = profile,
                points = points,
                catalogItems = profileCatalogItems,
                stockRecords = profileStockRecords,
                initialPointId = selectedPointId,
                warehouseProfileId = warehouseProfileId,
                onDismiss = { showIncomeDialog = false },
                onConfirm = { toPointId, toPointName, supplier, items, comment ->
                    viewModel.recordIncome(toPointId, toPointName, supplier, items, comment)
                }
            )
        } else {
            IncomeOperationDialog(
                profile = profile,
                points = points,
                catalogItems = profileCatalogItems,
                stockRecords = profileStockRecords,
                initialPointId = selectedPointId,
                warehouseProfileId = warehouseProfileId,
                onDismiss = { showIncomeDialog = false },
                onConfirm = { toPointId, toPointName, supplier, items, comment ->
                    viewModel.recordIncome(toPointId, toPointName, supplier, items, comment)
                }
            )
        }
    }

    if (showTransferDialog) {
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalTransferOperationDialog(
                points = points,
                catalogItems = profileCatalogItems,
                stockRecords = profileStockRecords,
                initialPointId = selectedPointId,
                warehouseProfileId = warehouseProfileId,
                onDismiss = { showTransferDialog = false },
                onConfirm = { fromPointId, fromPointName, toPointId, toPointName, items, comment ->
                    viewModel.recordTransfer(fromPointId, fromPointName, toPointId, toPointName, items, comment)
                }
            )
        } else {
            TransferOperationDialog(
                points = points,
                catalogItems = profileCatalogItems,
                stockRecords = profileStockRecords,
                initialPointId = selectedPointId,
                warehouseProfileId = warehouseProfileId,
                onDismiss = { showTransferDialog = false },
                onConfirm = { fromPointId, fromPointName, toPointId, toPointName, items, comment ->
                    viewModel.recordTransfer(fromPointId, fromPointName, toPointId, toPointName, items, comment)
                }
            )
        }
    }

    if (showIssueDialog) {
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalIssueOperationDialog(
                points = points,
                catalogItems = profileCatalogItems,
                stockRecords = profileStockRecords,
                initialPointId = selectedPointId,
                warehouseProfileId = warehouseProfileId,
                onDismiss = { showIssueDialog = false },
                onConfirm = { fromPointId, fromPointName, toPointId, toPointName, items, comment ->
                    viewModel.recordIssue(fromPointId, fromPointName, toPointId, toPointName, items, comment)
                }
            )
        } else {
            IssueOperationDialog(
                points = points,
                catalogItems = profileCatalogItems,
                stockRecords = profileStockRecords,
                initialPointId = selectedPointId,
                warehouseProfileId = warehouseProfileId,
                onDismiss = { showIssueDialog = false },
                onConfirm = { fromPointId, fromPointName, toPointId, toPointName, items, comment ->
                    viewModel.recordIssue(fromPointId, fromPointName, toPointId, toPointName, items, comment)
                }
            )
        }
    }

    if (showExpenditureDialog) {
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalExpenditureOperationDialog(
                profile = profile,
                points = points,
                catalogItems = profileCatalogItems,
                stockRecords = profileStockRecords,
                initialPointId = selectedPointId,
                warehouseProfileId = warehouseProfileId,
                onDismiss = { showExpenditureDialog = false },
                onConfirm = { fromPointId, pointName, docNumber, responsiblePerson, items, comment ->
                    viewModel.recordExpenditure(fromPointId, pointName, docNumber, responsiblePerson, items, comment)
                }
            )
        } else {
            ExpenditureOperationDialog(
                profile = profile,
                points = points,
                catalogItems = profileCatalogItems,
                stockRecords = profileStockRecords,
                initialPointId = selectedPointId,
                warehouseProfileId = warehouseProfileId,
                onDismiss = { showExpenditureDialog = false },
                onConfirm = { fromPointId, pointName, docNumber, responsiblePerson, items, comment ->
                    viewModel.recordExpenditure(fromPointId, pointName, docNumber, responsiblePerson, items, comment)
                }
            )
        }
    }

    if (showAddPointDialog) {
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalAddPointDialog(
                onDismiss = { showAddPointDialog = false },
                onConfirm = { name, desc ->
                    viewModel.addWarehousePoint(name, desc)
                }
            )
        } else {
            AddPointDialog(
                onDismiss = { showAddPointDialog = false },
                onConfirm = { name, desc ->
                    viewModel.addWarehousePoint(name, desc)
                }
            )
        }
    }

    editingPoint?.let { pt ->
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalEditPointDialog(
                point = pt,
                onDismiss = { editingPoint = null },
                onSave = { updated ->
                    viewModel.updateWarehousePoint(updated)
                },
                onDelete = { ptId ->
                    viewModel.deleteWarehousePoint(ptId)
                }
            )
        } else {
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
    }

    if (showAddCustomItemDialog) {
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalAddItemDialog(
                warehouseProfileId = warehouseProfileId,
                availableCategories = availableCategories,
                onDismiss = { showAddCustomItemDialog = false },
                onConfirm = { name, category, group, unit ->
                    viewModel.addCustomItem(name, category, group, unit)
                }
            )
        } else {
            AddCustomItemDialog(
                availableCategories = availableCategories,
                onDismiss = { showAddCustomItemDialog = false },
                onConfirm = { name, service, subType, unit ->
                    viewModel.addCustomItem(name, service, subType, unit)
                }
            )
        }
    }

    if (showUnitKeySyncDialog) {
        UnitKeySyncDialog(
            profile = profile,
            onRegenerateKey = { viewModel.regenerateUnitKey() },
            onUpdateUnitKey = { newKey -> viewModel.updateUnitKey(newKey) },
            onForceSync = { viewModel.simulateCloudSync() },
            onDismiss = { showUnitKeySyncDialog = false }
        )
    }

    if (showPaymentProDialog) {
        if (BuildConfig.IS_UNIVERSAL_APP) {
            UniversalProDialog(
                entitlement = universalEntitlement,
                backendConfigured = universalBackend.isConfigured,
                hasPendingPayment = universalBackend.pendingPaymentId().isNotBlank(),
                loading = universalSubscriptionLoading,
                message = universalSubscriptionMessage,
                onPay = {
                    universalSubscriptionLoading = true
                    universalSubscriptionMessage = null
                    universalBackend.startMonthlyPayment { result ->
                        universalSubscriptionLoading = false
                        when (result) {
                            is UniversalBackendResult.Success -> {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(result.value.confirmationUrl)
                                        )
                                    )
                                } catch (_: Throwable) {
                                    universalSubscriptionMessage =
                                        "Не удалось открыть страницу оплаты"
                                }
                            }
                            is UniversalBackendResult.Error -> {
                                universalSubscriptionMessage = result.message
                            }
                        }
                    }
                },
                onCheckPayment = {
                    universalSubscriptionLoading = true
                    universalSubscriptionMessage = null
                    universalBackend.checkPendingPayment { result ->
                        universalSubscriptionLoading = false
                        when (result) {
                            is UniversalBackendResult.Success -> {
                                universalEntitlement = result.value
                                universalSubscriptionMessage =
                                    if (result.value.isProActive)
                                        "Оплата подтверждена. PRO активирован."
                                    else
                                        "Платёж ещё не подтверждён."
                            }
                            is UniversalBackendResult.Error -> {
                                universalSubscriptionMessage = result.message
                            }
                        }
                    }
                },
                onDismiss = {
                    showPaymentProDialog = false
                    universalSubscriptionMessage = null
                }
            )
        } else {
            PersonalLicenseDialog(
                profile = profile,
                licenseStatus = licenseStatus,
                yooKassaConfig = viewModel.yooKassaService.getConfig(),
                issuedPaymentKey = issuedPaymentKey,
                onPayYooKassaClick = { viewModel.startYooKassaPayment() },
                onActivateLicenseKey = { key -> viewModel.activateLicenseKey(key) },
                onTestPaymentConfirm = { viewModel.confirmPaymentAndActivateLicense() },
                onRestoreSavedLicense = { viewModel.restoreSavedLicenseOnDevice() },
                onRestoreFromCloud = { email, callsign -> viewModel.restoreLicenseFromCloud(email, callsign) },
                onSaveYooKassaSettings = { shopId, secretKey, isTest, price ->
                    viewModel.saveYooKassaSettings(shopId, secretKey, isTest, price)
                },
                onResendEmailKey = { email ->
                    viewModel.resendLicenseKeyToEmail(email)
                },
                onResetLicense = {
                    viewModel.resetLicense()
                },
                onDismiss = {
                    showPaymentProDialog = false
                    viewModel.clearIssuedPaymentKey()
                }
            )
        }
    }

    if (showDevAuthPrompt) {
        DeveloperAccessPromptDialog(
            onAuthenticate = { secret -> viewModel.authenticateDeveloper(secret) },
            onSuccessAuth = {
                showDevAuthPrompt = false
                showDevAdminDialog = true
            },
            onDismiss = { showDevAuthPrompt = false }
        )
    }

    if (showDevAdminDialog) {
        val syncDeviceId = context
            .getSharedPreferences("kapterka_sync_prefs", android.content.Context.MODE_PRIVATE)
            .getString("device_uuid", "")
            .orEmpty()
        val lastSyncText = if (syncState.lastSyncTime > 0L) {
            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(syncState.lastSyncTime))
        } else {
            "ещё не было"
        }
        val licenseStateText = when {
            licenseStatus.isProActive -> "PRO • ${licenseStatus.daysRemaining} дн."
            licenseStatus.isDemoActive -> "ДЕМО • ${licenseStatus.demoDaysLeft} дн."
            else -> "Не активна"
        }

        DeveloperAdminDialog(
            fightersList = allFighters,
            diagnostics = DeveloperDiagnosticsSnapshot(
                appVersion = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                applicationId = BuildConfig.APPLICATION_ID,
                databaseVersion = 3,
                androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                fighterId = licenseStatus.fighterId,
                syncDeviceId = syncDeviceId,
                callsign = profile?.callsign.orEmpty(),
                unitName = profile?.unitName.orEmpty(),
                unitKey = profile?.unitKey.orEmpty(),
                isLoggedIn = profile?.isLoggedIn == true,
                licenseState = licenseStateText,
                licenseExpires = licenseStatus.expiresAtDateFormatted,
                syncState = syncState.syncMessage,
                isOnline = syncState.isOnline,
                connectedDevices = syncState.connectedDevicesCount,
                lastSync = lastSyncText,
                pointsCount = points.size,
                catalogItemsCount = catalogItems.size,
                stockRecordsCount = stockRecords.size,
                operationsCount = operations.size,
                requisitionsCount = requisitions.size
            ),
            onDeleteFighter = { viewModel.deleteFighterFromRegistry(it) },
            onGrantLicense = { id, days -> viewModel.grantLicenseFromDevMenu(id, days) },
            onRefreshList = { viewModel.refreshFightersRegistry() },
            onForceSync = { viewModel.simulateCloudSync() },
            onDismiss = { showDevAdminDialog = false }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TacticalBg)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        NavigationBar(
            containerColor = TacticalSurface,
            contentColor = SageGreenPrimary,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(22.dp))
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
                                        Text(
                                            "$pendingRequestsCount",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = destination.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SageGreenPrimary,
                        selectedTextColor = SageGreenPrimary,
                        unselectedIconColor = TacticalTextMuted,
                        unselectedTextColor = TacticalTextMuted,
                        indicatorColor = SageGreenDark
                    ),
                    modifier = Modifier.testTag(destination.tag)
                )
            }
        }
    }
}
