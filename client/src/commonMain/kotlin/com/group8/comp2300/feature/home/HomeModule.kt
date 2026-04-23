package com.group8.comp2300.feature.home

import com.group8.comp2300.data.notifications.RoutineNotificationService
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import com.group8.comp2300.domain.repository.medical.OfflineSyncCoordinator
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel {
        HomeViewModel(
            syncCoordinator = get<OfflineSyncCoordinator>(),
            appointmentRepository = get<AppointmentDataRepository>(),
            medicationRepository = get<MedicationDataRepository>(),
            medicationLogRepository = get<MedicationLogDataRepository>(),
            notificationService = get<RoutineNotificationService>(),
        )
    }
}
