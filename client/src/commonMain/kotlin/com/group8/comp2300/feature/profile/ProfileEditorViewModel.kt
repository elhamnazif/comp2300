package com.group8.comp2300.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.UpdateProfileInput
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.usecase.auth.UpdateProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class ProfileEditorViewModel(
    private val authRepository: AuthRepository,
    private val updateProfileUseCase: UpdateProfileUseCase,
) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State.fromUser(authRepository.session.value.userOrNull))

    init {
        viewModelScope.launch {
            authRepository.session.collect { session ->
                syncFromSession(session)
            }
        }
    }

    fun onFirstNameChanged(value: String) {
        state.update { it.copy(firstName = value, errorMessage = null) }
    }

    fun onLastNameChanged(value: String) {
        state.update { it.copy(lastName = value, errorMessage = null) }
    }

    fun onPhoneChanged(value: String) {
        state.update { it.copy(phone = value, errorMessage = null) }
    }

    fun onGenderChanged(value: String) {
        state.update { it.copy(gender = value, errorMessage = null) }
    }

    fun onOrientationChanged(value: String) {
        state.update { it.copy(sexualOrientation = value, errorMessage = null) }
    }

    fun onDateOfBirthChanged(dateMillis: Long?) {
        state.update { it.copy(dateOfBirthMillis = dateMillis, showDatePicker = false, errorMessage = null) }
    }

    fun setDatePickerVisible(visible: Boolean) {
        state.update { it.copy(showDatePicker = visible) }
    }

    fun onPhotoSelected(fileName: String, fileBytes: ByteArray) {
        state.update {
            it.copy(
                selectedPhotoName = fileName,
                selectedPhotoBytes = fileBytes,
                removePhoto = false,
                errorMessage = null,
            )
        }
    }

    fun removePhoto() {
        state.update {
            it.copy(
                selectedPhotoName = null,
                selectedPhotoBytes = null,
                removePhoto = it.originalProfileImageUrl != null,
                errorMessage = null,
            )
        }
    }

    fun clearError() {
        state.update { it.copy(errorMessage = null) }
    }

    private fun syncFromSession(session: AuthSession) {
        val user = (session as? AuthSession.SignedIn)?.user ?: return
        state.update { current ->
            if (current.isLoading || current.isComplete || current.isDirty) {
                current
            } else {
                State.fromUser(user).copy(showDatePicker = current.showDatePicker)
            }
        }
    }

    fun save() {
        val currentState = state.value
        if (!currentState.isSaveEnabled) return

        val signedInUser = authRepository.session.value.userOrNull
        if (signedInUser == null) {
            state.update { it.copy(errorMessage = "Sign in required") }
            return
        }

        state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val updateResult = updateProfileUseCase(
                UpdateProfileInput(
                    firstName = currentState.firstName.trim(),
                    lastName = currentState.lastName.trim(),
                    phone = currentState.normalizedPhone,
                    dateOfBirth = currentState.dateOfBirth,
                    gender = currentState.gender.takeIf(String::isNotBlank)?.let(Gender::fromDisplayName),
                    sexualOrientation = currentState.sexualOrientation
                        .takeIf(String::isNotBlank)
                        ?.let(SexualOrientation::fromDisplayName),
                ),
            )

            val result = updateResult.fold(
                onSuccess = {
                    when {
                        currentState.selectedPhotoBytes != null && currentState.selectedPhotoName != null ->
                            authRepository.uploadProfilePhoto(
                                fileBytes = currentState.selectedPhotoBytes,
                                fileName = currentState.selectedPhotoName,
                            )

                        currentState.removePhoto ->
                            authRepository.removeProfilePhoto()

                        else -> Result.success(it)
                    }
                },
                onFailure = { Result.failure(it) },
            )

            result.fold(
                onSuccess = {
                    state.update { it.copy(isLoading = false, isComplete = true) }
                },
                onFailure = { error ->
                    state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.userFacingMessage(),
                        )
                    }
                },
            )
        }
    }

    data class State(
        val firstName: String = "",
        val lastName: String = "",
        val phone: String = "",
        val dateOfBirthMillis: Long? = null,
        val gender: String = "",
        val sexualOrientation: String = "",
        val originalFirstName: String = "",
        val originalLastName: String = "",
        val originalPhone: String = "",
        val originalDateOfBirthMillis: Long? = null,
        val originalGender: String = "",
        val originalSexualOrientation: String = "",
        val originalProfileImageUrl: String? = null,
        val selectedPhotoName: String? = null,
        val selectedPhotoBytes: ByteArray? = null,
        val removePhoto: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val showDatePicker: Boolean = false,
        val isComplete: Boolean = false,
    ) {
        val dateOfBirth: LocalDate?
            get() = dateOfBirthMillis?.let {
                Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
            }

        val normalizedPhone: String?
            get() = phone.trim().takeIf(String::isNotEmpty)

        val previewImageModel: Any?
            get() = when {
                selectedPhotoBytes != null -> selectedPhotoBytes
                removePhoto -> null
                else -> originalProfileImageUrl
            }

        val initials: String
            get() = listOfNotNull(
                firstName.trim().firstOrNull(),
                lastName.trim().firstOrNull(),
            ).joinToString("") { it.uppercase() }

        val isFormValid: Boolean
            get() = firstName.isNotBlank() && lastName.isNotBlank() && dateOfBirthMillis != null

        val isDirty: Boolean
            get() = firstName != originalFirstName ||
                lastName != originalLastName ||
                phone != originalPhone ||
                dateOfBirthMillis != originalDateOfBirthMillis ||
                gender != originalGender ||
                sexualOrientation != originalSexualOrientation ||
                selectedPhotoBytes != null ||
                removePhoto

        val isSaveEnabled: Boolean
            get() = isFormValid && isDirty && !isLoading

        fun formattedDate(): String = dateOfBirth?.let {
            "${it.day}/${it.month.number}/${it.year}"
        }.orEmpty()

        companion object {
            fun fromUser(user: User?): State {
                val dateOfBirthMillis = user?.dateOfBirth?.atStartOfDayIn(TimeZone.currentSystemDefault())
                return State(
                    firstName = user?.firstName.orEmpty(),
                    lastName = user?.lastName.orEmpty(),
                    phone = user?.phone.orEmpty(),
                    dateOfBirthMillis = dateOfBirthMillis,
                    gender = user?.gender?.displayName.orEmpty(),
                    sexualOrientation = user?.sexualOrientation?.displayName.orEmpty(),
                    originalFirstName = user?.firstName.orEmpty(),
                    originalLastName = user?.lastName.orEmpty(),
                    originalPhone = user?.phone.orEmpty(),
                    originalDateOfBirthMillis = dateOfBirthMillis,
                    originalGender = user?.gender?.displayName.orEmpty(),
                    originalSexualOrientation = user?.sexualOrientation?.displayName.orEmpty(),
                    originalProfileImageUrl = user?.profileImageUrl,
                )
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as State

            if (dateOfBirthMillis != other.dateOfBirthMillis) return false
            if (originalDateOfBirthMillis != other.originalDateOfBirthMillis) return false
            if (removePhoto != other.removePhoto) return false
            if (isLoading != other.isLoading) return false
            if (showDatePicker != other.showDatePicker) return false
            if (isComplete != other.isComplete) return false
            if (firstName != other.firstName) return false
            if (lastName != other.lastName) return false
            if (phone != other.phone) return false
            if (gender != other.gender) return false
            if (sexualOrientation != other.sexualOrientation) return false
            if (originalFirstName != other.originalFirstName) return false
            if (originalLastName != other.originalLastName) return false
            if (originalPhone != other.originalPhone) return false
            if (originalGender != other.originalGender) return false
            if (originalSexualOrientation != other.originalSexualOrientation) return false
            if (originalProfileImageUrl != other.originalProfileImageUrl) return false
            if (selectedPhotoName != other.selectedPhotoName) return false
            if (!selectedPhotoBytes.contentEquals(other.selectedPhotoBytes)) return false
            if (errorMessage != other.errorMessage) return false
            if (isFormValid != other.isFormValid) return false
            if (isDirty != other.isDirty) return false
            if (isSaveEnabled != other.isSaveEnabled) return false
            if (dateOfBirth != other.dateOfBirth) return false
            if (normalizedPhone != other.normalizedPhone) return false
            if (previewImageModel != other.previewImageModel) return false
            if (initials != other.initials) return false

            return true
        }

        override fun hashCode(): Int {
            var result = dateOfBirthMillis?.hashCode() ?: 0
            result = 31 * result + (originalDateOfBirthMillis?.hashCode() ?: 0)
            result = 31 * result + removePhoto.hashCode()
            result = 31 * result + isLoading.hashCode()
            result = 31 * result + showDatePicker.hashCode()
            result = 31 * result + isComplete.hashCode()
            result = 31 * result + firstName.hashCode()
            result = 31 * result + lastName.hashCode()
            result = 31 * result + phone.hashCode()
            result = 31 * result + gender.hashCode()
            result = 31 * result + sexualOrientation.hashCode()
            result = 31 * result + originalFirstName.hashCode()
            result = 31 * result + originalLastName.hashCode()
            result = 31 * result + originalPhone.hashCode()
            result = 31 * result + originalGender.hashCode()
            result = 31 * result + originalSexualOrientation.hashCode()
            result = 31 * result + (originalProfileImageUrl?.hashCode() ?: 0)
            result = 31 * result + (selectedPhotoName?.hashCode() ?: 0)
            result = 31 * result + (selectedPhotoBytes?.contentHashCode() ?: 0)
            result = 31 * result + (errorMessage?.hashCode() ?: 0)
            result = 31 * result + isFormValid.hashCode()
            result = 31 * result + isDirty.hashCode()
            result = 31 * result + isSaveEnabled.hashCode()
            result = 31 * result + (dateOfBirth?.hashCode() ?: 0)
            result = 31 * result + (normalizedPhone?.hashCode() ?: 0)
            result = 31 * result + (previewImageModel?.hashCode() ?: 0)
            result = 31 * result + initials.hashCode()
            return result
        }
    }
}

private fun Throwable.userFacingMessage(): String {
    val message = message.orEmpty()
    return when {
        message.contains("Failed to connect", ignoreCase = true) -> "Couldn't save your profile"
        message.contains("Connection refused", ignoreCase = true) -> "Couldn't save your profile"
        message.contains("UnknownHost", ignoreCase = true) -> "Couldn't save your profile"
        message.isNotBlank() && !message.contains("Exception") -> message
        else -> "Couldn't save your profile"
    }
}

private fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Long =
    LocalDateTime(this, LocalTime(0, 0)).toInstant(timeZone).toEpochMilliseconds()
