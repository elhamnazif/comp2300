package com.group8.comp2300.database

import kotlin.Long
import kotlin.String

public data class AppointmentSlots(
  public val id: String,
  public val clinic_id: String,
  public val start_time: String,
  public val end_time: String,
  public val is_booked: Long?
)