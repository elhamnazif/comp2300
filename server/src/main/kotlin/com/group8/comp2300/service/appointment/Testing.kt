
@file:Suppress("ktlint:standard:no-empty-file")

package com.group8.comp2300.service.appointment

// import com.group8.comp2300.database.Appointment
// import com.group8.comp2300.domain.repository.AppointmentRepository
// import com.group8.comp2300.domain.repository.SlotRepository
// import com.group8.comp2300.database.AppointmentSlots
// import com.group8.comp2300.domain.model.payment.PaymentMethod
// import com.group8.comp2300.service.payment.PaymentServiceImpl
// import com.group8.comp2300.service.payment.PaymentService
// import java.util.UUID
//
// fun main() {
//    println("=".repeat(60))
//    println("🧪 TESTING APPOINTMENT BOOKING SYSTEM")
//    println("=".repeat(60))
//
//    // Create mock implementations
//    val appointmentRepository = createMockAppointmentRepository()
//    val slotRepository = createMockSlotRepository()
//    val paymentService = PaymentServiceImpl()
//
//    // Create the service
//    val appointmentService = AppointmentService(
//        appointmentRepository = appointmentRepository,
//        slotRepository = slotRepository,
//        paymentService = paymentService
//    )
//
//    // Run all tests
//    runAllTests(appointmentService, paymentService)
// }
//
// fun createMockAppointmentRepository(): AppointmentRepository {
//    return object : AppointmentRepository {
//        private val appointments = mutableListOf<Appointment>()
//
//        override fun insertAppointment(appointment: Appointment) {
//            appointments.add(appointment)
//            println("   📝 [MOCK] Appointment saved: ${appointment.id}")
//            println("      - Title: ${appointment.title}")
//            println("      - Type: ${appointment.appointment_type}")
//            println("      - Payment: ${appointment.payment_method} (${appointment.payment_status})")
//            println("      - Amount: $${appointment.payment_amount}")
//        }
//
//        override fun getAppointmentById(id: String): Appointment? {
//            return appointments.find { it.id == id }
//        }
//
//        override fun updateAppointmentStatus(id: String, status: String) {
//            val index = appointments.indexOfFirst { it.id == id }
//            if (index >= 0) {
//                val old = appointments[index]
//                appointments[index] = old.copy(status = status)
//                println("   📝 [MOCK] Appointment $id status updated to: $status")
//            }
//        }
//
//        override fun updatePaymentDetails(id: String, paymentMethod: String, paymentStatus: String, transactionId: String?) {
//            val index = appointments.indexOfFirst { it.id == id }
//            if (index >= 0) {
//                val old = appointments[index]
//                appointments[index] = old.copy(
//                    payment_method = paymentMethod,
//                    payment_status = paymentStatus,
//                    transaction_id = transactionId
//                )
//                println("   📝 [MOCK] Payment details updated for: $id")
//            }
//        }
//
//        override fun getAppointmentsByUserAndPaymentStatus(userId: String, paymentStatus: String): List<Appointment> {
//            return appointments.filter { it.user_id == userId && it.payment_status == paymentStatus }
//        }
//    }
// }
//
// fun createMockSlotRepository(): SlotRepository {
//    return object : SlotRepository {
//        private val slots = mutableMapOf(
//            "slot-1" to AppointmentSlots(
//                id = "slot-1",
//                clinic_id = "clinic-123",
//                start_time = "2024-03-01T10:00:00",
//                end_time = "2024-03-01T11:00:00",
//                is_booked = 0L
//            ),
//            "slot-2" to AppointmentSlots(
//                id = "slot-2",
//                clinic_id = "clinic-123",
//                start_time = "2024-03-01T11:00:00",
//                end_time = "2024-03-01T12:00:00",
//                is_booked = 0L
//            ),
//            "slot-3" to AppointmentSlots(
//                id = "slot-3",
//                clinic_id = "clinic-123",
//                start_time = "2024-03-01T14:00:00",
//                end_time = "2024-03-01T15:00:00",
//                is_booked = 0L
//            ),
//            "slot-4" to AppointmentSlots(
//                id = "slot-4",
//                clinic_id = "clinic-123",
//                start_time = "2024-03-01T15:00:00",
//                end_time = "2024-03-01T16:00:00",
//                is_booked = 1L  // Already booked slot
//            )
//        )
//
//        override fun getSlotById(id: String): AppointmentSlots? {
//            return slots[id]
//        }
//
//        override fun updateSlotBookingStatus(id: String, isBooked: Long) {
//            slots[id]?.let { slot ->
//                slots[id] = slot.copy(is_booked = isBooked)
//                println("   📝 [MOCK] Slot $id booking status updated to: $isBooked")
//            }
//        }
//
//        override fun getSlotByBookingId(bookingId: String): AppointmentSlots? {
//            return slots.values.find { it.id == bookingId }
//        }
//    }
// }
// fun runAllTests(appointmentService: AppointmentService, PaymentService: PaymentServiceImpl) {
//
//    // TEST 1: Check available payment methods
//    println("\n📋 TEST 1: Available Payment Methods")
//    println("-".repeat(40))
//
//    val appointmentTypes = listOf("CONSULTATION", "FOLLOWUP", "EMERGENCY", "VIRTUAL_CONSULTATION")
//    appointmentTypes.forEach { type ->
//        val methods = appointmentService.getAvailablePaymentMethods(type)
//        println("   $type: ${methods.joinToString()}")
//    }
//
//    // TEST 2: Book with ONLINE payment (should succeed)
//    println("\n📋 TEST 2: Booking with ONLINE Payment (Consultation)")
//    println("-".repeat(40))
//
//    val result1 = appointmentService.bookAppointment(
//        userId = "user-alice",
//        slotId = "slot-1",
//        appointmentType = "CONSULTATION",
//        title = "Annual Physical Checkup",
//        paymentMethod = PaymentMethod.ONLINE
//    )
//
//    result1.fold(
//        onSuccess = { booking ->
//            println("   ✅ SUCCESS: ${booking.message}")
//            println("   📝 Instructions: ${booking.paymentInstructions}")
//            if (booking.transactionId != null) {
//                println("   🧾 Transaction: ${booking.transactionId}")
//            }
//            println("   💰 Amount: $${booking.amount}")
//        },
//        onFailure = { error ->
//            println("   ❌ FAILED: ${error.message}")
//        }
//    )
//
//    // TEST 3: Book with PHYSICAL payment (should succeed)
//    println("\n📋 TEST 3: Booking with PHYSICAL Payment (Follow-up)")
//    println("-".repeat(40))
//
//    val result2 = appointmentService.bookAppointment(
//        userId = "user-bob",
//        slotId = "slot-2",
//        appointmentType = "FOLLOWUP",
//        title = "Follow-up Visit",
//        paymentMethod = PaymentMethod.PHYSICAL
//    )
//
//    result2.fold(
//        onSuccess = { booking ->
//            println("   ✅ SUCCESS: ${booking.message}")
//            println("   📝 Instructions: ${booking.paymentInstructions}")
//            println("   💰 Amount: $${booking.amount}")
//        },
//        onFailure = { error ->
//            println("   ❌ FAILED: ${error.message}")
//        }
//    )
//
//    // TEST 4: Try invalid payment method (should fail)
//    println("\n📋 TEST 4: Invalid Payment Method (Online for Emergency)")
//    println("-".repeat(40))
//
//    val result3 = appointmentService.bookAppointment(
//        userId = "user-charlie",
//        slotId = "slot-3",
//        appointmentType = "EMERGENCY",
//        title = "Emergency Visit",
//        paymentMethod = PaymentMethod.ONLINE
//    )
//
//    result3.fold(
//        onSuccess = { booking ->
//            println("   ❌ SHOULD HAVE FAILED: ${booking.message}")
//        },
//        onFailure = { error ->
//            println("   ✅ CORRECTLY FAILED: ${error.message}")
//        }
//    )
//
//    // TEST 5: Try to book already booked slot (should fail)
//    println("\n📋 TEST 5: Book Already Booked Slot")
//    println("-".repeat(40))
//
//    val result4 = appointmentService.bookAppointment(
//        userId = "user-david",
//        slotId = "slot-4",  // This slot is already booked (is_booked = 1L)
//        appointmentType = "CHECKUP",
//        title = "Dental Checkup",
//        paymentMethod = PaymentMethod.PHYSICAL
//    )
//
//    result4.fold(
//        onSuccess = { booking ->
//            println("   ❌ SHOULD HAVE FAILED: ${booking.message}")
//        },
//        onFailure = { error ->
//            println("   ✅ CORRECTLY FAILED: ${error.message}")
//        }
//    )
//
//    // TEST 6: Cancel an appointment (if we have one to cancel)
//    println("\n📋 TEST 6: Cancel Appointment")
//    println("-".repeat(40))
//
//    // First create an appointment to cancel
//    val bookingToCancel = appointmentService.bookAppointment(
//        userId = "user-emma",
//        slotId = "slot-3",  // Use slot-3 which is still available
//        appointmentType = "CONSULTATION",
//        title = "Appointment to Cancel",
//        paymentMethod = PaymentMethod.ONLINE
//    )
//
//    bookingToCancel.fold(
//        onSuccess = { booking ->
//            println("   ✅ Created appointment to cancel: ${booking.appointmentId}")
//
//            // Now cancel it
//            Thread.sleep(1000) // Small delay to simulate time passing
//            val cancelResult = appointmentService.cancelAppointment(
//                appointmentId = booking.appointmentId,
//                userId = "user-emma"
//            )
//
//            if (cancelResult.success) {
//                println("   ✅ CANCELLATION SUCCESS: ${cancelResult.message}")
//                if (cancelResult.refundAmount != null && cancelResult.refundAmount > 0) {
//                    println("   💰 Refund: $${cancelResult.refundAmount} (${cancelResult.refundStatus})")
//                }
//            } else {
//                println("   ❌ CANCELLATION FAILED: ${cancelResult.message}")
//            }
//        },
//        onFailure = { error ->
//            println("   ❌ Failed to create test appointment: ${error.message}")
//        }
//    )
//
//    // TEST 7: Try to cancel someone else's appointment (should fail)
//    println("\n📋 TEST 7: Cancel Someone Else's Appointment")
//    println("-".repeat(40))
//
//    // Create an appointment for user-frank
//    val franksBooking = appointmentService.bookAppointment(
//        userId = "user-frank",
//        slotId = "slot-1",  // Using a different slot
//        appointmentType = "CHECKUP",
//        title = "Frank's Checkup",
//        paymentMethod = PaymentMethod.PHYSICAL
//    )
//
//    franksBooking.fold(
//        onSuccess = { booking ->
//            println("   ✅ Created appointment for user-frank: ${booking.appointmentId}")
//
//            // Try to cancel it as user-eve (different user)
//            val cancelResult = appointmentService.cancelAppointment(
//                appointmentId = booking.appointmentId,
//                userId = "user-eve"  // Different user!
//            )
//
//            if (!cancelResult.success) {
//                println("   ✅ CORRECTLY FAILED: ${cancelResult.message}")
//            } else {
//                println("   ❌ SHOULD HAVE FAILED: Cancelled someone else's appointment")
//            }
//        },
//        onFailure = { error ->
//            println("   ❌ Failed to create test appointment: ${error.message}")
//        }
//    )
//
//    // TEST 8: Get available payment methods for different types
//    println("\n📋 TEST 8: Payment Method Summary")
//    println("-".repeat(40))
//
//    val types = listOf("CONSULTATION", "FOLLOWUP", "EMERGENCY", "VIRTUAL_CONSULTATION", "CHECKUP")
//    println("   Appointment Type | Available Payment Methods | Pre-payment Required | Cost")
//    println("   " + "-".repeat(70))
//
//    types.forEach { type ->
//        val methods = appointmentService.getAvailablePaymentMethods(type)
//        val requiresPrePayment = PaymentService.requiresPrePayment(type)
//        val cost = PaymentService.calculatePaymentAmount(type)
//        println("   ${type.padEnd(16)} | ${methods.joinToString().padEnd(27)} | ${requiresPrePayment.toString().padEnd(10)} | $$$cost")
//    }
//
//    println("\n" + "=".repeat(60))
//    println("✅ ALL TESTS COMPLETED")
//    println("=".repeat(60))
// }
//
// // Helper extension function for padding strings
// fun String.padEnd(length: Int): String {
//    return if (this.length >= length) this else this + " ".repeat(length - this.length)
// }
