package com.group8.comp2300.feature.booking

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.BookingPaymentMethod
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.model.medical.PricingTier
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import com.group8.comp2300.domain.repository.medical.FailedSyncMutation
import com.group8.comp2300.domain.repository.medical.OfflineSyncCoordinator
import com.group8.comp2300.domain.repository.medical.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {
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
    fun `selected payment method persists across booking draft updates`() = runTest(dispatcher) {
        val repository = RecordingAppointmentRepository()
        val viewModel = BookingViewModel(
            clinicRepository = FakeClinicRepository(),
            appointmentRepository = repository,
            syncCoordinator = NoOpOfflineSyncCoordinator(),
        )

        advanceUntilIdle()
        viewModel.ensureBookingDraft(clinicId = "clinic-1", slotId = "slot-1")
        viewModel.selectPaymentMethod(BookingPaymentMethod.DIGITAL_WALLET)
        viewModel.updateBookingDraft(reason = "Screening")

        assertEquals(BookingPaymentMethod.DIGITAL_WALLET, viewModel.state.value.bookingDraft?.selectedPaymentMethod)
        assertEquals(45.0, viewModel.state.value.bookingDraft?.quotedFee)
    }

    @Test
    fun `payment route restoration preserves confirmation draft fields`() = runTest(dispatcher) {
        val viewModel = BookingViewModel(
            clinicRepository = FakeClinicRepository(),
            appointmentRepository = RecordingAppointmentRepository(),
            syncCoordinator = NoOpOfflineSyncCoordinator(),
        )

        advanceUntilIdle()
        viewModel.ensureBookingDraft(
            clinicId = "clinic-1",
            slotId = "slot-1",
            appointmentType = "FOLLOW_UP",
            reason = "Bring previous results",
            hasReminder = false,
        )

        val restoredDraft = viewModel.state.value.bookingDraft

        assertNotNull(restoredDraft)
        assertEquals("FOLLOW_UP", restoredDraft.appointmentType)
        assertEquals("Bring previous results", restoredDraft.reason)
        assertEquals(false, restoredDraft.hasReminder)
        assertEquals(45.0, restoredDraft.quotedFee)
    }

    @Test
    fun `reschedule path does not require payment method`() = runTest(dispatcher) {
        val repository = RecordingAppointmentRepository()
        val viewModel = BookingViewModel(
            clinicRepository = FakeClinicRepository(),
            appointmentRepository = repository,
            syncCoordinator = NoOpOfflineSyncCoordinator(),
        )

        advanceUntilIdle()
        val existingAppointment = sampleAppointment(id = "appointment-1")
        viewModel.ensureBookingDraft(
            clinicId = "clinic-1",
            slotId = "slot-1",
            rescheduleAppointment = existingAppointment,
        )
        viewModel.bookClinicAppointment(
            clinicId = "clinic-1",
            slotId = "slot-1",
            appointmentType = "FOLLOW_UP",
            reason = "Bring results",
            hasReminder = false,
        )

        advanceUntilIdle()

        assertNotNull(repository.lastRescheduleRequest)
        assertEquals(null, repository.lastRescheduleRequest?.paymentMethod)
        assertEquals(null, viewModel.state.value.paymentErrorMessage)
    }
}

private class FakeClinicRepository : ClinicRepository {
    private val clinic = Clinic(
        id = "clinic-1",
        name = "City Sexual Health",
        distanceKm = 1.2,
        tags = listOf("testing"),
        nextAvailableSlot = 2_000L,
        lat = 0.0,
        lng = 0.0,
        address = "123 Test Street",
        phone = "12345",
        pricingTier = PricingTier.MEDIUM,
    )
    private val slot = AppointmentSlot(
        id = "slot-1",
        clinicId = "clinic-1",
        startTime = 10_000L,
        endTime = 12_000L,
        isBooked = false,
    )

    override suspend fun getAllClinics(): List<Clinic> = listOf(clinic)

    override suspend fun getClinicById(id: String): Clinic? = clinic.takeIf { it.id == id }

    override suspend fun getAvailableSlots(clinicId: String): List<AppointmentSlot> = listOf(slot)
}

private class RecordingAppointmentRepository : AppointmentDataRepository {
    var lastBookRequest: ClinicBookingRequest? = null
    var lastRescheduleRequest: ClinicBookingRequest? = null

    override suspend fun getAppointments(): List<Appointment> = emptyList()

    override suspend fun getBookingHistory(): List<Appointment> = emptyList()

    override suspend fun getAppointment(id: String): Appointment? = null

    override suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment {
        lastBookRequest = request
        return sampleAppointment(id = "appointment-booked", paymentMethod = request.paymentMethod?.name)
    }

    override suspend fun cancelAppointment(id: String): Appointment = error("unused")

    override suspend fun rescheduleAppointment(id: String, request: ClinicBookingRequest): Appointment {
        lastRescheduleRequest = request
        return sampleAppointment(id = id, appointmentType = request.appointmentType)
    }
}

private class NoOpOfflineSyncCoordinator : OfflineSyncCoordinator {
    override suspend fun syncNow(): SyncStatus = SyncStatus(true, 0, 0, false)

    override suspend fun refreshCaches(): SyncStatus = SyncStatus(true, 0, 0, true)

    override suspend fun listFailedMutations(): List<FailedSyncMutation> = emptyList()

    override suspend fun retryFailedMutation(id: String): SyncStatus = SyncStatus(true, 0, 0, false)

    override suspend fun discardFailedMutation(id: String) = Unit
}

private fun sampleAppointment(
    id: String,
    appointmentType: String = "STI_TESTING",
    paymentMethod: String? = BookingPaymentMethod.VISA_4242.name,
): Appointment = Appointment(
    id = id,
    userId = "user-1",
    title = "Appointment at Clinic",
    appointmentTime = 123_456L,
    appointmentType = appointmentType,
    clinicId = "clinic-1",
    bookingId = "slot-1",
    status = "CONFIRMED",
    notes = "Screening",
    hasReminder = true,
    paymentStatus = "PAID",
    paymentMethod = paymentMethod,
    paymentAmount = 45.0,
    transactionId = "mock_transaction",
)
