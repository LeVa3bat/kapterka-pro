package com.example

import com.example.data.sync.SyncIdentityGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncIdentityGeneratorTest {

    @Test
    fun newUnitKeysAreLongAndUnique() {
        val keys = (1..200).map { SyncIdentityGenerator.newUnitKey() }
        assertEquals(200, keys.toSet().size)
        assertTrue(keys.all { it.matches(Regex("^kapt_[0-9a-f]{20}$")) })
    }

    @Test
    fun newDeviceIdsAreLongAndUnique() {
        val ids = (1..200).map { SyncIdentityGenerator.newDeviceId() }
        assertEquals(200, ids.toSet().size)
        assertTrue(ids.all { it.matches(Regex("^dev_[0-9a-f]{16}$")) })
    }
}
