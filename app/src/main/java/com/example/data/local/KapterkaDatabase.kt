package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        InventoryItem::class,
        WarehousePoint::class,
        StockRecord::class,
        OperationRecord::class,
        RequisitionRequest::class,
        UserProfile::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KapterkaDatabase : RoomDatabase() {
    abstract fun kapterkaDao(): KapterkaDao

    companion object {
        @Volatile
        private var INSTANCE: KapterkaDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): KapterkaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KapterkaDatabase::class.java,
                    "kapterka_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.kapterkaDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: KapterkaDao) {
                // Initialize default profile
                dao.saveUserProfile(
                    UserProfile(
                        id = 1,
                        callsign = "",
                        unitName = "",
                        unitKey = "kapt_" + java.util.UUID.randomUUID().toString().take(6),
                        email = "",
                        isLoggedIn = false,
                        isProActive = false,
                        demoDaysLeft = 3,
                        proDaysLeft = 30,
                        isOnline = true,
                        onlineCount = 1
                    )
                )

                // Initialize default points
                dao.insertPoints(InitialData.defaultPoints)

                // Initialize catalog
                dao.insertItems(InitialData.defaultItems)

                // Seed some initial realistic stock for Base Sklad to make the app live and ready
                val baseStock = listOf(
                    StockRecord("base_sklad", "rav_01", quantity = 48, incomeTotal = 48, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_03", quantity = 64, incomeTotal = 64, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_05", quantity = 12, incomeTotal = 12, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_09", quantity = 30, incomeTotal = 30, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_11", quantity = 40, incomeTotal = 40, expenseTotal = 0),
                    StockRecord("base_sklad", "bpla_01", quantity = 2, incomeTotal = 2, expenseTotal = 0),
                    StockRecord("base_sklad", "bpla_02", quantity = 1, incomeTotal = 1, expenseTotal = 0),
                    StockRecord("base_sklad", "bpla_03", quantity = 10, incomeTotal = 10, expenseTotal = 0),
                    StockRecord("base_sklad", "bpla_05", quantity = 20, incomeTotal = 20, expenseTotal = 0),
                    StockRecord("base_sklad", "reb_01", quantity = 6, incomeTotal = 6, expenseTotal = 0),
                    StockRecord("base_sklad", "reb_03", quantity = 1, incomeTotal = 1, expenseTotal = 0),
                    StockRecord("base_sklad", "med_01", quantity = 25, incomeTotal = 25, expenseTotal = 0),
                    StockRecord("base_sklad", "med_03", quantity = 20, incomeTotal = 20, expenseTotal = 0),
                    StockRecord("base_sklad", "gsm_01", quantity = 200, incomeTotal = 200, expenseTotal = 0),
                    StockRecord("base_sklad", "prod_01", quantity = 50, incomeTotal = 50, expenseTotal = 0)
                )
                dao.insertOrUpdateStockList(baseStock)
            }
        }
    }
}
