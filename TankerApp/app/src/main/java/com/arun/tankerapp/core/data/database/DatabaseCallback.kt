package com.arun.tankerapp.core.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Inject

/**
 * Seeding the database with the master apartment list using SupportSQLiteDatabase.
 * This avoids circular dependency issues with Hilt + Room.
 */
class DatabaseCallback @Inject constructor() : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedApartments(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Ensure apartments effectively exist even if onCreate was skipped or failed silently
        val cursor = db.query("SELECT COUNT(*) FROM apartments")
        if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            if (count == 0) {
                seedApartments(db)
            }
        }
        cursor.close()
    }

    private fun seedApartments(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            MasterApartmentList.apartments.forEach { number ->
                db.execSQL("INSERT INTO apartments (number) VALUES (?)", arrayOf(number))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
