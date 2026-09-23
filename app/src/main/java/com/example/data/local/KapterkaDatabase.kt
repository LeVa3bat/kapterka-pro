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
import com.example.data.model.SyncTombstone
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import androidx.room.TypeConverters
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
        UserProfile::class,
        SyncTombstone::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KapterkaDatabase : RoomDatabase() {
    abstract fun kapterkaDao(): KapterkaDao

    companion object {
        @Volatile
        private var INSTANCE: KapterkaDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes between v1 and v2, preserve all tables and data
            }
        }

        internal val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add-only migration. Existing user tables/data remain untouched.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_tombstones (
                        id TEXT NOT NULL PRIMARY KEY,
                        unitKey TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sync_tombstones_unitKey ON sync_tombstones(unitKey)"
                )
            }
        }

        internal val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add-only ownership marker for Sklad PRO catalog isolation.
                // Existing rows are preserved and claimed by the active profile on first launch.
                db.execSQL(
                    "ALTER TABLE inventory_items ADD COLUMN profileId TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): KapterkaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KapterkaDatabase::class.java,
                    "kapterka_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
                        unitKey = "",
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
