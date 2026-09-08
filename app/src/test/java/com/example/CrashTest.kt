package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KapterkaDatabase
import com.example.data.repository.KapterkaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import com.example.data.model.OperationItemEntry

@RunWith(AndroidJUnit4::class)
class CrashTest {
    @Test
    fun testRecordIncome() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = com.example.data.local.KapterkaDatabase.getDatabase(context, kotlinx.coroutines.GlobalScope)
        val repo = KapterkaRepository(db.kapterkaDao(), null)
        
        repo.recordIncome("point1", "Point 1", "Supplier", listOf(
            OperationItemEntry("item1", "Item 1", "шт", 10)
        ), "Comment", "Actor")
    }
}
