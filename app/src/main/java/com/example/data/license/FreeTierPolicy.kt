package com.example.data.license

import com.example.data.model.InventoryItem
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint

sealed class FreeTierDecision {
    object Allowed : FreeTierDecision()
    data class RequirePro(val reason: String) : FreeTierDecision()
}

/**
 * Limits applied in free offline mode after the 72-hour PRO trial ends:
 * - Only 1 primary warehouse point is writable
 * - Up to 15 active stock items (quantity > 0)
 * - Up to 15 custom catalog cards
 * - Issue is allowed to recipients on hand only (not inter-warehouse transfers)
 */
object FreeTierPolicy {
    const val MAX_FREE_ACTIVE_ITEMS = 15
    const val MAX_FREE_CUSTOM_ITEMS = 15

    fun primaryPointId(points: List<WarehousePoint>): String? {
        return points.firstOrNull { it.isBase }?.id
            ?: points.minByOrNull { it.orderIndex }?.id
            ?: points.firstOrNull()?.id
    }

    fun activeItemIds(stockRecords: List<StockRecord>): Set<String> {
        return stockRecords.asSequence()
            .filter { it.quantity > 0 }
            .map { it.itemId }
            .toSet()
    }

    fun customItemCount(
        catalogItems: List<InventoryItem>,
        defaultItemIds: Set<String> = emptySet()
    ): Int {
        return catalogItems.count { item ->
            item.isCustom || (defaultItemIds.isNotEmpty() && item.id !in defaultItemIds)
        }
    }

    fun checkIncome(
        isProOrDemo: Boolean,
        points: List<WarehousePoint>,
        stockRecords: List<StockRecord>,
        targetPointId: String,
        itemIds: List<String>
    ): FreeTierDecision {
        if (isProOrDemo) return FreeTierDecision.Allowed

        val primaryId = primaryPointId(points)
        if (primaryId != null && targetPointId != primaryId) {
            return FreeTierDecision.RequirePro(
                "В бесплатном режиме приход доступен только на основной склад. Остальные точки — в PRO."
            )
        }

        val currentActive = activeItemIds(stockRecords)
        val resultingActiveCount = (currentActive + itemIds).size
        if (resultingActiveCount > MAX_FREE_ACTIVE_ITEMS) {
            return FreeTierDecision.RequirePro(
                "Бесплатный лимит: до $MAX_FREE_ACTIVE_ITEMS активных позиций на остатках (сейчас получится $resultingActiveCount). Откройте PRO для безлимитного учёта."
            )
        }

        return FreeTierDecision.Allowed
    }

    fun checkIssue(
        isProOrDemo: Boolean,
        points: List<WarehousePoint>,
        fromPointId: String,
        toPointId: String
    ): FreeTierDecision {
        if (isProOrDemo) return FreeTierDecision.Allowed

        if (toPointId.isNotBlank()) {
            return FreeTierDecision.RequirePro(
                "Передача на другую точку доступна в PRO. В бесплатном режиме доступна только выдача на руки."
            )
        }

        val primaryId = primaryPointId(points)
        if (primaryId != null && fromPointId != primaryId) {
            return FreeTierDecision.RequirePro(
                "В бесплатном режиме выдача доступна только с основного склада. Остальные точки — в PRO."
            )
        }

        return FreeTierDecision.Allowed
    }

    fun checkStockAdjust(
        isProOrDemo: Boolean,
        points: List<WarehousePoint>,
        stockRecords: List<StockRecord>,
        pointId: String,
        itemId: String,
        newQuantity: Int
    ): FreeTierDecision {
        if (isProOrDemo) return FreeTierDecision.Allowed

        val primaryId = primaryPointId(points)
        if (primaryId != null && pointId != primaryId) {
            return FreeTierDecision.RequirePro(
                "В бесплатном режиме редактирование остатков доступно только на основном складе."
            )
        }

        val currentQty = stockRecords.firstOrNull { it.pointId == pointId && it.itemId == itemId }?.quantity ?: 0
        val currentActive = activeItemIds(stockRecords)
        val resultingActiveCount = if (newQuantity > 0) {
            (currentActive + itemId).size
        } else {
            (currentActive - itemId).size
        }

        if (newQuantity > currentQty && resultingActiveCount > MAX_FREE_ACTIVE_ITEMS) {
            return FreeTierDecision.RequirePro(
                "Бесплатный лимит: до $MAX_FREE_ACTIVE_ITEMS активных позиций на остатках. Откройте PRO для большего списка."
            )
        }

        return FreeTierDecision.Allowed
    }

    fun checkCustomItemAdd(
        isProOrDemo: Boolean,
        catalogItems: List<InventoryItem>,
        defaultItemIds: Set<String> = emptySet()
    ): FreeTierDecision {
        if (isProOrDemo) return FreeTierDecision.Allowed

        val count = customItemCount(catalogItems, defaultItemIds)
        if (count >= MAX_FREE_CUSTOM_ITEMS) {
            return FreeTierDecision.RequirePro(
                "Бесплатный лимит: до $MAX_FREE_CUSTOM_ITEMS своих позиций в справочнике. Откройте PRO для безлимита."
            )
        }

        return FreeTierDecision.Allowed
    }
}
