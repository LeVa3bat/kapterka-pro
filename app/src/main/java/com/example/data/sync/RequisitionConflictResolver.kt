package com.example.data.sync

import com.example.data.model.RequisitionRequest

/**
 * Deterministic conflict resolution rule for cross-device requisition sync.
 *
 * Ensures that:
 * 1. Brand-new requisitions created on another phone are always accepted.
 * 2. When explicit [cloudUpdatedAt] / [localUpdatedAt] timestamps are available,
 *    the newer revision wins (allowing intentional status rollbacks via "Назад").
 * 3. When offline changes lack a newer cloud revision (or an older client does not
 *    write `updatedAt`), forward pipeline progress (PENDING -> ASSEMBLING ->
 *    COLLECTED -> ISSUED) is never silently reverted by a stale cloud snapshot.
 */
object RequisitionConflictResolver {
    fun cloudMayReplaceLocal(
        local: RequisitionRequest?,
        cloud: RequisitionRequest,
        localUpdatedAt: Long = 0L,
        cloudUpdatedAt: Long = cloud.timestamp
    ): Boolean {
        if (local == null) return true

        val effectiveCloudTime = maxOf(cloudUpdatedAt, cloud.timestamp)
        val effectiveLocalTime = maxOf(localUpdatedAt, local.timestamp)

        // Explicit newer local status update (e.g. made offline in this session) beats older cloud state.
        if (localUpdatedAt > 0L && effectiveCloudTime < localUpdatedAt) {
            return false
        }

        // Explicit newer cloud edit (from another phone after local creation/edit) wins,
        // including intentional step-back transitions.
        if (cloudUpdatedAt > cloud.timestamp && effectiveCloudTime > effectiveLocalTime) {
            return true
        }

        // Fallback when neither side has a newer explicit status timestamp:
        // preserve forward workflow progress instead of reverting to an older stage.
        if (cloud.status.step != local.status.step) {
            return cloud.status.step > local.status.step
        }

        return effectiveCloudTime >= effectiveLocalTime
    }
}
