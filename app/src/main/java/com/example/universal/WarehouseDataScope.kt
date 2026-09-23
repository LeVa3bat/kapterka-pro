package com.example.universal

import com.example.data.model.InventoryItem
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint

/**
 * Single source of truth for what data belongs to the currently selected
 * physical warehouse in Sklad PRO.
 */
object WarehouseDataScope {

    fun profileId(
        warehouse: WarehousePoint?,
        fallbackProfileId: String? = null
    ): String = WarehouseProfileCatalog.normalizeId(
        warehouse?.profileId?.takeIf { it.isNotBlank() } ?: fallbackProfileId
    )

    fun catalogFor(
        items: List<InventoryItem>,
        warehouse: WarehousePoint?,
        fallbackProfileId: String? = null
    ): List<InventoryItem> {
        if (warehouse == null) return emptyList()
        val profile = profileId(warehouse, fallbackProfileId)

        return items.filter { item ->
            when {
                item.isCustom ->
                    item.warehouseId == warehouse.id &&
                        WarehouseProfileCatalog.normalizeId(item.profileId) == profile
                else ->
                    item.warehouseId.isBlank() &&
                        item.profileId == profile
            }
        }
    }

    fun stockFor(
        stocks: List<StockRecord>,
        warehouse: WarehousePoint?,
        allowedItemIds: Set<String>
    ): List<StockRecord> {
        if (warehouse == null) return emptyList()
        return stocks.filter { stock ->
            stock.pointId == warehouse.id && stock.itemId in allowedItemIds
        }
    }

    fun compatibleWarehouses(
        points: List<WarehousePoint>,
        activeWarehouse: WarehousePoint?,
        fallbackProfileId: String? = null
    ): List<WarehousePoint> {
        if (activeWarehouse == null) return emptyList()
        val profile = profileId(activeWarehouse, fallbackProfileId)
        return points.filter { point ->
            WarehouseProfileCatalog.normalizeId(point.profileId) == profile
        }
    }
}
