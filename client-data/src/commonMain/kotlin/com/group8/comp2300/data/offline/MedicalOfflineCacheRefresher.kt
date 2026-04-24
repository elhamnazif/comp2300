package com.group8.comp2300.data.offline

import com.group8.comp2300.data.local.*
import com.group8.comp2300.data.notifications.AppointmentNotificationScheduler
import com.group8.comp2300.data.notifications.RoutineNotificationScheduler
import com.group8.comp2300.data.remote.ApiService

class MedicalOfflineCacheRefresher(
    private val apiService: ApiService,
    private val appointmentLocal: AppointmentLocalDataSource,
    private val medicationLocal: MedicationLocalDataSource,
    private val routineLocal: RoutineLocalDataSource,
    private val routineOccurrenceOverrideLocal: RoutineOccurrenceOverrideLocalDataSource,
    private val medicationLogLocal: MedicationLogLocalDataSource,
    private val moodLocal: MoodLocalDataSource,
    private val routineNotificationScheduler: RoutineNotificationScheduler,
    private val appointmentNotificationScheduler: AppointmentNotificationScheduler,
) : OfflineCacheRefresher {
    override suspend fun refreshCaches() {
        val remoteMedications = apiService.getUserMedications()
        val remoteRoutines = apiService.getUserRoutines()
        val remoteOverrides = apiService.getRoutineOccurrenceOverrides()
        val remoteLogs = apiService.getMedicationLogHistory()
        val remoteMoods = apiService.getMoodHistory()
        val remoteAppointments = apiService.getAppointments()

        medicationLocal.replaceAll(remoteMedications)
        routineLocal.replaceAll(remoteRoutines)
        routineOccurrenceOverrideLocal.replaceAll(remoteOverrides)
        medicationLogLocal.replaceAll(remoteLogs)
        moodLocal.replaceAll(remoteMoods)
        appointmentLocal.replaceAll(remoteAppointments)
        routineNotificationScheduler.syncAllRoutines()
        appointmentNotificationScheduler.syncAllAppointments()
    }
}
