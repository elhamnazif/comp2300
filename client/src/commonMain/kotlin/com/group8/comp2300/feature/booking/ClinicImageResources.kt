package com.group8.comp2300.feature.booking

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.group8.comp2300.domain.model.medical.Clinic
import comp2300.client.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import comp2300.client.generated.resources.Res as ClientRes

internal fun clinicMockImage(clinic: Clinic): DrawableResource = when (clinic.id.hashCode().mod(5)) {
    0 -> ClientRes.drawable.clinic_photo_medical_center_glass
    1 -> ClientRes.drawable.clinic_photo_frontage_night
    2 -> ClientRes.drawable.clinic_photo_waiting_room
    3 -> ClientRes.drawable.clinic_photo_exam_room
    else -> ClientRes.drawable.clinic_photo_treatment_room
}

@Composable
internal fun ClinicImage(clinic: Clinic, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(
        painter = painterResource(clinicMockImage(clinic)),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}
