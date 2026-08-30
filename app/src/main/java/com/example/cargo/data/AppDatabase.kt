package com.example.cargo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Shipment::class, Contact::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shipmentDao(): ShipmentDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cargo_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration v1 → v2: add phones + smsSent to shipments, create contacts table
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shipments ADD COLUMN senderPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shipments ADD COLUMN receiverPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shipments ADD COLUMN smsSent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""CREATE TABLE IF NOT EXISTS contacts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    notes TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL
                )""")
            }
        }

        val ALL_MIGRATIONS = arrayOf<androidx.room.migration.Migration>(MIGRATION_1_2)
    }
}
