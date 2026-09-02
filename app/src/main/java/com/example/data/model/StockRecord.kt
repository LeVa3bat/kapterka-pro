package com.example.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "stock_records",
    primaryKeys = ["pointId", "itemId"],
    indices = [Index(value = ["pointId"]), Index(value = ["itemId"])]
)
data class StockRecord(
    val pointId: String,
    val itemId: String,
    val quantity: Int = 0,
    val incomeTotal: Int = 0,
    val expenseTotal: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ItemWithStock(
    val item: InventoryItem,
    val quantity: Int,
    val incomeTotal: Int,
    val expenseTotal: Int
)
