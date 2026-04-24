package com.group8.comp2300.service.appointment

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.AppointmentStatus
import com.group8.comp2300.domain.model.medical.BookingPaymentMethod
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.bookingConsultationFee
import com.group8.comp2300.domain.model.medical.consultationFeeFor
import com.group8.comp2300.domain.model.medical.resolvedStatus
import com.group8.comp2300.domain.repository.AppointmentRepository
import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.absoluteValue

class MockClinicOperationsService(
    private val appointmentRepository: AppointmentRepository,
    private val appointmentSlotRepository: AppointmentSlotRepository,
    private val clinicRepository: ClinicRepository,
) {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun prepareCatalog() {
        ensureAvailability(clinicRepository.getAll())
    }

    fun prepareClinic(clinicId: String) {
        clinicRepository.getById(clinicId)?.let { clinic ->
            ensureAvailability(listOf(clinic))
        }
    }

    fun syncUserAppointments(userId: String): List<Appointment> {
        progressLifecycle(userId)
        return appointmentRepository.getByUserId(userId)
    }

    private fun ensureAvailability(clinics: List<Clinic>) {
        val today = LocalDate.now(zoneId)
        clinics.forEach { clinic ->
            val futureSlots = appointmentSlotRepository.getByClinic(clinic.id)
                .filter { it.endTime > System.currentTimeMillis() }
                .mapTo(mutableSetOf(), AppointmentSlot::startTime)

            repeat(HORIZON_DAYS) { dayOffset ->
                val date = today.plusDays(dayOffset.toLong() + 1L)
                slotTemplatesFor(clinic.id, date).forEach { startTime ->
                    if (futureSlots.add(startTime)) {
                        appointmentSlotRepository.createSlot(
                            AppointmentSlot(
                                id = UUID.randomUUID().toString(),
                                clinicId = clinic.id,
                                startTime = startTime,
                                endTime = startTime + SLOT_DURATION_MS,
                                isBooked = false,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun progressLifecycle(userId: String) {
        val now = System.currentTimeMillis()
        appointmentRepository.getByUserId(userId).forEach { appointment ->
            if (appointment.resolvedStatus() == AppointmentStatus.CANCELLED) return@forEach

            val desiredStatus = simulatedStatusFor(appointment, now)
            if (desiredStatus.name != appointment.status) {
                appointmentRepository.updateAppointmentStatus(appointment.id, desiredStatus.name)
            }

            if (
                appointment.paymentStatus != MOCK_PAID_STATUS ||
                appointment.paymentMethod.isNullOrBlank() ||
                appointment.paymentAmount == null
            ) {
                appointmentRepository.updatePaymentDetails(
                    id = appointment.id,
                    paymentMethod = appointment.paymentMethod ?: DEFAULT_PAYMENT_METHOD.name,
                    paymentStatus = MOCK_PAID_STATUS,
                    paymentAmount = appointment.paymentAmount ?: appointment.clinicId
                        ?.let { clinicRepository.getById(it)?.bookingConsultationFee() }
                        ?: DEFAULT_CONSULTATION_FEE,
                    transactionId = appointment.transactionId ?: mockTransactionId(appointment.id),
                )
            }
        }
    }

    private fun simulatedStatusFor(appointment: Appointment, now: Long): AppointmentStatus {
        val currentStatus = appointment.resolvedStatus()
        if (currentStatus.isTerminal) {
            return currentStatus
        }

        val start = appointment.appointmentTime
        val end = start + SLOT_DURATION_MS
        val isNoShow = shouldBecomeNoShow(appointment.id)

        return when {
            currentStatus == AppointmentStatus.PENDING_PAYMENT && now < start -> AppointmentStatus.CONFIRMED
            now < start -> AppointmentStatus.CONFIRMED
            now < end -> AppointmentStatus.CHECKED_IN
            isNoShow && now >= end + NO_SHOW_GRACE_MS -> AppointmentStatus.NO_SHOW
            now >= end -> AppointmentStatus.COMPLETED
            else -> AppointmentStatus.CONFIRMED
        }
    }

    private fun slotTemplatesFor(clinicId: String, date: LocalDate): List<Long> = BASE_SLOT_TIMES
        .filter { shouldExposeSlot(clinicId, date, it) }
        .map { time ->
            date.atTime(time)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }

    private fun shouldExposeSlot(clinicId: String, date: LocalDate, time: LocalTime): Boolean {
        val fingerprint = listOf(clinicId, date.toString(), time.toString()).joinToString("#").hashCode().absoluteValue
        return when {
            time.hour == 16 -> fingerprint % 4 != 0
            time.hour == 11 -> fingerprint % 5 != 0
            else -> fingerprint % 7 != 0
        }
    }

    private fun shouldBecomeNoShow(appointmentId: String): Boolean = appointmentId.hashCode().absoluteValue % 11 == 0

    private fun mockTransactionId(appointmentId: String): String {
        val suffix = appointmentId.filter(Char::isLetterOrDigit).take(12).ifBlank { "booking" }
        return "mock_$suffix"
    }

    companion object {
        private const val HORIZON_DAYS = 21
        const val SLOT_DURATION_MS: Long = 30L * 60L * 1000L
        private const val NO_SHOW_GRACE_MS: Long = 2L * 60L * 60L * 1000L
        const val MOCK_PAID_STATUS = "PAID"
        val DEFAULT_PAYMENT_METHOD: BookingPaymentMethod = BookingPaymentMethod.VISA_4242
        val DEFAULT_CONSULTATION_FEE: Double = consultationFeeFor(null)

        private val BASE_SLOT_TIMES = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
        )
    }
}
