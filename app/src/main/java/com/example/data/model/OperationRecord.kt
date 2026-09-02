package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OperationType(val titleRu: String, val code: String) {
    INCOME("Привезли", "income"),
    TRANSFER("Перемещение", "transfer"),
    ISSUE("Подняли", "issue"),
    EXPENDITURE("Расход (ф. 8)", "expenditure")
}

data class OperationItemEntry(
    val itemId: String,
    val itemName: String,
    val unit: String,
    val quantity: Int,
    val categoryClass: String = "Кат. 1",
    val reason: String = "" // Причина расхода (для ф.8: "Боевая работа", "Пристрелка", etc.)
)

@Entity(tableName = "operation_records")
data class OperationRecord(
    @PrimaryKey val id: String,
    val type: OperationType,
    val fromPointName: String,
    val toPointName: String,
    val docNumber: String = "",       // № Акта / Формуляра
    val responsiblePerson: String = "", // Ответственный
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val itemsSummary: String = "",     // e.g. "Мина 120-мм — 24 шт., ВОГ-17 — 50 шт."
    val itemsJson: String = ""         // Serialized list
)
