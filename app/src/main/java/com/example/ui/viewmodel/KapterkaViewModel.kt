package com.example.ui.viewmodel

import android.app.Application
import com.example.BuildConfig
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class KapterkaViewModel(application: Application) : AndroidViewModel(application) {
    


    private val repository: KapterkaRepository
    private var universalSyncManager: com.example.data.sync.UniversalFirestoreSyncManager? = null

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
    private val adminBackendService = com.example.data.admin.AdminBackendService()
    private var adminSessionToken: String = ""

    private val prefs = application.getSharedPreferences("kapterka_app_prefs", android.content.Context.MODE_PRIVATE)

    private val _availableCategories = MutableStateFlow<List<String>>(loadCategoriesFromPrefs())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private fun loadCategoriesFromPrefs(): List<String> {
        val saved = prefs.getStringSet("saved_categories", null)
        val defaultOrder = com.example.data.local.InitialData.getDefaultCategories()
        return if (saved != null && saved.isNotEmpty()) {
            val list = defaultOrder.toMutableList() // Always include default categories (to force new ones)
            saved.filter { it !in defaultOrder }.forEach { list.add(it) }
            list
        } else {
            defaultOrder
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
                _toastEvent.emit(if (BuildConfig.IS_UNIVERSAL_APP) "Категория «$clean» добавлена" else "Группа «$clean» добавлена")
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
        saveCategoriesToPrefs(com.example.data.local.InitialData.getDefaultCategories())
        viewModelScope.launch {
            _toastEvent.emit("Группы по умолчанию восстановлены")
        }
    }

    fun ensureWarehouseProfileStarterCatalog(profileId: String) {
        if (!BuildConfig.IS_UNIVERSAL_APP) return

        viewModelScope.launch {
            val added = repository.ensureUniversalStarterCatalog(profileId)
            if (added > 0) {
                _toastEvent.emit("Готовый каталог добавлен • $added позиций")
            }
        }
    }

    fun applyWarehouseProfile(profileId: String) {
        val template = com.example.universal.WarehouseProfileCatalog.find(profileId)
        val customCategories = _availableCategories.value.filter {
            it !in com.example.universal.WarehouseProfileCatalog.allPresetCategories
        }
        val merged = (template.categories + customCategories).distinct()
        saveCategoriesToPrefs(merged)
        _selectedCategory.value = "Все виды"
        viewModelScope.launch {
            _toastEvent.emit("Профиль «${template.title}» применён")
        }
    }

    // Theme Mode (Light / Dark)
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.value = newTheme
        prefs.edit().putBoolean("is_dark_theme", newTheme).apply()
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
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
        val syncGateway: com.example.data.sync.WarehouseSyncGateway = if (BuildConfig.IS_UNIVERSAL_APP) {
            com.example.data.sync.UniversalFirestoreSyncManager(
                application,
                database.kapterkaDao(),
                viewModelScope
            ).also { universalSyncManager = it }
        } else {
            com.example.data.sync.LegacySyncGateway(
                com.example.data.sync.FirebaseSyncManager(
                    application,
                    database.kapterkaDao(),
                    viewModelScope
                )
            )
        }
        repository = KapterkaRepository(database.kapterkaDao(), syncGateway)
        
        repository.syncEvents?.let { eventsFlow ->
            viewModelScope.launch {
                eventsFlow.collect { eventMsg ->
                    _toastEvent.emit(eventMsg)
                    try {
                        val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = android.media.RingtoneManager.getRingtone(application.applicationContext, ringtoneUri)
                        ringtone?.play()
                    } catch (e: Throwable) {
                        android.util.Log.w("KapterkaViewModel", "Ringtone play ignored: ${e.message}")
                    }
                }
            }
        }

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

    fun saveUniversalProfile(profile: UserProfile) {
        if (!BuildConfig.IS_UNIVERSAL_APP) return

        viewModelScope.launch {
            repository.saveUserProfile(
                profile.copy(unitKey = "")
            )
        }
    }

    fun setUniversalCloudSyncEnabled(enabled: Boolean) {
        if (!BuildConfig.IS_UNIVERSAL_APP) return

        universalSyncManager?.setEnabled(enabled)
        if (enabled) {
            viewModelScope.launch {
                val result = repository.triggerCloudSync()
                if (result.first) {
                    _toastEvent.emit("Облачная синхронизация PRO включена")
                }
            }
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
            val src = supplier.ifBlank {
                if (BuildConfig.IS_UNIVERSAL_APP) "Поставщик не указан" else "Служба снабжения / Тыл"
            }
            repository.recordIncome(toPointId, toPointName, src, items, comment, actor)
            val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
            if (!BuildConfig.IS_UNIVERSAL_APP) {
                TacticalNotificationHelper.notifyIncome(
                    context = getApplication(),
                    toPoint = toPointName,
                    supplier = src,
                    itemsSummary = summary,
                    baseWarehouseStockSummary = getBaseStockSummary()
                )
            }
            _toastEvent.emit(
                if (BuildConfig.IS_UNIVERSAL_APP) {
                    "Поступление сохранено • $toPointName\n$summary"
                } else {
                    "📥 ПРИВЕЗЛИ на $toPointName\nОткуда: $src\nПринято: $summary"
                }
            )
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
            if (!BuildConfig.IS_UNIVERSAL_APP) {
                TacticalNotificationHelper.notifyTransfer(
                    context = getApplication(),
                    fromPoint = fromPointName,
                    toPoint = toPointName,
                    itemsSummary = summary,
                    baseWarehouseStockSummary = getBaseStockSummary()
                )
            }
            _toastEvent.emit(
                if (BuildConfig.IS_UNIVERSAL_APP) {
                    "Перемещение сохранено • $fromPointName → $toPointName\n$summary"
                } else {
                    "🔄 ПЕРЕМЕЩЕНИЕ:\nМаршрут: $fromPointName ➔ $toPointName\nПередано: $summary"
                }
            )
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
            val actor = userProfile.value?.callsign
                ?: if (BuildConfig.IS_UNIVERSAL_APP) "Ответственный" else "Старшина подразделения"
            repository.recordIssue(fromPointId, fromPointName, toPointId, toPointName, items, comment, actor)

            val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
            if (!BuildConfig.IS_UNIVERSAL_APP) {
                TacticalNotificationHelper.notifyIssue(
                    context = getApplication(),
                    fromPoint = fromPointName,
                    toPoint = toPointName,
                    itemsSummary = summary,
                    baseWarehouseStockSummary = getBaseStockSummary()
                )
            }

            _toastEvent.emit(
                if (BuildConfig.IS_UNIVERSAL_APP) {
                    "Выдача сохранена • $fromPointName → $toPointName\n$summary"
                } else {
                    "⬆️ ПОДНЯЛИ (ВЫДАНО):\nМаршрут: $fromPointName ➔ $toPointName\nВыдано: $summary"
                }
            )
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
            if (!BuildConfig.IS_UNIVERSAL_APP) {
                TacticalNotificationHelper.notifyExpenditure(
                    context = getApplication(),
                    pointName = pointName,
                    docNumber = docNumber,
                    itemsSummary = summary,
                    reason = comment
                )
            }
            val docLabel = if (docNumber.isNotBlank()) " (Акт № $docNumber)" else ""
            _toastEvent.emit(
                if (BuildConfig.IS_UNIVERSAL_APP) {
                    "Списание сохранено$docLabel • $pointName\n$summary"
                } else {
                    "💥 РАСХОД (Ф. 8)$docLabel:\nТочка: $pointName\nСписано: $summary"
                }
            )
        }
    }

    // Points
    fun addWarehousePoint(name: String, description: String) {
        viewModelScope.launch {
            repository.addWarehousePoint(name, description)
            _toastEvent.emit(if (BuildConfig.IS_UNIVERSAL_APP) "Склад «$name» добавлен" else "Точка «$name» добавлена в журнал")
        }
    }

    fun updateWarehousePoint(point: WarehousePoint) {
        viewModelScope.launch {
            repository.updateWarehousePoint(point)
            _toastEvent.emit(if (BuildConfig.IS_UNIVERSAL_APP) "Склад «${point.name}» обновлён" else "Точка «${point.name}» обновлена")
        }
    }

    fun deleteWarehousePoint(pointId: String) {
        viewModelScope.launch {
            repository.deleteWarehousePoint(pointId)
            _toastEvent.emit(if (BuildConfig.IS_UNIVERSAL_APP) "Склад удалён" else "Точка удалена")
        }
    }

    fun reorderWarehousePoints(orderedPoints: List<WarehousePoint>) {
        viewModelScope.launch {
            repository.reorderWarehousePoints(orderedPoints)
            _toastEvent.emit("Порядок складов и точек сохранён")
        }
    }

    // Custom Nomenclature Item
    fun addCustomItem(name: String, serviceCategory: String, subType: String, unit: String) {
        val cleanCategory = serviceCategory.trim()
        if (BuildConfig.IS_UNIVERSAL_APP && cleanCategory.isNotEmpty() && cleanCategory !in _availableCategories.value) {
            saveCategoriesToPrefs((_availableCategories.value + cleanCategory).distinct())
        }

        viewModelScope.launch {
            repository.addCustomInventoryItem(name, cleanCategory.ifBlank { serviceCategory }, subType, unit)
            _toastEvent.emit(
                if (BuildConfig.IS_UNIVERSAL_APP) {
                    "Позиция «$name» добавлена в каталог"
                } else {
                    "Позиция «$name» внесена в номенклатуру"
                }
            )
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
            repository.updateRequisitionStatus(req.id, newStatus)
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
            repository.setStockAbsoluteQuantity(pointId, itemId, newQuantity)
            _toastEvent.emit("Остаток «$itemName» на «$pointName» установлен: $newQuantity")
        }
    }

    // User Profile / Settings / Auth
    fun registerOrLoginProfile(profile: UserProfile, isNewRegistration: Boolean = false) {
        viewModelScope.launch {
            var resolvedKey = profile.unitKey.trim()
            var resolvedUnitName = profile.unitName.trim()

            // 1. Проверяем облачный реестр бойцов по email, позывному или id устройства, чтобы не потерять ключ подразделения
            val cloudRecord = fighterRegistryManager.lookupFighter(
                email = profile.email,
                callsign = profile.callsign,
                fighterId = licenseManager.getFighterPersonalId()
            )

            if (cloudRecord != null) {
                if (resolvedKey.isBlank() && cloudRecord.unitKey.isNotBlank()) {
                    resolvedKey = cloudRecord.unitKey
                }
                if (resolvedUnitName.isBlank() || resolvedUnitName == "1-е Подразделение") {
                    resolvedUnitName = cloudRecord.unitName.ifBlank { resolvedUnitName }
                }
            }

            // Если ключ всё ещё пустой, пробуем достать ранее сохраненный из перманентного хранилища
            if (resolvedKey.isBlank()) {
                resolvedKey = licenseManager.getSavedUnitKeyFromVault() ?: ""
            }

            if (resolvedUnitName.isBlank()) {
                resolvedUnitName = "1-е Подразделение"
            }

            val current = userProfile.value ?: UserProfile()
            val oldKey = current.unitKey.trim()
            val isUnitSwitch = oldKey.isNotBlank() &&
                resolvedKey.isNotBlank() &&
                !oldKey.equals(resolvedKey, ignoreCase = true)

            if (isUnitSwitch && repository.hasLocalUnitData()) {
                _toastEvent.emit(
                    "Ключ подразделения не изменён: на телефоне есть рабочие данные. " +
                        "Автоматическая очистка запрещена для защиты остатков и истории."
                )
                return@launch
            }

            // Persist a unit key only after the switch is proven safe.
            licenseManager.saveUnitKeyToVault(resolvedKey)

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
            repository.triggerCloudSync()
            
            // Отправляем уведомление разработчику в Telegram
            com.example.data.notification.TelegramNotifier.notifyRegistration(
                callsign = updatedProfile.callsign,
                unitName = resolvedUnitName,
                unitKey = resolvedKey,
                email = updatedProfile.email
            )
        }
    }

    fun updateUnitKey(newKey: String) {
        val clean = newKey.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            val oldKey = current.unitKey.trim()
            if (oldKey.equals(clean, ignoreCase = true)) {
                _toastEvent.emit("Ключ подразделения уже используется")
                return@launch
            }
            if (oldKey.isNotBlank() && repository.hasLocalUnitData()) {
                _toastEvent.emit(
                    "Смена ключа заблокирована: на телефоне есть остатки, история, заявки " +
                        "или пользовательские данные. Ничего не удалено."
                )
                return@launch
            }

            licenseManager.saveUnitKeyToVault(clean)
            val updated = current.copy(unitKey = clean)
            repository.saveUserProfile(updated)
            
            val curLicense = licenseManager.licenseStatus.value
            fighterRegistryManager.registerOrUpdateFighter(
                fighterId = licenseManager.getFighterPersonalId(),
                callsign = updated.callsign,
                unitName = updated.unitName,
                unitKey = updated.unitKey,
                email = updated.email,
                licenseKey = curLicense.licenseKey.ifEmpty { curLicense.lastSavedKey },
                isProActive = curLicense.isProActive,
                expiresAt = System.currentTimeMillis() + (curLicense.daysRemaining.toLong() * 86400000L),
                role = "Старшина подразделения"
            )
            
            val (_, msg) = repository.triggerCloudSync()
            _toastEvent.emit(msg)
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            val oldKey = current.unitKey.trim()
            val requestedKey = profile.unitKey.trim().ifBlank { oldKey }
            val wantsUnitSwitch = oldKey.isNotBlank() &&
                requestedKey.isNotBlank() &&
                !oldKey.equals(requestedKey, ignoreCase = true)

            val safeProfile = if (wantsUnitSwitch && repository.hasLocalUnitData()) {
                _toastEvent.emit(
                    "Профиль сохранён без смены ключа подразделения: локальные складские данные защищены."
                )
                profile.copy(unitKey = current.unitKey)
            } else {
                if (requestedKey.isNotBlank()) licenseManager.saveUnitKeyToVault(requestedKey)
                profile.copy(unitKey = requestedKey)
            }

            repository.saveUserProfile(safeProfile)
            val curLicense = licenseManager.licenseStatus.value
            fighterRegistryManager.registerOrUpdateFighter(
                fighterId = licenseManager.getFighterPersonalId(),
                callsign = safeProfile.callsign,
                unitName = safeProfile.unitName,
                unitKey = safeProfile.unitKey,
                email = safeProfile.email,
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
                val daysLeft = licenseManager.licenseStatus.value.daysRemaining.coerceAtLeast(1)
                repository.saveUserProfile(
                    curProfile.copy(
                        isProActive = true,
                        proDaysLeft = daysLeft,
                        demoDaysLeft = 0
                    )
                )
            }
            _toastEvent.emit(msg)
        }
    }

    @Deprecated("PRO activation is server-authoritative in next-safe")
    fun activateProSubscription() {
        viewModelScope.launch {
            _toastEvent.emit(
                "Локальная активация PRO отключена. Лицензия активируется только после подтверждения сервера."
            )
        }
    }

    fun regenerateUnitKey() {
        val newKey = com.example.data.sync.SyncIdentityGenerator.newUnitKey()
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            if (current.unitKey.isNotBlank() && repository.hasLocalUnitData()) {
                _toastEvent.emit(
                    "Новый ключ не создан: на телефоне есть данные текущего подразделения. Ничего не удалено."
                )
                return@launch
            }
            licenseManager.saveUnitKeyToVault(newKey)
            repository.saveUserProfile(current.copy(unitKey = newKey))
            _toastEvent.emit("Новый ключ подразделения: $newKey")
        }
    }

    fun simulateCloudSync() {
        viewModelScope.launch {
            _toastEvent.emit("Запуск онлайн-синхронизации базы...")
            val (_, msg) = repository.triggerCloudSync()
            _toastEvent.emit(msg)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _toastEvent.emit("Все операции и остатки успешно удалены")
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
    private var paymentPollingJob: kotlinx.coroutines.Job? = null
    private var paymentActivationInProgress = false

    private val _issuedPaymentKey = MutableStateFlow<String?>(null)
    val issuedPaymentKey: StateFlow<String?> = _issuedPaymentKey.asStateFlow()

    fun clearIssuedPaymentKey() {
        _issuedPaymentKey.value = null
    }

    fun startYooKassaPayment() {
        viewModelScope.launch {
            val profile = userProfile.value
            val callsign = profile?.callsign?.ifBlank { "Боец" } ?: "Боец"
            val email = profile?.email?.trim()?.lowercase().orEmpty()

            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _toastEvent.emit("Для оплаты укажите корректный Email в профиле. На него придёт информация о лицензии.")
                return@launch
            }

            _toastEvent.emit("Формирование счета ЮKassa на 30 дней...")
            val fighterId = licenseManager.getFighterPersonalId()
            val result = yooKassaService.createPayment(
                fighterCallsign = callsign,
                fighterEmail = email,
                fighterId = fighterId
            )
            if (result.success && result.confirmationUrl.isNotEmpty()) {
                lastPaymentId = result.paymentId
                prefs.edit().putString("last_yookassa_payment_id", result.paymentId).apply()
                yooKassaService.openPaymentUrl(result.confirmationUrl)
                _toastEvent.emit("Переход к оплате ЮKassa (СБП/Карта)...")

                // Уведомление в Telegram
                com.example.data.notification.TelegramNotifier.notifyPaymentStarted(
                    callsign = callsign,
                    email = email,
                    amountRub = yooKassaService.getConfig().priceRubles
                )

                // Фоновый опрос для автоматической выдачи ключа после возврата
                paymentPollingJob?.cancel()
                paymentPollingJob = viewModelScope.launch {
                    var attempts = 0
                    while (attempts < 60) {
                        kotlinx.coroutines.delay(3500)
                        attempts++
                        val verification = yooKassaService.verifyPaymentLicense(
                            paymentId = result.paymentId,
                            fighterId = fighterId
                        )
                        if (verification.success && verification.paid) {
                            confirmPaymentAndActivateLicense()
                            break
                        }
                    }
                }
            } else {
                _toastEvent.emit(result.errorMessage ?: "Не удалось создать счет ЮKassa")
            }
        }
    }

    fun confirmPaymentAndActivateLicense() {
        if (paymentActivationInProgress) return

        viewModelScope.launch {
            paymentActivationInProgress = true
            try {
                paymentPollingJob?.cancel()

                val profile = userProfile.filterNotNull().first()
                val callsign = profile.callsign.ifBlank { "Боец" }
                val email = profile.email.trim()
                val unitName = profile.unitName.ifBlank { "Подразделение" }
                val unitKey = profile.unitKey.trim()

                val paymentIdToVerify = if (lastPaymentId.isNotBlank()) {
                    lastPaymentId
                } else {
                    prefs.getString("last_yookassa_payment_id", "") ?: ""
                }

                if (paymentIdToVerify.isBlank()) {
                    _toastEvent.emit("Счёт на оплату ещё не был сформирован. Сначала нажмите «Оплатить через ЮKassa / СБП».")
                    return@launch
                }

                val alreadyActivatedPaymentId = prefs.getString("last_activated_yookassa_payment_id", "") ?: ""
                if (alreadyActivatedPaymentId == paymentIdToVerify) {
                    licenseManager.refreshLicenseStatus()
                    _toastEvent.emit("Лицензия по этому платежу уже активирована.")
                    return@launch
                }

                _toastEvent.emit("Проверка статуса оплаты в ЮKassa...")

                val verification = yooKassaService.verifyPaymentLicense(
                    paymentId = paymentIdToVerify,
                    fighterId = licenseManager.getFighterPersonalId()
                )
                if (!verification.success || !verification.paid) {
                    val msg = verification.errorMessage ?: "Платёж ещё не подтверждён сервером."
                    _toastEvent.emit("❌ ЛИЦЕНЗИЯ НЕ АКТИВИРОВАНА!\n$msg")
                    return@launch
                }

                val newKey = verification.licenseKey
                val activated = licenseManager.activateServerVerifiedLicense(
                    licenseKey = newKey,
                    expiresAt = verification.expiresAt,
                    paymentId = paymentIdToVerify
                )
                if (!activated) {
                    _toastEvent.emit("❌ Сервер подтвердил оплату, но данные лицензии не прошли проверку.")
                    return@launch
                }

                val daysLeft = ((verification.expiresAt - System.currentTimeMillis()) / 86400000L)
                    .toInt()
                    .coerceAtLeast(1)

                _issuedPaymentKey.value = newKey
                prefs.edit()
                    .putString("last_activated_yookassa_payment_id", paymentIdToVerify)
                    .apply()

                // Локальный профиль отражает только уже подтверждённую сервером лицензию.
                repository.saveUserProfile(
                    profile.copy(isProActive = true, proDaysLeft = daysLeft, demoDaysLeft = 0)
                )

                // Заносим пользователя в реестр без фиктивного unit key.
                fighterRegistryManager.registerOrUpdateFighter(
                    fighterId = licenseManager.getFighterPersonalId(),
                    callsign = callsign,
                    unitName = unitName,
                    unitKey = unitKey,
                    email = email,
                    licenseKey = newKey,
                    isProActive = true,
                    expiresAt = verification.expiresAt
                )

                com.example.data.notification.TelegramNotifier.notifyPaymentConfirmed(
                    callsign = callsign,
                    email = email,
                    licenseKey = newKey,
                    days = daysLeft
                )

                if (email.isNotBlank()) {
                    com.example.data.notification.EmailDeliveryService.sendLicenseKeyEmail(
                        context = getApplication(),
                        recipientEmail = email,
                        callsign = callsign,
                        licenseKey = newKey,
                        days = daysLeft
                    )
                }

                _toastEvent.emit("🎉 Оплата подтверждена! Лицензия PRO активирована на 30 дней.")
            } finally {
                paymentActivationInProgress = false
            }
        }
    }

    fun resendLicenseKeyToEmail(customEmail: String? = null) {
        viewModelScope.launch {
            val profile = userProfile.value
            val email = customEmail?.ifBlank { null } ?: profile?.email ?: ""
            val key = _issuedPaymentKey.value ?: licenseStatus.value.licenseKey.ifEmpty { licenseStatus.value.lastSavedKey }
            val callsign = profile?.callsign ?: "Боец"
            if (email.isNotBlank() && key.isNotBlank()) {
                val daysLeft = licenseStatus.value.daysRemaining.coerceAtLeast(1)
                val sent = com.example.data.notification.EmailDeliveryService.sendLicenseKeyEmail(
                    context = getApplication(),
                    recipientEmail = email,
                    callsign = callsign,
                    licenseKey = key,
                    days = daysLeft
                )
                _toastEvent.emit(
                    if (sent) "✉️ Лицензионный ключ отправлен на $email"
                    else "Не удалось отправить письмо. Проверьте Email и повторите позже."
                )
            } else {
                _toastEvent.emit("Укажите email для отправки ключа")
            }
        }
    }

    fun activateLicenseKey(enteredKey: String) {
        viewModelScope.launch {
            val profile = userProfile.value
            val callsign = profile?.callsign ?: "Боец"
            val (success, message) = licenseManager.activateKeyManually(enteredKey, callsign)
            if (success) {
                val curProfile = userProfile.value ?: UserProfile()
                val currentLicense = licenseManager.licenseStatus.value
                val daysLeft = currentLicense.daysRemaining.coerceAtLeast(1)
                val activeKey = currentLicense.licenseKey.ifBlank { enteredKey.trim().uppercase() }

                repository.saveUserProfile(
                    curProfile.copy(
                        isProActive = true,
                        proDaysLeft = daysLeft,
                        demoDaysLeft = 0
                    )
                )
                fighterRegistryManager.registerOrUpdateFighter(
                    fighterId = licenseManager.getFighterPersonalId(),
                    callsign = callsign,
                    unitName = curProfile.unitName,
                    unitKey = curProfile.unitKey,
                    email = curProfile.email,
                    licenseKey = activeKey,
                    isProActive = true,
                    expiresAt = System.currentTimeMillis() + daysLeft.toLong() * 86400000L
                )

                // Уведомление в Telegram
                com.example.data.notification.TelegramNotifier.notifyKeyActivated(
                    callsign = callsign,
                    licenseKey = activeKey,
                    days = daysLeft
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
                val daysLeft = licenseManager.licenseStatus.value.daysRemaining.coerceAtLeast(1)
                repository.saveUserProfile(
                    curProfile.copy(
                        isProActive = true,
                        proDaysLeft = daysLeft,
                        demoDaysLeft = 0
                    )
                )
            }
            _toastEvent.emit(msg)
        }
    }

    /**
     * Сброс / отзыв лицензии (для повторного тестирования оплаты)
     */
    fun resetLicense() {
        viewModelScope.launch {
            paymentPollingJob?.cancel()
            _issuedPaymentKey.value = null
            lastPaymentId = ""
            prefs.edit().remove("last_yookassa_payment_id").apply()
            licenseManager.resetLicense()
            val curProfile = userProfile.value ?: UserProfile()
            repository.saveUserProfile(curProfile.copy(isProActive = false, proDaysLeft = 0))
            licenseManager.refreshLicenseStatus()
            val demoLeft = licenseManager.licenseStatus.value.demoDaysLeft
            _toastEvent.emit(
                if (demoLeft > 0) "Лицензия сброшена. Остаток демо-периода: $demoLeft дн."
                else "Лицензия сброшена. Демо-период уже завершён."
            )
        }
    }

    // --- DEVELOPER BACKDOOR ACTIONS ---

    suspend fun authenticateDeveloper(secret: String): Pair<Boolean, String> {
        val result = adminBackendService.authenticate(secret)
        adminSessionToken = if (result.success) result.token else ""
        if (result.success) {
            val registry = adminBackendService.listFighters(adminSessionToken)
            if (registry.success) {
                fighterRegistryManager.replaceCachedFighters(registry.fighters)
            }
        }
        return Pair(
            result.success,
            if (result.success) "Доступ подтверждён сервером." else result.errorMessage
        )
    }

    fun deleteFighterFromRegistry(fighterId: String) {
        viewModelScope.launch {
            val result = adminBackendService.deleteFighter(adminSessionToken, fighterId)
            if (result.success) {
                fighterRegistryManager.removeCachedFighter(fighterId)
                _toastEvent.emit("Запись пользователя удалена из реестра. Лицензии и складские данные сохранены.")
            } else {
                if (result.errorMessage.contains("сессия", ignoreCase = true)) adminSessionToken = ""
                _toastEvent.emit(result.errorMessage)
            }
        }
    }

    fun grantLicenseFromDevMenu(fighterId: String, days: Int = 30) {
        viewModelScope.launch {
            val result = adminBackendService.grantLicense(adminSessionToken, fighterId, days)
            if (result.success) {
                val registry = adminBackendService.listFighters(adminSessionToken)
                if (registry.success) {
                    fighterRegistryManager.replaceCachedFighters(registry.fighters)
                }
                _toastEvent.emit("Лицензия обновлена: ${result.licenseKey} (+$days дн.)")
            } else {
                if (result.errorMessage.contains("сессия", ignoreCase = true)) adminSessionToken = ""
                _toastEvent.emit(result.errorMessage)
            }
        }
    }

    fun refreshFightersRegistry() {
        viewModelScope.launch {
            val result = adminBackendService.listFighters(adminSessionToken)
            if (result.success) {
                fighterRegistryManager.replaceCachedFighters(result.fighters)
                _toastEvent.emit("Реестр бойцов обновлен")
            } else {
                if (result.errorMessage.contains("сессия", ignoreCase = true)) {
                    adminSessionToken = ""
                }
                _toastEvent.emit(result.errorMessage)
            }
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
