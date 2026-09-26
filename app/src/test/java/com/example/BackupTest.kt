package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.backup.BackupCodec
import com.example.data.backup.BackupFormatException
import com.example.data.backup.BackupSnapshot
import com.example.data.local.KapterkaDatabase
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val snapshot = BackupSnapshot(
        items = listOf(InventoryItem("i1", "Мина \"120\"", "Служба РАВ", "Мины", "шт.", "Кат. 2", "3ВОФ34", true)),
        points = listOf(WarehousePoint("p1", "Склад №1", "описание\nвторая строка", true, 2, 111L)),
        stocks = listOf(StockRecord("p1", "i1", 40, 100, 60, 222L)),
        operations = listOf(
            OperationRecord("o1", OperationType.EXPENDITURE, "Склад №1", "", "12", "Иванов", "коммент", 333L, "Мина — 5 шт.", "[{\"a\":1}]")
        ),
        requisitions = listOf(RequisitionRequest("r1", "Склад №1", "Петров", RequestStatus.ASSEMBLING, "срочно", 444L, "Мина — 2 шт.", "[]"))
    )

    @Test
    fun encodeDecodeKeepsEverything() {
        val restored = BackupCodec.decode(BackupCodec.encode(snapshot, "3.7.0", 1L))
        assertEquals(snapshot, restored)
    }

    @Test
    fun brokenOrForeignFileIsRejected() {
        for (bad in listOf("", "не json", "{\"a\":1}", "{\"format\":\"kapterka-backup\",\"version\":1}")) {
            try {
                BackupCodec.decode(bad)
                fail("должен отклонить: $bad")
            } catch (_: BackupFormatException) {
            }
        }
    }

    @Test
    fun newerFormatIsRejected() {
        val text = BackupCodec.encode(snapshot, "9.9", 1L).replace("\"version\":1", "\"version\":99")
        try {
            BackupCodec.decode(text)
            fail("должен отклонить более новую копию")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!.contains("новой"))
        }
    }

    @Test
    fun replaceAllDataSwapsDatabaseContents() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dao = KapterkaDatabase.getDatabase(context, kotlinx.coroutines.GlobalScope).kapterkaDao()
        dao.insertPoint(WarehousePoint("old", "Старый", "", false, 0, 1L))
        dao.replaceAllData(snapshot.items, snapshot.points, snapshot.stocks, snapshot.operations, snapshot.requisitions)
        assertEquals(listOf("p1"), dao.getAllPoints().first().map { it.id })
        assertEquals(listOf("i1"), dao.getAllItems().first().map { it.id })
        assertEquals(snapshot.stocks, dao.getAllStockRecords().first())
        assertEquals(snapshot.operations, dao.getAllOperations().first())
        assertEquals(snapshot.requisitions, dao.getAllRequisitions().first())
    }
}
