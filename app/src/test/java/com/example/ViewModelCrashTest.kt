package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.KapterkaViewModel
import com.example.data.model.OperationItemEntry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelCrashTest {
    @Test
    fun testRecordIncome() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = KapterkaViewModel(app)
        vm.recordIncome("point1", "Point 1", "Supplier", listOf(
            OperationItemEntry("item1", "Item 1", "шт", 10)
        ), "Comment")
        
        // Wait a bit for coroutines
        kotlinx.coroutines.delay(2000)
    }
}
