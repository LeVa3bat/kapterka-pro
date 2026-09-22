package com.example.data.sync

import com.example.data.model.StockRecord

/**
 * Deterministic last-write-wins rule for stock synchronization.
 *
 * A cloud stock snapshot may replace local stock only when it is at least as
 * recent as the local row. A newer local row must be pushed back to cloud
 * instead of being silently overwritten after offline work.
 */
object StockConflictResolver {
    fun cloudMayReplaceLocal(
        local: StockRecord?,
        cloud: StockRecord
    ): Boolean = local == null || cloud.lastUpdated >= local.lastUpdated
}
