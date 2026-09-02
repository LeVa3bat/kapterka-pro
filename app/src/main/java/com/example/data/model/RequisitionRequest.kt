package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RequestStatus(val titleRu: String) {
    PENDING("В обработке"),
    COLLECTED("Собрана"),
    ISSUED("Выдана")
}

data class RequisitionItemEntry(
    val itemName: String,
    val quantity: Int,
    val unit: String
)

@Entity(tableName = "requisitions")
data class RequisitionRequest(
    @PrimaryKey val id: String,
    val pointName: String,
    val applicantName: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val itemsSummary: String = "",
    val itemsJson: String = ""
)
