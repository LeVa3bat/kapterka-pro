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
                // Points initialized in Repository

                // Initialize catalog
                // Items initialized in Repository
            }
        }
    }
}
