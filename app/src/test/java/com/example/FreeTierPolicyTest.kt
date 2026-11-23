package com.example

import com.example.data.license.FreeTierDecision
import com.example.data.license.FreeTierPolicy
import com.example.data.model.InventoryItem
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeTierPolicyTest {

    private val points = listOf(
        WarehousePoint(id = "base_sklad", name = "Базовый склад", isBase = true, orderIndex = 0),
        WarehousePoint(id = "point_1", name = "Цех № 1", isBase = false, orderIndex = 1)
    )

    private fun makeStock(pointId: String, count: Int): List<StockRecord> =
        (1..count).map { idx ->
            StockRecord(pointId = pointId, itemId = "item_$idx", quantity = 3)
        }

    @Test
    fun proOrDemo_allowsAdditionalPointsAndMoreThan15Items() {
        val stock = makeStock("base_sklad", 18)
        val income = FreeTierPolicy.checkIncome(
            isProOrDemo = true,
            points = points,
            stockRecords = stock,
            targetPointId = "point_1",
            itemIds = listOf("item_19", "item_20")
        )
        assertEquals(FreeTierDecision.Allowed, income)

        val issue = FreeTierPolicy.checkIssue(
            isProOrDemo = true,
            points = points,
            fromPointId = "point_1",
            toPointId = "base_sklad"
        )
        assertEquals(FreeTierDecision.Allowed, issue)
    }

    @Test
    fun freeMode_allowsIncomeOnPrimaryWarehouseWithin15ActiveItems() {
        val stock = makeStock("base_sklad", 14) + StockRecord(pointId = "base_sklad", itemId = "zero_item", quantity = 0)
        val res = FreeTierPolicy.checkIncome(
            isProOrDemo = false,
            points = points,
            stockRecords = stock,
            targetPointId = "base_sklad",
            itemIds = listOf("item_15")
        )
        assertEquals(FreeTierDecision.Allowed, res)
    }

    @Test
    fun freeMode_blocksIncomeOnNonPrimaryWarehouseOrOver15Items() {
        val secondary = FreeTierPolicy.checkIncome(
            isProOrDemo = false,
            points = points,
            stockRecords = emptyList(),
            targetPointId = "point_1",
            itemIds = listOf("item_1")
        )
        assertTrue(secondary is FreeTierDecision.RequirePro)

        val stock = makeStock("base_sklad", 15)
        val overLimit = FreeTierPolicy.checkIncome(
            isProOrDemo = false,
            points = points,
            stockRecords = stock,
            targetPointId = "base_sklad",
            itemIds = listOf("item_16")
        )
        assertTrue(overLimit is FreeTierDecision.RequirePro)
    }

    @Test
    fun freeMode_issueAllowsPrimaryPersonOnlyAndBlocksPointTransfer() {
        val ok = FreeTierPolicy.checkIssue(
            isProOrDemo = false,
            points = points,
            fromPointId = "base_sklad",
            toPointId = ""
        )
        assertEquals(FreeTierDecision.Allowed, ok)

        val blockedTarget = FreeTierPolicy.checkIssue(
            isProOrDemo = false,
            points = points,
            fromPointId = "base_sklad",
            toPointId = "point_1"
        )
        assertTrue(blockedTarget is FreeTierDecision.RequirePro)

        val blockedSource = FreeTierPolicy.checkIssue(
            isProOrDemo = false,
            points = points,
            fromPointId = "point_1",
            toPointId = ""
        )
        assertTrue(blockedSource is FreeTierDecision.RequirePro)
    }

    @Test
    fun freeMode_stockAdjustAllowsDecreaseAndBlocksOverLimitIncrease() {
        val stock = makeStock("base_sklad", 15)
        val decrease = FreeTierPolicy.checkStockAdjust(
            isProOrDemo = false,
            points = points,
            stockRecords = stock,
            pointId = "base_sklad",
            itemId = "item_1",
            newQuantity = 0
        )
        assertEquals(FreeTierDecision.Allowed, decrease)

        val increaseNew = FreeTierPolicy.checkStockAdjust(
            isProOrDemo = false,
            points = points,
            stockRecords = stock,
            pointId = "base_sklad",
            itemId = "item_16",
            newQuantity = 2
        )
        assertTrue(increaseNew is FreeTierDecision.RequirePro)

        val wrongWarehouse = FreeTierPolicy.checkStockAdjust(
            isProOrDemo = false,
            points = points,
            stockRecords = stock,
            pointId = "point_1",
            itemId = "item_1",
            newQuantity = 2
        )
        assertTrue(wrongWarehouse is FreeTierDecision.RequirePro)
    }

    @Test
    fun freeMode_customItemsEnforceLimitOf15IgnoringDefaultCatalog() {
        val defaultItems = (1..30).map { idx ->
            InventoryItem("def_$idx", "Штатная $idx", "Кат", "Подтип", "шт", isCustom = false)
        }
        val customItems = (1..15).map { idx ->
            InventoryItem("cust_$idx", "Своя $idx", "Кат", "Подтип", "шт", isCustom = true)
        }
        val underLimit = FreeTierPolicy.checkCustomItemAdd(
            isProOrDemo = false,
            catalogItems = defaultItems + customItems.dropLast(1),
            defaultItemIds = defaultItems.map { it.id }.toSet()
        )
        assertEquals(FreeTierDecision.Allowed, underLimit)

        val atLimit = FreeTierPolicy.checkCustomItemAdd(
            isProOrDemo = false,
            catalogItems = defaultItems + customItems,
            defaultItemIds = defaultItems.map { it.id }.toSet()
        )
        assertTrue(atLimit is FreeTierDecision.RequirePro)
    }

    @Test
    fun freeMode_secondaryWarehouseItemsDoNotDeadlockPrimaryWarehouse() {
        val secondaryOnlyStock = makeStock("point_1", 15)
        val incomeOnPrimary = FreeTierPolicy.checkIncome(
            isProOrDemo = false,
            points = points,
            stockRecords = secondaryOnlyStock,
            targetPointId = "base_sklad",
            itemIds = listOf("item_16")
        )
        assertEquals(FreeTierDecision.Allowed, incomeOnPrimary)
    }
}
