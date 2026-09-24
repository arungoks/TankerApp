package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.BillingCycle
import com.arun.tankerapp.core.data.database.entity.TankerLog
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import com.arun.tankerapp.core.data.model.firestore.ApartmentDocument
import com.arun.tankerapp.core.data.model.firestore.BillingCycleDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class BillingRepositoryTest {

    private lateinit var repository: BillingRepository
    
    // Mocks
    private val firestore: FirebaseFirestore = mock()
    private val auth: FirebaseAuth = mock()
    private val tankerRepository: TankerRepository = mock()
    private val vacancyRepository: VacancyRepository = mock()
    
    // Firestore Mocks (Deep mocking needed for constructor initialization which calls collection())
    private val collectionReference: CollectionReference = mock()
    private val documentReference: DocumentReference = mock()

    @Before
    fun setUp() {
        // Mock Firestore collection calls
        whenever(firestore.collection(any())).thenReturn(collectionReference)
        whenever(collectionReference.document(any())).thenReturn(documentReference)
        whenever(vacancyRepository.getAllDailyOccupancies()).thenReturn(flowOf(emptyList()))
        
        repository = BillingRepository(firestore, auth, tankerRepository, vacancyRepository)
    }

    @Test
    fun getBillingReport_calculates_correctly_without_vacancies() = runBlocking {
        // Given
        val aptDoc1 = ApartmentDocument(id = "101", number = "101", defaultOccupancy = 2)
        val aptDoc2 = ApartmentDocument(id = "102", number = "102", defaultOccupancy = 2)
        val apartments = listOf(aptDoc1, aptDoc2)

        val tankers = listOf(
            TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 2),
            TankerLog(date = "2026-02-12", month = 2, year = 2026, count = 3)
        )
        
        whenever(vacancyRepository.getApartmentDocuments()).thenReturn(flowOf(apartments))
        whenever(tankerRepository.getAllTankers()).thenReturn(flowOf(tankers))
        whenever(vacancyRepository.getAllVacancies()).thenReturn(flowOf(emptyList()))

        // When
        val report = repository.getBillingReport().first()

        // Then
        assertEquals(2, report.size)
        // Check apt1
        assertEquals(5, report[0].billableTankers) // 2 + 3
        assertEquals(5, report[0].totalTankersInCycle)
        // Check apt2
        assertEquals(5, report[1].billableTankers)
    }

    @Test
    fun getBillingReport_deducts_vacancies_correctly() = runBlocking {
        // Given
        val aptDoc1 = ApartmentDocument(id = "101", number = "101", defaultOccupancy = 2)
        val aptDoc2 = ApartmentDocument(id = "102", number = "102", defaultOccupancy = 2)
        val apartments = listOf(aptDoc1, aptDoc2)

        val tankers = listOf(
            TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 2),
            TankerLog(date = "2026-02-12", month = 2, year = 2026, count = 3)
        )
        
        // Apt2 is vacant on Feb 10
        val vacancies = listOf(
             VacancyLog(apartmentId = "102".hashCode().toLong(), startDate = "2026-02-10", endDate = "2026-02-10")
        )

        whenever(vacancyRepository.getApartmentDocuments()).thenReturn(flowOf(apartments))
        whenever(tankerRepository.getAllTankers()).thenReturn(flowOf(tankers))
        whenever(vacancyRepository.getAllVacancies()).thenReturn(flowOf(vacancies))

        // When
        val report = repository.getBillingReport().first()

        // Then
        // Apt1: no vacancy -> 5
        assertEquals(5, report[0].billableTankers)
        
        // Apt2: vacant on Feb 10 (2 tankers), present on Feb 12 (3 tankers) -> 3
        assertEquals(3, report[1].billableTankers)
    }

    @Test
    fun getBillingReport_handles_vacancy_range() = runBlocking {
        // Given
        val aptDoc1 = ApartmentDocument(id = "101", number = "101", defaultOccupancy = 2)
        val apartments = listOf(aptDoc1)

        val tankers = listOf(
            TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 2),
            TankerLog(date = "2026-02-12", month = 2, year = 2026, count = 3)
        )

        // Vacant from Feb 9 to Feb 11 (covers Feb 10)
        val vacancies = listOf(
            VacancyLog(apartmentId = "101".hashCode().toLong(), startDate = "2026-02-09", endDate = "2026-02-11")
        )

        whenever(vacancyRepository.getApartmentDocuments()).thenReturn(flowOf(apartments))
        whenever(tankerRepository.getAllTankers()).thenReturn(flowOf(tankers))
        whenever(vacancyRepository.getAllVacancies()).thenReturn(flowOf(vacancies))

        // When
        val report = repository.getBillingReport().first()

        // Then
        assertEquals(3, report[0].billableTankers) // Only Feb 12 counts (3)
    }
}
