package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OperationType(val titleRu: String, val code: String) {
    INCOME("Поступление", "income"),
    TRANSFER("Перемещение", "transfer"),
    ISSUE("Выдача", "issue"),
    EXPENDITURE("Списание", "expenditure"),
    CORRECTION("Корректировка", "correction")
}

data class OperationItemEntry(
    val itemId: String,
    val itemName: String,
    val unit: String,
    val quantity: Int,
    val categoryClass: String = "Кат. 1",
    val reason: String = "" // Причина списания / расхода
)

@Entity(tableName = "operation_records")
data class OperationRecord(
    @PrimaryKey val id: String,
    val type: OperationType,
    val fromPointName: String,
    val toPointName: String,
    val docNumber: String = "",       // Документ / номер акта
    val responsiblePerson: String = "", // Ответственное лицо
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val itemsSummary: String = "",     // Краткий список позиций
    val itemsJson: String = "",        // Serialized list
    val fromPointId: String = "",      // Stable warehouse identity for new records
    val toPointId: String = ""         // Stable warehouse identity for new records
)
