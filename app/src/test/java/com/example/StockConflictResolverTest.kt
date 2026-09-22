package com.example

import com.example.data.model.StockRecord
import com.example.data.sync.StockConflictResolver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockConflictResolverTest {

    @Test
    fun cloudReplacesWhenLocalMissing() {
        val cloud = StockRecord("p", "i", quantity = 10, lastUpdated = 100L)
        assertTrue(StockConflictResolver.cloudMayReplaceLocal(null, cloud))
    }

    @Test
    fun newerCloudReplacesLocal() {
        val local = StockRecord("p", "i", quantity = 5, lastUpdated = 100L)
        val cloud = StockRecord("p", "i", quantity = 10, lastUpdated = 101L)
        assertTrue(StockConflictResolver.cloudMayReplaceLocal(local, cloud))
    }

    @Test
    fun equalTimestampIsIdempotent() {
        val local = StockRecord("p", "i", quantity = 5, lastUpdated = 100L)
        val cloud = StockRecord("p", "i", quantity = 5, lastUpdated = 100L)
        assertTrue(StockConflictResolver.cloudMayReplaceLocal(local, cloud))
    }

    @Test
    fun olderCloudCannotOverwriteOfflineLocalEdit() {
        val local = StockRecord("p", "i", quantity = 12, lastUpdated = 200L)
        val cloud = StockRecord("p", "i", quantity = 8, lastUpdated = 100L)
        assertFalse(StockConflictResolver.cloudMayReplaceLocal(local, cloud))
    }
}
