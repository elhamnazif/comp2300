package com.group8.comp2300.feature.records

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.RecordSortOrder
import com.group8.comp2300.domain.repository.medical.MedicalRecordDataRepository
import com.group8.comp2300.platform.files.MedicalRecordFileOpener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MedicalRecordViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh keeps current records visible while request is in flight`() = runTest(dispatcher) {
        val firstBatch = listOf(
            sampleRecord(id = "1", category = MedicalRecordCategory.GENERAL),
            sampleRecord(id = "2", category = MedicalRecordCategory.LAB_RESULT),
        )
        val gate = CompletableDeferred<Unit>()
        val repository = RefreshControlledMedicalRecordRepository(
            initialRecords = firstBatch,
            refreshGate = gate,
        )
        val viewModel = MedicalRecordViewModel(
            repository = repository,
            fileOpener = MedicalRecordFileOpener(),
        )

        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.records.size)

        viewModel.refresh()
        runCurrent()

        assertTrue(viewModel.uiState.isRefreshing)
        assertEquals(2, viewModel.uiState.records.size)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isRefreshing)
        assertEquals(2, viewModel.uiState.records.size)
    }

    @Test
    fun `category filter narrows visible records without refetching`() = runTest(dispatcher) {
        val repository = RefreshControlledMedicalRecordRepository(
            initialRecords = listOf(
                sampleRecord(id = "1", category = MedicalRecordCategory.GENERAL),
                sampleRecord(id = "2", category = MedicalRecordCategory.LAB_RESULT),
                sampleRecord(id = "3", category = MedicalRecordCategory.LAB_RESULT),
            ),
        )
        val viewModel = MedicalRecordViewModel(
            repository = repository,
            fileOpener = MedicalRecordFileOpener(),
        )

        advanceUntilIdle()
        viewModel.selectCategoryFilter(MedicalRecordCategory.LAB_RESULT)

        assertEquals(MedicalRecordCategory.LAB_RESULT, viewModel.uiState.selectedCategory)
        assertEquals(listOf("2", "3"), viewModel.uiState.records.map { it.id })

        viewModel.selectCategoryFilter(null)
        assertNull(viewModel.uiState.selectedCategory)
        assertEquals(3, viewModel.uiState.records.size)
    }

    @Test
    fun `delete removes record from visible filtered list`() = runTest(dispatcher) {
        val repository = RefreshControlledMedicalRecordRepository(
            initialRecords = listOf(
                sampleRecord(id = "1", category = MedicalRecordCategory.GENERAL),
                sampleRecord(id = "2", category = MedicalRecordCategory.LAB_RESULT),
            ),
        )
        val viewModel = MedicalRecordViewModel(
            repository = repository,
            fileOpener = MedicalRecordFileOpener(),
        )

        advanceUntilIdle()
        viewModel.selectCategoryFilter(MedicalRecordCategory.LAB_RESULT)
        viewModel.deleteRecord("2")
        advanceUntilIdle()

        assertNull(viewModel.uiState.deletingRecordId)
        assertTrue(viewModel.uiState.records.isEmpty())
        assertEquals(listOf("2"), repository.deletedIds)
    }

    @Test
    fun `load failure keeps full screen error for empty state`() = runTest(dispatcher) {
        val repository = FailingMedicalRecordRepository()
        val viewModel = MedicalRecordViewModel(
            repository = repository,
            fileOpener = MedicalRecordFileOpener(),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.isRefreshing)
        assertEquals("Failed to load records", viewModel.uiState.loadErrorMessage)
        assertTrue(viewModel.uiState.records.isEmpty())
    }

    @Test
    fun `sort change reloads records with requested order`() = runTest(dispatcher) {
        val repository = RefreshControlledMedicalRecordRepository(
            initialRecords = listOf(sampleRecord(id = "1")),
        )
        val viewModel = MedicalRecordViewModel(
            repository = repository,
            fileOpener = MedicalRecordFileOpener(),
        )

        advanceUntilIdle()
        viewModel.loadRecords(RecordSortOrder.NAME_AZ)
        advanceUntilIdle()

        assertEquals(RecordSortOrder.NAME_AZ, viewModel.uiState.selectedSort)
        assertEquals("NAME_ASC", repository.requestedSorts.last())
    }
}

private class RefreshControlledMedicalRecordRepository(
    initialRecords: List<MedicalRecord>,
    private val refreshGate: CompletableDeferred<Unit>? = null,
) : MedicalRecordDataRepository {
    private var currentRecords: List<MedicalRecord> = initialRecords
    val deletedIds = mutableListOf<String>()
    val requestedSorts = mutableListOf<String>()
    private var requestCount = 0

    override suspend fun getMedicalRecords(sort: String): List<MedicalRecord> {
        requestedSorts += sort
        if (requestCount++ > 0) {
            refreshGate?.await()
        }
        return currentRecords
    }

    override suspend fun uploadMedicalRecord(
        fileBytes: ByteArray,
        fileName: String,
        category: MedicalRecordCategory,
    ): Boolean = true

    override suspend fun downloadMedicalRecord(id: String): ByteArray = error("unused")

    override suspend fun deleteMedicalRecord(id: String) {
        deletedIds += id
        currentRecords = currentRecords.filterNot { it.id == id }
    }
}

private class FailingMedicalRecordRepository : MedicalRecordDataRepository {
    override suspend fun getMedicalRecords(sort: String): List<MedicalRecord> = error("boom")

    override suspend fun uploadMedicalRecord(
        fileBytes: ByteArray,
        fileName: String,
        category: MedicalRecordCategory,
    ): Boolean = false

    override suspend fun downloadMedicalRecord(id: String): ByteArray = error("unused")

    override suspend fun deleteMedicalRecord(id: String) = Unit
}

private fun sampleRecord(
    id: String,
    category: MedicalRecordCategory = MedicalRecordCategory.GENERAL,
): MedicalRecord = MedicalRecord(
    id = id,
    fileName = "record-$id.pdf",
    fileSize = 1_024,
    createdAt = 1_000L + id.toLong(),
    category = category,
)
