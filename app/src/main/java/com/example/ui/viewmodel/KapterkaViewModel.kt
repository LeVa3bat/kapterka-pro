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
    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            _toastEvent.emit("Настройки профиля сохранены")
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
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
