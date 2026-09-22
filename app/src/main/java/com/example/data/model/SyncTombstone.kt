package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Explicit deletion marker for multi-device sync.
 *
 * Absence in Firestore is never treated as deletion. A record is removed on
 * another device only after an explicit tombstone is observed.
 */
@Entity(tableName = "sync_tombstones")
data class SyncTombstone(
    @PrimaryKey val id: String,
    val unitKey: String,
    val entityType: String,
    val entityId: String,
    val deletedAt: Long
) {
    companion object {
        fun create(
            unitKey: String,
            entityType: String,
            entityId: String,
            deletedAt: Long = System.currentTimeMillis()
        ): SyncTombstone {
            val cleanType = entityType.trim().lowercase()
            return SyncTombstone(
                id = "$unitKey|$cleanType|$entityId",
                unitKey = unitKey,
                entityType = cleanType,
                entityId = entityId,
                deletedAt = deletedAt
            )
        }
    }
}
