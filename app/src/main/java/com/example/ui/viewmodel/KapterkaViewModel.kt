package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.KapterkaDatabase
import com.example.data.model.InventoryItem
import com.example.data.model.ItemWithStock
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequisitionItemEntry
import com.example.data.model.RequisitionRequest
import com.example.data.model.RequestStatus
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.data.repository.KapterkaRepository
import com.example.util.TacticalNotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class KapterkaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KapterkaRepository

    val userProfile: StateFlow<UserProfile?>
    val allPoints: StateFlow<List<WarehousePoint>>
    val allOperations: StateFlow<List<OperationRecord>>
    val allRequisitions: StateFlow<List<RequisitionRequest>>
    val allCatalogItems: StateFlow<List<InventoryItem>>
    val allStockRecords: StateFlow<List<StockRecord>>
    val syncState: StateFlow<com.example.data.sync.SyncState>

    // License & Payments
    private val licenseManager: com.example.data.license.LicenseManager
    val licenseStatus: StateFlow<com.example.data.license.FighterLicenseStatus>
    val yooKassaService: com.example.data.payment.YooKassaPaymentService

    // Registry of all fighters across units (Developer Mode)
    val fighterRegistryManager: com.example.data.admin.FighterRegistryManager
    val allFighters: StateFlow<List<com.example.data.admin.FighterAdminRecord>>

    private val prefs = application.getSharedPreferences("kapterka_app_prefs", android.content.Context.MODE_PRIVATE)

    private val _availableCategories = MutableStateFlow<List<String>>(loadCategoriesFromPrefs())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private fun loadCategoriesFromPrefs(): List<String> {
        val saved = prefs.getStringSet("saved_categories", null)
        return if (saved != null && saved.isNotEmpty()) {
            val defaultOrder = com.example.data.local.InitialData.defaultCategories
            val list = defaultOrder.filter { it in saved }.toMutableList()
            saved.filter { it !in defaultOrder }.forEach { list.add(it) }
            list
        } else {
            com.example.data.local.InitialData.defaultCategories
        }
    }

    private fun saveCategoriesToPrefs(list: List<String>) {
        prefs.edit().putStringSet("saved_categories", list.toSet()).apply()
        _availableCategories.value = list
    }

    fun addCategory(categoryName: String) {
        val clean = categoryName.trim()
        if (clean.isEmpty()) return
        val current = _availableCategories.value.toMutableList()
        if (!current.contains(clean)) {
            current.add(clean)
            saveCategoriesToPrefs(current)
            viewModelScope.launch {
                _toastEvent.emit("Группа «$clean» добавлена")
            }
        }
    }

    fun deleteCategory(categoryName: String, deleteItems: Boolean = true) {
        val current = _availableCategories.value.toMutableList()
        current.remove(categoryName)
        saveCategoriesToPrefs(current)
        if (_selectedCategory.value == categoryName) {
            _selectedCategory.value = "Все виды"
        }
        viewModelScope.launch {
            repository.deleteCategory(categoryName, deleteItems)
            _toastEvent.emit("Группа «$categoryName» удалена")
        }
    }

    fun resetCategoriesToDefault() {
        saveCategoriesToPrefs(com.example.data.local.InitialData.defaultCategories)
        viewModelScope.launch {
            _toastEvent.emit("Штатные группы восстановлены по умолчанию")
        }
    }

    // Selected state on Main Dashboard
    private val _selectedPointId = MutableStateFlow<String>("base_sklad")
    val selectedPointId: StateFlow<String> = _selectedPointId.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("Все виды")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _inventorySearchQuery = MutableStateFlow<String>("")
    val inventorySearchQuery: StateFlow<String> = _inventorySearchQuery.asStateFlow()

    // History filter
    private val _historyFilterType = MutableStateFlow<OperationType?>(null)
    val historyFilterType: StateFlow<OperationType?> = _historyFilterType.asStateFlow()

    private val _historySearchQuery = MutableStateFlow<String>("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // Combined Items With Stock StateFlow
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val itemsWithStock: StateFlow<List<ItemWithStock>>

    // UI Message Events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        val database = KapterkaDatabase.getDatabase(application, viewModelScope)
        val syncManager = com.example.data.sync.FirebaseSyncManager(application, database.kapterkaDao(), viewModelScope)
        repository = KapterkaRepository(database.kapterkaDao(), syncManager)

        licenseManager = com.example.data.license.LicenseManager(application, database.kapterkaDao(), viewModelScope)
        licenseStatus = licenseManager.licenseStatus
        yooKassaService = com.example.data.payment.YooKassaPaymentService(application)

        fighterRegistryManager = com.example.data.admin.FighterRegistryManager(application, viewModelScope)
        allFighters = fighterRegistryManager.fighters

        syncState = (repository.syncState ?: MutableStateFlow(com.example.data.sync.SyncState()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.sync.SyncState())

        userProfile = repository.userProfile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        allPoints = repository.allPoints
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allOperations = repository.allOperations
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allRequisitions = repository.allRequisitions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allCatalogItems = repository.allItems
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allStockRecords = repository.allStockRecords
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        itemsWithStock = combine(
            _selectedPointId,
            _selectedCategory,
            _inventorySearchQuery,
            allCatalogItems
        ) { ptId, cat, query, items ->
            Quadruple(ptId, cat, query, items)
        }.flatMapLatest { (ptId, cat, query, items) ->
            repository.getStockForPoint(ptId).combine(MutableStateFlow(items)) { stocks, catalog ->
                val stockMap = stocks.associateBy { it.itemId }
                val cleanQuery = query.trim().lowercase()

                catalog
                    .filter { item ->
                        if (cat == "Все виды") true
                        else item.serviceCategory == cat
                    }
                    .filter { item ->
                        if (cleanQuery.isEmpty()) true
                        else item.name.lowercase().contains(cleanQuery) ||
                                item.subType.lowercase().contains(cleanQuery) ||
                                item.serviceCategory.lowercase().contains(cleanQuery)
                    }
                    .map { item ->
                        val st = stockMap[item.id]
                        ItemWithStock(
                            item = item,
                            quantity = st?.quantity ?: 0,
                            incomeTotal = st?.incomeTotal ?: 0,
                            expenseTotal = st?.expenseTotal ?: 0
                        )
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun selectPoint(pointId: String) {
        _selectedPointId.value = pointId
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setInventorySearchQuery(query: String) {
        _inventorySearchQuery.value = query
    }

    fun setHistoryFilterType(type: OperationType?) {
        _historyFilterType.value = type
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    // Helper to get base warehouse stock summary
    private fun getBaseStockSummary(): String {
        return itemsWithStock.value
            .filter { it.quantity > 0 }
            .take(4)
            .joinToString(", ") { "${it.item.name}: ${it.quantity} ${it.item.unit}" }
    }

    // Operations Handlers
    fun recordIncome(
        toPointId: String,
        toPointName: String,
        supplier: String,
        items: List<OperationItemEntry>,
        comment: String
    ) {
        viewModelScope.launch {
            val actor = userProfile.value?.callsign ?: "Ответственный"
            repository.recordIncome(toPointId, toPointName, supplier, items, comment, actor)
            val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
            TacticalNotificationHelper.notifyIncome(
                context = getApplication(),
                toPoint = toPointName,
                supplier = supplier.ifEmpty { "Служба снабжения" },
                itemsSummary = summary,
                baseWarehouseStockSummary = getBaseStockSummary()
            )
            _toastEvent.emit("Операция «Привезли» успешно сохранена")
        }
    }

    fun recordTransfer(
        fromPointId: String,
        fromPointName: String,
        toPointId: String,
        toPointName: String,
        items: List<OperationItemEntry>,
        comment: String
    ) {
        viewModelScope.launch {
            val actor = userProfile.value?.callsign ?: "Ответственный"
            repository.recordTransfer(fromPointId, fromPointName, toPointId, toPointName, items, comment, actor)
            val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
            TacticalNotificationHelper.notifyTransfer(
                context = getApplication(),
                fromPoint = fromPointName,
                toPoint = toPointName,
                itemsSummary = summary,
                baseWarehouseStockSummary = getBaseStockSummary()
            )
            _toastEvent.emit("Перемещение выполнено: $fromPointName ➔ $toPointName")
        }
    }

    fun recordIssue(
        fromPointId: String,
        fromPointName: String,
        toPointId: String,
        toPointName: String,
        items: List<OperationItemEntry>,
        comment: String
    ) {
        viewModelScope.launch {
            val actor = userProfile.value?.callsign ?: "Старшина"
            repository.recordIssue(fromPointId, fromPointName, toPointId, toPointName, items, comment, actor)
            
            val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
            TacticalNotificationHelper.notifyIssue(
                context = getApplication(),
                fromPoint = fromPointName,
                toPoint = toPointName,
                itemsSummary = summary,
                baseWarehouseStockSummary = getBaseStockSummary()
            )
            
            _toastEvent.emit("Имущество выдано («Подняли»)")
        }
    }

    fun recordExpenditure(
        fromPointId: String,
        pointName: String,
        docNumber: String,
        responsiblePerson: String,
        items: List<OperationItemEntry>,
        comment: String
    ) {
        viewModelScope.launch {
            repository.recordExpenditure(fromPointId, pointName, docNumber, responsiblePerson, items, comment)
            val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
            TacticalNotificationHelper.notifyExpenditure(
                context = getApplication(),
                pointName = pointName,
                docNumber = docNumber,
                itemsSummary = summary,
                reason = comment
            )
            _toastEvent.emit("Акт расхода (ф. 8) оформлен: № $docNumber")
        }
    }

    // Points
    fun addWarehousePoint(name: String, description: String) {
        viewModelScope.launch {
            repository.addWarehousePoint(name, description)
            _toastEvent.emit("Точка «$name» добавлена в журнал")
        }
    }

    fun updateWarehousePoint(point: WarehousePoint) {
        viewModelScope.launch {
            repository.updateWarehousePoint(point)
            _toastEvent.emit("Точка «${point.name}» обновлена")
        }
    }

    fun deleteWarehousePoint(pointId: String) {
        viewModelScope.launch {
            repository.deleteWarehousePoint(pointId)
            _toastEvent.emit("Точка удалена")
        }
    }

    // Custom Nomenclature Item
    fun addCustomItem(name: String, serviceCategory: String, subType: String, unit: String) {
        viewModelScope.launch {
            repository.addCustomInventoryItem(name, serviceCategory, subType, unit)
            _toastEvent.emit("Позиция «$name» внесена в номенклатуру")
        }
    }

    // Requisitions
    fun createRequisition(
        pointName: String,
        applicantName: String,
        items: List<RequisitionItemEntry>,
        comment: String
    ) {
        viewModelScope.launch {
            repository.createRequisition(pointName, applicantName, items, comment)
            val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
            val req = RequisitionRequest(
                id = UUID.randomUUID().toString(),
                pointName = pointName,
                applicantName = applicantName,
                status = RequestStatus.PENDING,
                comment = comment,
                itemsSummary = summary
            )
            TacticalNotificationHelper.notifyRequisitionStatus(
                context = getApplication(),
                req = req,
                status = RequestStatus.PENDING
            )
            _toastEvent.emit("Заявка успешно отправлена на комплектацию")
        }
    }

    fun updateRequisitionStatus(req: RequisitionRequest, newStatus: RequestStatus) {
        viewModelScope.launch {
            repository.updateRequisitionStatus(req, newStatus)
            TacticalNotificationHelper.notifyRequisitionStatus(
                context = getApplication(),
                req = req,
                status = newStatus
            )
            _toastEvent.emit("Статус заявки изменен на: ${newStatus.titleRu}")
        }
    }

    fun deleteRequisition(reqId: String) {
        viewModelScope.launch {
            repository.deleteRequisition(reqId)
            _toastEvent.emit("Заявка удалена")
        }
    }

    // Catalog Item CRUD & Stock Adjust
    fun updateCatalogItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.updateInventoryItem(item)
            _toastEvent.emit("Позиция «${item.name}» успешно обновлена")
        }
    }

    fun deleteCatalogItem(itemId: String, itemName: String) {
        viewModelScope.launch {
            repository.deleteInventoryItem(itemId)
            _toastEvent.emit("Позиция «$itemName» удалена из номенклатуры")
        }
    }

    fun adjustPointStock(pointId: String, pointName: String, itemId: String, itemName: String, newQuantity: Int) {
        viewModelScope.launch {
            repository.adjustStockQuantity(pointId, itemId, newQuantity)
            _toastEvent.emit("Остаток «$itemName» на «$pointName» изменен: $newQuantity")
        }
    }

    // User Profile / Settings / Auth
    fun registerOrLoginProfile(profile: UserProfile, isNewRegistration: Boolean = false) {
        viewModelScope.launch {
            var resolvedKey = profile.unitKey.trim()
            var resolvedUnitName = profile.unitName.trim()

            // 1. Проверяем облачный реестр бойцов по позывному / email, чтобы не потерять ключ подразделения
            val cloudRecord = fighterRegistryManager.lookupFighter(profile.callsign)
                ?: if (profile.email.isNotBlank()) fighterRegistryManager.lookupFighter(profile.email) else null

            if (cloudRecord != null) {
                if (resolvedKey.isBlank() || resolvedKey.startsWith("kapt_") && cloudRecord.unitKey.isNotBlank()) {
                    resolvedKey = cloudRecord.unitKey
                }
                if (resolvedUnitName.isBlank() || resolvedUnitName == "1-е Подразделение") {
                    resolvedUnitName = cloudRecord.unitName.ifBlank { resolvedUnitName }
                }
            }

            if (resolvedKey.isBlank()) {
                resolvedKey = "kapt_" + UUID.randomUUID().toString().take(6)
            }
            if (resolvedUnitName.isBlank()) {
                resolvedUnitName = "1-е Подразделение"
            }

            val updatedProfile = profile.copy(
                unitKey = resolvedKey,
                unitName = resolvedUnitName,
                isLoggedIn = true
            )
            repository.saveUserProfile(updatedProfile)

            // 2. Пытаемся автоматически подтянуть ранее оплаченную лицензию из облака
            val (restored, restoreMsg) = licenseManager.restoreLicenseFromCloud(
                email = updatedProfile.email,
                callsign = updatedProfile.callsign,
                unitKey = resolvedKey
            )

            val curLicense = licenseManager.licenseStatus.value
            fighterRegistryManager.registerOrUpdateFighter(
                fighterId = licenseManager.getFighterPersonalId(),
                callsign = updatedProfile.callsign,
                unitName = resolvedUnitName,
                unitKey = resolvedKey,
                email = updatedProfile.email,
                licenseKey = curLicense.licenseKey.ifEmpty { curLicense.lastSavedKey },
                isProActive = curLicense.isProActive,
                expiresAt = System.currentTimeMillis() + (curLicense.daysRemaining.toLong() * 86400000L),
                role = "Старшина подразделения"
            )

            if (restored) {
                _toastEvent.emit("Вход выполнен! Ключ: $resolvedKey. $restoreMsg")
            } else {
                _toastEvent.emit("Вход выполнен! Подразделение: $resolvedUnitName")
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            val curLicense = licenseManager.licenseStatus.value
            fighterRegistryManager.registerOrUpdateFighter(
                fighterId = licenseManager.getFighterPersonalId(),
                callsign = profile.callsign,
                unitName = profile.unitName,
                unitKey = profile.unitKey,
                email = profile.email,
                licenseKey = curLicense.licenseKey.ifEmpty { curLicense.lastSavedKey },
                isProActive = curLicense.isProActive,
                expiresAt = System.currentTimeMillis() + (curLicense.daysRemaining.toLong() * 86400000L)
            )
            _toastEvent.emit("Настройки профиля сохранены")
        }
    }

    fun restoreLicenseFromCloud(customEmail: String? = null, customCallsign: String? = null) {
        viewModelScope.launch {
            val prof = userProfile.value
            val emailToUse = customEmail?.ifBlank { prof?.email.orEmpty() } ?: prof?.email.orEmpty()
            val callsignToUse = customCallsign?.ifBlank { prof?.callsign.orEmpty() } ?: prof?.callsign.orEmpty()
            val unitKeyToUse = prof?.unitKey.orEmpty()

            _toastEvent.emit("Поиск оплаченной лицензии в облачной базе...")
            val (success, msg) = licenseManager.restoreLicenseFromCloud(emailToUse, callsignToUse, unitKeyToUse)
            if (success) {
                val curProfile = userProfile.value ?: UserProfile()
                repository.saveUserProfile(curProfile.copy(isProActive = true, proDaysLeft = 30, demoDaysLeft = 0))
            }
            _toastEvent.emit(msg)
        }
    }

    fun activateProSubscription() {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.saveUserProfile(current.copy(isProActive = true, proDaysLeft = 30))
            _toastEvent.emit("Подписка «Каптёрка ПРО» успешно активирована на 30 дней!")
        }
    }

    fun regenerateUnitKey() {
        val newKey = "kapt_" + UUID.randomUUID().toString().take(6)
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.saveUserProfile(current.copy(unitKey = newKey))
            _toastEvent.emit("Новый ключ подразделения: $newKey")
        }
    }

    fun simulateCloudSync() {
        viewModelScope.launch {
            _toastEvent.emit("Запуск онлайн-синхронизации базы...")
            repository.triggerCloudSync()
            _toastEvent.emit("База подразделения успешно синхронизирована!")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _toastEvent.emit("Все операции и остатки успешно удалены")
        }
    }

    /**
     * Сброс текущей лицензии и профиля для проверки регистрации нового бойца с нуля
     */
    fun resetProfileAndLicenseForTesting() {
        viewModelScope.launch {
            licenseManager.resetLicense()
            val freshProfile = UserProfile(
                callsign = "",
                unitName = "1-е Подразделение",
                unitKey = "kapt_" + UUID.randomUUID().toString().take(6),
                email = "",
                isLoggedIn = false,
                isProActive = false,
                proDaysLeft = 0,
                demoDaysLeft = 0
            )
            repository.saveUserProfile(freshProfile)
            _toastEvent.emit("Сессия и лицензия сброшены. Войдите как новый пользователь.")
        }
    }

    // Excel exports
    fun getForm8ExcelText(): String {
        val ops = allOperations.value
        val unit = userProfile.value?.unitName ?: "1-е Подразделение"
        return repository.generateForm8ExcelText(ops, unit)
    }

    fun getForm18ExcelText(): String {
        val ops = allOperations.value
        val unit = userProfile.value?.unitName ?: "1-е Подразделение"
        return repository.generateForm18ExcelText(ops, unit)
    }

    fun parseOperationItems(json: String): List<OperationItemEntry> {
        return repository.parseOperationItems(json)
    }

    fun parseRequisitionItems(json: String): List<RequisitionItemEntry> {
        return repository.parseRequisitionItems(json)
    }

    // --- LICENSE & YOOKASSA ACTIONS ---
    private var lastPaymentId: String = ""

    fun startYooKassaPayment() {
        viewModelScope.launch {
            val profile = userProfile.value
            val callsign = profile?.callsign?.ifBlank { "Боец" } ?: "Боец"
            val email = profile?.email?.trim().orEmpty()

            _toastEvent.emit("Формирование счета ЮKassa на 30 дней...")
            val result = yooKassaService.createPayment(callsign, email)
            if (result.success && result.confirmationUrl.isNotEmpty()) {
                lastPaymentId = result.paymentId
                prefs.edit().putString("last_yookassa_payment_id", result.paymentId).apply()
                yooKassaService.openPaymentUrl(result.confirmationUrl)
                _toastEvent.emit("Переход к оплате ЮKassa (СБП/Карта)...")
            } else {
                _toastEvent.emit(result.errorMessage ?: "Не удалось создать счет ЮKassa")
            }
        }
    }

    fun confirmPaymentAndActivateLicense() {
        viewModelScope.launch {
            val profile = userProfile.value
            val callsign = profile?.callsign?.ifBlank { "Боец" } ?: "Боец"
            val email = profile?.email?.trim().orEmpty()
            val unitName = profile?.unitName ?: "1-е Подразделение"
            val unitKey = profile?.unitKey ?: "kapt_default"

            val paymentIdToVerify = if (lastPaymentId.isNotBlank()) {
                lastPaymentId
            } else {
                prefs.getString("last_yookassa_payment_id", "") ?: ""
            }

            if (paymentIdToVerify.isBlank()) {
                _toastEvent.emit("Счет на оплату еще не был сформирован. Сначала нажмите «Оплатить через ЮKassa / СБП». Ключ выдается только после подтверждения.")
                return@launch
            }

            _toastEvent.emit("Проверка статуса оплаты в ЮKassa (ID: $paymentIdToVerify)...")

            val (isPaid, statusMsg) = yooKassaService.verifyPaymentStatus(paymentIdToVerify)
            if (!isPaid) {
                _toastEvent.emit("Платеж не подтвержден: $statusMsg. Ключ не может быть выдан без подтверждения оплаты.")
                return@launch
            }

            // Ключ генерируется и активируется СТРОГО после подтверждения ЮKassa
            val newKey = licenseManager.activateLicenseAfterPayment(callsign, email, paymentIdToVerify)

            // Заносим бойца в реестр всех подразделений
            fighterRegistryManager.registerOrUpdateFighter(
                fighterId = licenseManager.getFighterPersonalId(),
                callsign = callsign,
                unitName = unitName,
                unitKey = unitKey,
                email = email,
                licenseKey = newKey,
                isProActive = true,
                expiresAt = System.currentTimeMillis() + 30L * 86400000L
            )

            _toastEvent.emit("Оплата подтверждена ЮKassa! Выдан персональный ключ: $newKey (30 дней)")
        }
    }

    fun activateLicenseKey(enteredKey: String) {
        viewModelScope.launch {
            val profile = userProfile.value
            val callsign = profile?.callsign ?: "Боец"
            val (success, message) = licenseManager.activateKeyManually(enteredKey, callsign)
            if (success) {
                val curProfile = userProfile.value ?: UserProfile()
                repository.saveUserProfile(curProfile.copy(isProActive = true, proDaysLeft = 30))
                fighterRegistryManager.registerOrUpdateFighter(
                    fighterId = licenseManager.getFighterPersonalId(),
                    callsign = callsign,
                    unitName = curProfile.unitName,
                    unitKey = curProfile.unitKey,
                    email = curProfile.email,
                    licenseKey = enteredKey.trim().uppercase(),
                    isProActive = true,
                    expiresAt = System.currentTimeMillis() + 30L * 86400000L
                )
            }
            _toastEvent.emit(message)
        }
    }

    /**
     * Восстанавливает лицензию по ранее сохраненному ключу на этом устройстве
     */
    fun restoreSavedLicenseOnDevice() {
        viewModelScope.launch {
            val (success, msg) = licenseManager.restoreSavedLicense()
            if (success) {
                val curProfile = userProfile.value ?: UserProfile()
                repository.saveUserProfile(curProfile.copy(isProActive = true, proDaysLeft = 30))
            }
            _toastEvent.emit(msg)
        }
    }

    // --- DEVELOPER BACKDOOR ACTIONS ---

    fun deleteFighterFromRegistry(fighterId: String) {
        fighterRegistryManager.deleteFighter(fighterId)
        viewModelScope.launch {
            _toastEvent.emit("Боец удален из реестра подразделений")
        }
    }

    fun grantLicenseFromDevMenu(fighterId: String, days: Int = 30) {
        val newKey = fighterRegistryManager.grantLicense(fighterId, days)
        viewModelScope.launch {
            _toastEvent.emit("Выдан ключ: $newKey на $days дней")
        }
    }

    fun refreshFightersRegistry() {
        viewModelScope.launch {
            fighterRegistryManager.fetchFightersFromCloud()
            _toastEvent.emit("Реестр бойцов обновлен")
        }
    }

    fun saveYooKassaSettings(shopId: String, secretKey: String, isTestMode: Boolean, priceRubles: Int) {
        yooKassaService.saveConfig(shopId, secretKey, isTestMode, priceRubles)
        viewModelScope.launch {
            _toastEvent.emit("Настройки ЮKassa сохранены (ShopID: $shopId)")
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
