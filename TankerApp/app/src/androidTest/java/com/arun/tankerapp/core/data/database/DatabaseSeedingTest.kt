package com.arun.tankerapp.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSeedingTest {

    private lateinit var database: TankerDatabase
    private lateinit var apartmentDao: ApartmentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // callback to seed
        val callback = DatabaseCallback()

        database = Room.inMemoryDatabaseBuilder(
            context,
            TankerDatabase::class.java
        )
            .addCallback(callback)
            .allowMainThreadQueries()
            .build()
        
        apartmentDao = database.apartmentDao()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
    }

    @Test
    fun database_isSeeded_initially() = runBlocking {
        // Trigger database creation by accessing DAO
        // This will call onCreate, which runs the seeding
        
        // Wait for data? onCreate runs synchronously in the transation? Yes.
        // It runs inside the transaction of opening the DB.
        
        // Check count
        val count = apartmentDao.getCount()
        
        // Depending on race condition? No, onCreate runs during first access.
        // But DatabaseCallback uses execSQL directly.
        
        assertEquals("Expected 68 apartments after seeding", 68, count)
        
        // Check content
        val all = apartmentDao.getAllApartments().first()
        val unique = all.map { it.number }.toSet()
        assertEquals(68, unique.size)
        assertTrue(unique.contains("101"))
        assertTrue(unique.contains("1704"))
    }
}
