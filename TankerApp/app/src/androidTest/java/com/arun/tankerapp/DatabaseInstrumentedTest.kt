package com.arun.tankerapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arun.tankerapp.core.data.database.TankerDatabase
import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import com.arun.tankerapp.core.data.database.dao.TankerDao
import com.arun.tankerapp.core.data.database.dao.VacancyDao
import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.TankerLog
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Room Database verification.
 * Story 1.1 AC: "AppDatabase class should be created with Apartment and Tanker entities defined"
 */
@RunWith(AndroidJUnit4::class)
class DatabaseInstrumentedTest {

    private lateinit var database: TankerDatabase
    private lateinit var apartmentDao: ApartmentDao
    private lateinit var tankerDao: TankerDao
    private lateinit var vacancyDao: VacancyDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TankerDatabase::class.java
        ).allowMainThreadQueries().build()

        apartmentDao = database.apartmentDao()
        tankerDao = database.tankerDao()
        vacancyDao = database.vacancyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun database_isCreated() {
        // Verify database was created by checking DAOs are accessible
        val dao1 = database.apartmentDao()
        val dao2 = database.tankerDao()
        val dao3 = database.vacancyDao()
        
        // If we got here without exceptions, database was created successfully
        assertTrue(true)
    }

    @Test
    fun insertAndReadApartments() = runTest {
        val apartments = listOf(
            Apartment(number = "A-101"),
            Apartment(number = "A-102"),
            Apartment(number = "B-201")
        )
        apartmentDao.insertAll(apartments)

        val count = apartmentDao.getCount()
        assertEquals(3, count)

        val all = apartmentDao.getAllApartments().first()
        assertEquals(3, all.size)
        assertEquals("A-101", all[0].number)
    }

    @Test
    fun insertAndReadTankerLog() = runTest {
        val log = TankerLog(date = "2026-02-10", month = 2, year = 2026)
        tankerDao.insert(log)

        val count = tankerDao.getCount()
        assertEquals(1, count)

        val logs = tankerDao.getAllTankerLogs().first()
        assertEquals(1, logs.size)
        assertEquals("2026-02-10", logs[0].date)
    }

    @Test
    fun insertAndReadVacancyLog() = runTest {
        // First insert an apartment (FK constraint)
        val apartments = listOf(Apartment(id = 1, number = "A-101"))
        apartmentDao.insertAll(apartments)

        val vacancy = VacancyLog(
            apartmentId = 1,
            startDate = "2026-02-01",
            endDate = "2026-02-15"
        )
        vacancyDao.insert(vacancy)

        val count = vacancyDao.getCount()
        assertEquals(1, count)

        val vacancies = vacancyDao.getVacanciesByApartment(1).first()
        assertEquals(1, vacancies.size)
        assertEquals("2026-02-01", vacancies[0].startDate)
    }

    @Test
    fun duplicateApartments_areIgnored() = runTest {
        val apartments = listOf(
            Apartment(id = 1, number = "A-101"),
            Apartment(id = 1, number = "A-101")  // Same ID - should be ignored
        )
        apartmentDao.insertAll(apartments)

        val count = apartmentDao.getCount()
        assertEquals(1, count)
    }
}
