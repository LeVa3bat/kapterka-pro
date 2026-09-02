package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val serviceCategory: String, // e.g. "Служба РАВ", "Служба БПЛА и робототехники", etc.
    val subType: String,         // e.g. "Мины 120-мм", "Дроны разведки"
    val unit: String,            // e.g. "шт.", "компл.", "ящ.", "л."
    val categoryClass: String = "Кат. 1", // e.g. "Кат. 1", "Кат. 2"
    val standardCode: String = "",        // e.g. "3ВОФ34"
    val isCustom: Boolean = false
)
