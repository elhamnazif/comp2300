package com.group8.comp2300

import com.group8.comp2300.domain.model.medical.BookingPaymentMethod
import com.group8.comp2300.domain.model.medical.PricingTier
import com.group8.comp2300.domain.model.medical.consultationFeeFor
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun consultationFeeUsesPricingTierDefaults() {
        assertEquals(25.0, consultationFeeFor(PricingTier.LOW))
        assertEquals(45.0, consultationFeeFor(PricingTier.MEDIUM))
        assertEquals(65.0, consultationFeeFor(PricingTier.HIGH))
        assertEquals(35.0, consultationFeeFor(null))
    }

    @Test
    fun paymentMethodParserAcceptsStoredNames() {
        assertEquals(BookingPaymentMethod.VISA_4242, BookingPaymentMethod.fromRaw("VISA_4242"))
        assertEquals(null, BookingPaymentMethod.fromRaw("MOCK_CARD"))
    }
}
