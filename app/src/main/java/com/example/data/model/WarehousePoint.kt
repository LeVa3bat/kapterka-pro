package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warehouse_points")
data class WarehousePoint(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val isBase: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
