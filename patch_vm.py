import re

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'r') as f:
    content = f.read()

target = """    fun recordIssue(
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
            _toastEvent.emit("Имущество выдано («Подняли»)")
        }
    }"""

replacement = """    fun recordIssue(
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
    }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'w') as f:
    f.write(content)
