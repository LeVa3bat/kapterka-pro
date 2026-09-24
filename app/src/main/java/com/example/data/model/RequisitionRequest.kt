package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stored by name. ASSEMBLING was added in 3.6 and is declared last so older
 * data keeps its meaning; older app versions read an unknown status as PENDING.
 * [step] is the position in the pipeline.
 */
enum class RequestStatus(val titleRu: String, val step: Int) {
    PENDING("Новая", 0),
    COLLECTED("Собрана", 2),
    ISSUED("Выдана", 3),
    ASSEMBLING("Собирается", 1);

    companion object {
        val pipeline: List<RequestStatus> = values().sortedBy { it.step }
    }
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
