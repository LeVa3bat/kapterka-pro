package com.example.data.local

import com.example.data.model.InventoryItem
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog

object InitialData {
    fun getDefaultItems(): List<InventoryItem> = emptyList()

    fun getDefaultPoints(): List<WarehousePoint> {
        return listOf(
            WarehousePoint(
                id = "main_warehouse",
                name = "Основной склад",
                description = "Главное место хранения",
                isBase = true
            )
        )
    }

    fun getDefaultCategories(): List<String> =
        WarehouseProfileCatalog.find("universal").categories
}
