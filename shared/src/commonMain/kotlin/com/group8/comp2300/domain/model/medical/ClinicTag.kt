package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class ClinicTag(val clinicId: String, val tagName: String)
