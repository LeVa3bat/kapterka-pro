package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.InitialData
import com.example.data.local.KapterkaDao
import com.example.data.local.KapterkaDatabase
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.OperationItemEntry
import com.example.data.model.StockRecord
import com.example.data.model.SyncTombstone
import com.example.data.model.WarehousePoint
import com.example.data.repository.KapterkaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KapterkaDatabaseTest {

    private lateinit var db: KapterkaDatabase
    private lateinit var dao: KapterkaDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KapterkaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.kapterkaDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveWarehousePoints() = runBlocking {
        val point1 = WarehousePoint("test_base", "Базовый склад 1", "Главный склад", true, 0)
        val point2 = WarehousePoint("test_vop", "ВОП Южный", "Передовая позиция", false, 1)

        dao.insertPoints(listOf(point1, point2))

        val points = dao.getAllPoints().first()
        assertEquals(2, points.size)
        assertEquals("test_base", points[0].id)
        assertTrue(points[0].isBase)
        assertEquals("test_vop", points[1].id)
        assertFalse(points[1].isBase)
    }

    @Test
    fun testInsertAndQueryInventoryItems() = runBlocking {
        val items = listOf(
            InventoryItem("item_1", "АК-74М 5.45мм", "Служба РАВ", "Стрелковое оружие", "шт.", "Кат. 1"),
            InventoryItem("item_2", "Патроны 5.45х39", "Служба РАВ", "Боеприпасы", "цинк", "Кат. 1"),
            InventoryItem("item_3", "Сухпаек ИРП-П", "Вещевая и продслужба", "Продовольствие", "компл.", "Кат. 1")
        )

        dao.insertItems(items)

        val allItems = dao.getAllItems().first()
        assertEquals(3, allItems.size)

        val ravItems = dao.getItemsByCategory("Служба РАВ").first()
        assertEquals(2, ravItems.size)
        assertTrue(ravItems.all { it.serviceCategory == "Служба РАВ" })
    }

    @Test
    fun testStockRecordInsertAndStockQuantityUpdate() = runBlocking {
        val pointId = "base_sklad"
        val itemId = "rav_mina_120"

        // Insert initial stock
        val initialStock = StockRecord(
            pointId = pointId,
            itemId = itemId,
            quantity = 50,
            incomeTotal = 50,
            expenseTotal = 0
        )
        dao.insertOrUpdateStock(initialStock)

        var record = dao.getStockItem(pointId, itemId)
        assertNotNull(record)
        assertEquals(50, record!!.quantity)

        // Simulate delivery/receipt (+20)
        val updatedStock = record.copy(quantity = 70, incomeTotal = 70)
        dao.insertOrUpdateStock(updatedStock)

        record = dao.getStockItem(pointId, itemId)
        assertNotNull(record)
        assertEquals(70, record!!.quantity)

        // Simulate expenditure (-15)
        val expendedStock = record.copy(quantity = 55, expenseTotal = 15)
        dao.insertOrUpdateStock(expendedStock)

        record = dao.getStockItem(pointId, itemId)
        assertNotNull(record)
        assertEquals(55, record!!.quantity)
        assertEquals(15, record!!.expenseTotal)
    }

    @Test
    fun testOperationAuditHistory() = runBlocking {
        val op = OperationRecord(
            id = "op_test_123",
            type = OperationType.EXPENDITURE,
            fromPointName = "Базовый склад",
            toPointName = "Расход (ф. 8)",
            docNumber = "Акт № 8-12",
            responsiblePerson = "старшина Иванов",
            comment = "Боевые стрельбы",
            timestamp = System.currentTimeMillis(),
            itemsSummary = "Мина 120-мм — 10 шт."
        )

        dao.insertOperation(op)

        val ops = dao.getAllOperations().first()
        assertEquals(1, ops.size)
        assertEquals("op_test_123", ops[0].id)
        assertEquals(OperationType.EXPENDITURE, ops[0].type)
        assertEquals("Акт № 8-12", ops[0].docNumber)
        assertEquals("Мина 120-мм — 10 шт.", ops[0].itemsSummary)
    }

    @Test
    fun testOperationAndStocksCommitTogether() = runBlocking {
        val op = OperationRecord(
            id = "op_atomic_1",
            type = OperationType.INCOME,
            fromPointName = "Снабжение",
            toPointName = "Базовый склад",
            docNumber = "",
            responsiblePerson = "Тест",
            comment = "atomic",
            timestamp = System.currentTimeMillis(),
            itemsSummary = "Тест — 7 шт."
        )
        val stock = StockRecord(
            pointId = "base_sklad",
            itemId = "atomic_item",
            quantity = 7,
            incomeTotal = 7,
            expenseTotal = 0
        )

        dao.commitOperationAndStocks(op, listOf(stock))

        val operations = dao.getAllOperations().first()
        val savedStock = dao.getStockItem("base_sklad", "atomic_item")
        assertTrue(operations.any { it.id == "op_atomic_1" })
        assertNotNull(savedStock)
        assertEquals(7, savedStock!!.quantity)
    }

    @Test
    fun testDuplicateIncomeItemsAreAccumulatedBeforeAtomicCommit() = runBlocking {
        val repository = KapterkaRepository(dao, null)
        val repeated = listOf(
            OperationItemEntry("dup_item", "Повтор", "шт.", 3),
            OperationItemEntry("dup_item", "Повтор", "шт.", 4)
        )

        repository.recordIncome(
            toPointId = "base_sklad",
            toPointName = "Базовый склад",
            supplier = "Снабжение",
            items = repeated,
            comment = "duplicate aggregation",
            actor = "Тест"
        )

        val stock = dao.getStockItem("base_sklad", "dup_item")
        assertNotNull(stock)
        assertEquals(7, stock!!.quantity)
        assertEquals(7, stock.incomeTotal)
        assertEquals(1, dao.getAllOperations().first().size)
    }

    @Test
    fun testDuplicateTransferItemsAccumulateOnBothSides() = runBlocking {
        val repository = KapterkaRepository(dao, null)
        dao.insertOrUpdateStock(
            StockRecord(
                pointId = "from_point",
                itemId = "transfer_item",
                quantity = 20,
                incomeTotal = 20,
                expenseTotal = 0
            )
        )
        dao.insertOrUpdateStock(
            StockRecord(
                pointId = "to_point",
                itemId = "transfer_item",
                quantity = 5,
                incomeTotal = 5,
                expenseTotal = 0
            )
        )

        val repeated = listOf(
            OperationItemEntry("transfer_item", "Перемещение", "шт.", 3),
            OperationItemEntry("transfer_item", "Перемещение", "шт.", 2)
        )

        repository.recordTransfer(
            fromPointId = "from_point",
            fromPointName = "Склад А",
            toPointId = "to_point",
            toPointName = "Склад Б",
            items = repeated,
            comment = "duplicate transfer",
            actor = "Тест"
        )

        val from = dao.getStockItem("from_point", "transfer_item")
        val to = dao.getStockItem("to_point", "transfer_item")
        assertNotNull(from)
        assertNotNull(to)
        assertEquals(15, from!!.quantity)
        assertEquals(5, from.expenseTotal)
        assertEquals(10, to!!.quantity)
        assertEquals(10, to.incomeTotal)
    }

    @Test
    fun testSyncTombstonePersistsDeletionIntent() = runBlocking {
        val tombstone = SyncTombstone.create(
            unitKey = "kapt_test",
            entityType = "inventory_item",
            entityId = "item_deleted",
            deletedAt = 123456789L
        )

        dao.upsertSyncTombstone(tombstone)

        val stored = dao.getSyncTombstoneById(tombstone.id)
        assertNotNull(stored)
        assertEquals("kapt_test", stored!!.unitKey)
        assertEquals("inventory_item", stored.entityType)
        assertEquals("item_deleted", stored.entityId)
        assertEquals(123456789L, stored.deletedAt)
    }

    @Test
    fun testMigration2To3PreservesExistingRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_2_3_test.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Representative pre-existing user data. Migration 2 -> 3 must be add-only.
                        db.execSQL("CREATE TABLE legacy_user_data (id TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
                        db.execSQL("INSERT INTO legacy_user_data(id, payload) VALUES ('row-1', 'must-survive')")
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )

        val sqlite = helper.writableDatabase
        KapterkaDatabase.MIGRATION_2_3.migrate(sqlite)

        sqlite.query("SELECT payload FROM legacy_user_data WHERE id='row-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("must-survive", cursor.getString(0))
        }

        sqlite.query("SELECT name FROM sqlite_master WHERE type='table' AND name='sync_tombstones'").use { cursor ->
            assertTrue("Migration must add sync_tombstones", cursor.moveToFirst())
        }

        sqlite.query("PRAGMA table_info(sync_tombstones)").use { cursor ->
            val names = mutableSetOf<String>()
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) names.add(cursor.getString(nameIndex))
            assertTrue(names.containsAll(setOf("id", "unitKey", "entityType", "entityId", "deletedAt")))
        }

        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testInitialDataPopulateCompleteness() {
        assertTrue("Initial default points must not be empty", InitialData.getDefaultPoints().isNotEmpty())
        assertTrue("Initial default items must not be empty", InitialData.getDefaultItems().isNotEmpty())
        assertTrue("Must have at least one base warehouse", InitialData.getDefaultPoints().any { it.isBase })
        assertTrue("Must have RAV category items", InitialData.getDefaultItems().any { it.serviceCategory == "Служба РАВ" })
    }

    @Test
    fun testReorderWarehousePoints() = runBlocking {
        val p1 = WarehousePoint("p1", "Точка А", "Склад А", false, 0)
        val p2 = WarehousePoint("p2", "Точка Б", "Склад Б", false, 1)
        val p3 = WarehousePoint("p3", "Точка В", "Склад В", false, 2)

        dao.insertPoints(listOf(p1, p2, p3))

        val initial = dao.getAllPoints().first()
        assertEquals("p1", initial[0].id)
        assertEquals("p2", initial[1].id)
        assertEquals("p3", initial[2].id)

        // Reorder: p3 first, then p1, then p2
        val reordered = listOf(
            p3.copy(orderIndex = 0),
            p1.copy(orderIndex = 1),
            p2.copy(orderIndex = 2)
        )
        dao.insertPoints(reordered)

        val updated = dao.getAllPoints().first()
        assertEquals("p3", updated[0].id)
        assertEquals("p1", updated[1].id)
        assertEquals("p2", updated[2].id)
    }
}
