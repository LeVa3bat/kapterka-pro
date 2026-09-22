package com.example

import com.example.data.model.SyncTombstone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTombstoneTest {

    @Test
    fun newerStockRevisionSupersedesOlderDeletion() {
        val tombstone = SyncTombstone.create(
            unitKey = "kapt_test",
            entityType = "stock_record",
            entityId = "base_sklad:::item-1",
            deletedAt = 1000L
        )

        assertTrue(tombstone.isSupersededBy(1001L))
        assertFalse(tombstone.isSupersededBy(1000L))
        assertFalse(tombstone.isSupersededBy(999L))
    }
}
