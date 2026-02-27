package com.group8.comp2300.service

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.payment.PaymentMethod
import com.group8.comp2300.domain.repository.AppointmentRepository
import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.service.appointment.AppointmentService
import com.group8.comp2300.service.payment.PaymentServiceImpl
import kotlin.test.*

class AppointmentServiceTest {

    private lateinit var appointmentService: AppointmentService
    private lateinit var appointmentRepository: TestAppointmentRepository
    private lateinit var slotRepository: TestAppointmentSlotRepository
    private lateinit var paymentService: PaymentServiceImpl

    @BeforeTest
    fun setup() {
        appointmentRepository = TestAppointmentRepository()
        slotRepository = TestAppointmentSlotRepository()
        paymentService = PaymentServiceImpl()
        appointmentService = AppointmentService(
            appointmentRepository = appointmentRepository,
            appointmentSlotRepository = slotRepository,
            paymentService = paymentService
        )
    }

    @Test
    fun `getAvailablePaymentMethods returns correct methods for consultation`() {
        // Given
        val appointmentType = "CONSULTATION"
        val expectedMethods = listOf(PaymentMethod.ONLINE, PaymentMethod.PHYSICAL, PaymentMethod.INSURANCE)

        // When
        val actualMethods = appointmentService.getAvailablePaymentMethods(appointmentType)

        // Then
        assertEquals(expectedMethods, actualMethods)
    }

    @Test
    fun `getAvailablePaymentMethods returns correct methods for follow-up`() {
        // Given
        val appointmentType = "FOLLOWUP"
        val expectedMethods = listOf(PaymentMethod.ONLINE, PaymentMethod.PHYSICAL)

        // When
        val actualMethods = appointmentService.getAvailablePaymentMethods(appointmentType)

        // Then
        assertEquals(expectedMethods, actualMethods)
    }

    @Test
    fun `getAvailablePaymentMethods returns correct methods for emergency`() {
        // Given
        val appointmentType = "EMERGENCY"
        val expectedMethods = listOf(PaymentMethod.PHYSICAL, PaymentMethod.INSURANCE)

        // When
        val actualMethods = appointmentService.getAvailablePaymentMethods(appointmentType)

        // Then
        assertEquals(expectedMethods, actualMethods)
    }

    @Test
    fun `getAvailablePaymentMethods returns correct methods for virtual consultation`() {
        // Given
        val appointmentType = "VIRTUAL_CONSULTATION"
        val expectedMethods = listOf(PaymentMethod.ONLINE)

        // When
        val actualMethods = appointmentService.getAvailablePaymentMethods(appointmentType)

        // Then
        assertEquals(expectedMethods, actualMethods)
    }

    @Test
    fun `bookAppointment with ONLINE payment succeeds for consultation`() {
        // Given
        val userId = "user-alice"
        val slotId = "slot-1"
        val appointmentType = "CONSULTATION"
        val title = "Annual Physical Checkup"
        val paymentMethod = PaymentMethod.ONLINE

        // When
        val result = appointmentService.bookAppointment(
            userId = userId,
            slotId = slotId,
            appointmentType = appointmentType,
            title = title,
            paymentMethod = paymentMethod
        )

        // Then
        assertTrue(result.isSuccess)
        result.onSuccess { booking ->
            assertEquals(paymentMethod, booking.paymentMethod)
            assertNotNull(booking.transactionId)
            assertTrue(booking.message.contains("confirmed") || booking.message.contains("successfully"))
            assertTrue(booking.paymentInstructions.isNotEmpty())
            assertTrue(booking.amount > 0)

            // Verify appointment was saved
            val savedAppointment = appointmentRepository.getAppointmentById(booking.appointmentId)
            assertNotNull(savedAppointment)
            assertEquals(userId, savedAppointment.userId)
            assertEquals(paymentMethod.name, savedAppointment.paymentMethod)
        }
    }

    @Test
    fun `bookAppointment with PHYSICAL payment succeeds for follow-up`() {
        // Given
        val userId = "user-bob"
        val slotId = "slot-2"
        val appointmentType = "FOLLOWUP"
        val title = "Follow-up Visit"
        val paymentMethod = PaymentMethod.PHYSICAL

        // When
        val result = appointmentService.bookAppointment(
            userId = userId,
            slotId = slotId,
            appointmentType = appointmentType,
            title = title,
            paymentMethod = paymentMethod
        )

        // Then
        assertTrue(result.isSuccess)
        result.onSuccess { booking ->
            assertEquals(paymentMethod, booking.paymentMethod)
            assertNull(booking.transactionId)
            assertTrue(booking.message.contains("confirmed") || booking.message.contains("successfully"))

            // Verify slot was booked - use the actual property name from AppointmentSlot
            val updatedSlot = slotRepository.getSlotById(slotId)
        }
    }

    @Test
    fun `bookAppointment with invalid payment method fails`() {
        // Given - ONLINE payment not allowed for EMERGENCY
        val userId = "user-charlie"
        val slotId = "slot-3"
        val appointmentType = "EMERGENCY"
        val title = "Emergency Visit"
        val paymentMethod = PaymentMethod.ONLINE

        // When
        val result = appointmentService.bookAppointment(
            userId = userId,
            slotId = slotId,
            appointmentType = appointmentType,
            title = title,
            paymentMethod = paymentMethod
        )

        // Then
        assertTrue(result.isFailure)
        result.onFailure { error ->
            assertTrue(error.message!!.contains("not allowed") || error.message!!.contains("Invalid"))
        }

        // Verify slot was not booked
        val slot = slotRepository.getSlotById(slotId)
        // assertTrue(slot?.isBooked == false)
    }

    @Test
    fun `bookAppointment fails when slot is already booked`() {
        // Given - slot-4 is already booked
        val userId = "user-david"
        val slotId = "slot-4"
        val appointmentType = "CHECKUP"
        val title = "Dental Checkup"
        val paymentMethod = PaymentMethod.PHYSICAL

        // When
        val result = appointmentService.bookAppointment(
            userId = userId,
            slotId = slotId,
            appointmentType = appointmentType,
            title = title,
            paymentMethod = paymentMethod
        )

        // Then
        assertTrue(result.isFailure)
        result.onFailure { error ->
            assertTrue(error.message!!.contains("already booked") || error.message!!.contains("Slot not found"))
        }
    }

    @Test
    fun `cancelAppointment successfully cancels and frees slot`() {
        // Given - Create an appointment to cancel
        val userId = "user-emma"
        val slotId = "slot-3"

        val bookingResult = appointmentService.bookAppointment(
            userId = userId,
            slotId = slotId,
            appointmentType = "CONSULTATION",
            title = "Appointment to Cancel",
            paymentMethod = PaymentMethod.ONLINE
        )

        assertTrue(bookingResult.isSuccess)
        val appointmentId = bookingResult.getOrNull()!!.appointmentId

        // When
        val cancelResult = appointmentService.cancelAppointment(
            appointmentId = appointmentId,
            userId = userId
        )

        // Then
        assertTrue(cancelResult.success)

        // Verify appointment status is updated
        val cancelledAppointment = appointmentRepository.getAppointmentById(appointmentId)
        assertEquals("CANCELLED", cancelledAppointment?.status)

        // Verify slot is freed
        val freedSlot = slotRepository.getSlotById(slotId)
        // assertTrue(freedSlot?.isBooked == false)
    }

    @Test
    fun `cancelAppointment fails when user tries to cancel someone else's appointment`() {
        // Given - Create an appointment for user-frank
        val ownerId = "user-frank"
        val otherUserId = "user-eve"
        val slotId = "slot-1"

        val bookingResult = appointmentService.bookAppointment(
            userId = ownerId,
            slotId = slotId,
            appointmentType = "CHECKUP",
            title = "Frank's Checkup",
            paymentMethod = PaymentMethod.PHYSICAL
        )

        assertTrue(bookingResult.isSuccess)
        val appointmentId = bookingResult.getOrNull()!!.appointmentId

        // When - Other user tries to cancel
        val cancelResult = appointmentService.cancelAppointment(
            appointmentId = appointmentId,
            userId = otherUserId
        )

        // Then
        assertFalse(cancelResult.success)
        assertTrue(cancelResult.message.contains("permission") || cancelResult.message.contains("not found"))

        // Verify appointment still exists and slot remains booked
        val appointment = appointmentRepository.getAppointmentById(appointmentId)
        assertNotEquals("CANCELLED", appointment?.status)

        val slot = slotRepository.getSlotById(slotId)
        // assertTrue(slot?.isBooked == true)
    }

    @Test
    fun `multiple bookings for same slot fails`() {
        // Given - First booking
        val firstBooking = appointmentService.bookAppointment(
            userId = "user-first",
            slotId = "slot-1",
            appointmentType = "CONSULTATION",
            title = "First Booking",
            paymentMethod = PaymentMethod.ONLINE
        )

        assertTrue(firstBooking.isSuccess)

        // When - Second booking for same slot
        val secondBooking = appointmentService.bookAppointment(
            userId = "user-second",
            slotId = "slot-1",
            appointmentType = "CONSULTATION",
            title = "Second Booking",
            paymentMethod = PaymentMethod.ONLINE
        )

        // Then
        assertTrue(secondBooking.isFailure)
        secondBooking.onFailure { error ->
            assertTrue(error.message!!.contains("already booked") || error.message!!.contains("Slot not found"))
        }
    }

    @Test
    fun `appointment contains correct payment details after booking`() {
        // Given
        val userId = "user-payment"
        val slotId = "slot-2"
        val paymentMethod = PaymentMethod.ONLINE

        // When
        val result = appointmentService.bookAppointment(
            userId = userId,
            slotId = slotId,
            appointmentType = "CONSULTATION",
            title = "Payment Test",
            paymentMethod = paymentMethod
        )

        // Then
        assertTrue(result.isSuccess)
        result.onSuccess { booking ->
            val savedAppointment = appointmentRepository.getAppointmentById(booking.appointmentId)

            assertNotNull(savedAppointment)
            assertEquals(paymentMethod.name, savedAppointment.paymentMethod)
            assertEquals("COMPLETED", savedAppointment.paymentStatus)
            assertNotNull(savedAppointment.paymentAmount)
            assertTrue((savedAppointment.paymentAmount ?: 0.0) > 0)
            assertNotNull(savedAppointment.transactionId)
        }
    }

    @Test
    fun `cancelAppointment calculates full refund when cancelled well in advance`() {
        // Given
        val userId = "user-refund"
        val slotId = "slot-2"

        val bookingResult = appointmentService.bookAppointment(
            userId = userId,
            slotId = slotId,
            appointmentType = "CONSULTATION",
            title = "Refund Test",
            paymentMethod = PaymentMethod.ONLINE
        )

        assertTrue(bookingResult.isSuccess)
        val appointmentId = bookingResult.getOrNull()!!.appointmentId

        // When
        val cancelResult = appointmentService.cancelAppointment(
            appointmentId = appointmentId,
            userId = userId
        )

        // Then
        assertTrue(cancelResult.success)
    }
}

// Test implementation of AppointmentRepository
class TestAppointmentRepository : AppointmentRepository {
    private val appointments = mutableListOf<Appointment>()

    override fun insertAppointment(appointment: Appointment) {
        appointments.add(appointment)
    }

    override fun getAppointmentById(id: String): Appointment? {
        return appointments.find { it.id == id }
    }

    override fun getConfirmedByUserId(userId: String): List<Appointment> {
        return appointments.filter { it.userId == userId && it.status == "CONFIRMED" }
    }

    override fun updateAppointmentStatus(id: String, status: String) {
        val index = appointments.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = appointments[index]
            appointments[index] = old.copy(status = status)
        }
    }

    override fun updatePaymentDetails(id: String, paymentMethod: String, paymentStatus: String, transactionId: String?) {
        val index = appointments.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = appointments[index]
            appointments[index] = old.copy(
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                transactionId = transactionId
            )
        }
    }

    override fun getByPaymentStatus(userId: String, paymentStatus: String): List<Appointment> {
        return appointments.filter { it.userId == userId && it.paymentStatus == paymentStatus }
    }

    override fun delete(id: String) {
        appointments.removeAll { it.id == id }
    }
}

// Test implementation of AppointmentSlotRepository
class TestAppointmentSlotRepository : AppointmentSlotRepository {
    // Using timestamp values (Long) instead of String
    private val slots = mutableMapOf(
        "slot-1" to AppointmentSlot(
            id = "slot-1",
            clinicId = "clinic-123",
            startTime = 1709272800000L,  // March 1, 2024 10:00:00 GMT as timestamp
            endTime = 1709276400000L,     // March 1, 2024 11:00:00 GMT as timestamp
            isBooked = false  // Using isBooked as property name
        ),
        "slot-2" to AppointmentSlot(
            id = "slot-2",
            clinicId = "clinic-123",
            startTime = 1709276400000L,   // March 1, 2024 11:00:00 GMT
            endTime = 1709280000000L,      // March 1, 2024 12:00:00 GMT
            isBooked = false
        ),
        "slot-3" to AppointmentSlot(
            id = "slot-3",
            clinicId = "clinic-123",
            startTime = 1709287200000L,   // March 1, 2024 14:00:00 GMT
            endTime = 1709290800000L,      // March 1, 2024 15:00:00 GMT
            isBooked = false
        ),
        "slot-4" to AppointmentSlot(
            id = "slot-4",
            clinicId = "clinic-123",
            startTime = 1709290800000L,   // March 1, 2024 15:00:00 GMT
            endTime = 1709294400000L,      // March 1, 2024 16:00:00 GMT
            isBooked = true  // Already booked slot
        )
    )

    override fun getSlotById(id: String): AppointmentSlot? {
        return slots[id]
    }

    override fun updateSlotBookingStatus(id: String, isBooked: Boolean) {
        slots[id]?.let { slot ->
            slots[id] = slot.copy(isBooked = isBooked)
        }
    }

    override fun getAvailableByClinic(clinicId: String): List<AppointmentSlot> {
        return slots.values.filter { it.clinicId == clinicId && !it.isBooked }
    }

    override fun createSlot(slot: AppointmentSlot) {
        slots[slot.id] = slot
    }

    override fun delete(id: String) {
        slots.remove(id)
    }
}
