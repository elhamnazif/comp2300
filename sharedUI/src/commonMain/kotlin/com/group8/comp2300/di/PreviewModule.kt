package com.group8.comp2300.di

import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.EducationRepository
import com.group8.comp2300.domain.repository.ShopRepository
import com.group8.comp2300.mock.allQuizzes
import com.group8.comp2300.mock.educationContent
import com.group8.comp2300.mock.sampleClinics
import com.group8.comp2300.mock.sampleProducts
import com.group8.comp2300.presentation.screens.auth.AuthViewModel
import com.group8.comp2300.presentation.screens.education.EducationViewModel
import com.group8.comp2300.presentation.screens.medical.BookingViewModel
import com.group8.comp2300.presentation.screens.shop.ShopViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Fake repositories for Compose Preview support. */
class FakeClinicRepository : ClinicRepository {
    override fun getAllClinics(): List<Clinic> = sampleClinics
    override fun getClinicById(id: String): Clinic? = sampleClinics.find { it.id == id }
}

class FakeEducationRepository : EducationRepository {
    override fun getAllContent(): List<ContentItem> = educationContent
    override fun getContentById(id: String): ContentItem? = educationContent.find { it.id == id }
    override fun getQuizById(id: String): Quiz? = allQuizzes.find { it.id == id }
}

class FakeShopRepository : ShopRepository {
    override suspend fun getAllProducts(): List<Product> = sampleProducts
    override suspend fun getProductsByCategory(category: ProductCategory): List<Product> =
        if (category == ProductCategory.ALL) {
            sampleProducts
        } else {
            sampleProducts.filter { it.category == category }
        }

    override suspend fun getProductById(id: String): Product = sampleProducts.find { it.id == id } ?: sampleProducts[0]
}

class FakeAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    override suspend fun login(email: String, password: String): Result<User> {
        val user =
            User(
                id = "preview-user",
                email = email,
                firstName = "Preview",
                lastName = "User",
                gender = Gender.PREFER_NOT_TO_SAY,
                sexualOrientation = SexualOrientation.PREFER_NOT_TO_SAY,
                dateOfBirth = null
            )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?
    ): Result<User> {
        val user =
            User(
                id = "preview-user",
                email = email,
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                sexualOrientation = sexualOrientation,
                dateOfBirth = dateOfBirth
            )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override fun isGuest(): Boolean = _currentUser.value == null
}

/** Fake AuthViewModel for Compose Preview. */
class FakeAuthViewModel : com.group8.comp2300.presentation.screens.auth.AuthViewModel() {
    final override val state: StateFlow<State>
        field = MutableStateFlow(State())

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    override fun onEvent(event: AuthUiEvent) {
        /* no-op for preview */
    }

    override fun logout() {
        /* no-op */
    }

    override fun isGuest() = true
}

val previewModule = module {
    // Fake Repositories
    singleOf(::FakeClinicRepository) { bind<ClinicRepository>() }
    singleOf(::FakeEducationRepository) { bind<EducationRepository>() }
    singleOf(::FakeShopRepository) { bind<ShopRepository>() }
    singleOf(::FakeAuthRepository) { bind<AuthRepository>() }

    viewModelOf(::FakeAuthViewModel) { bind<com.group8.comp2300.presentation.screens.auth.AuthViewModel>() }
    viewModelOf(::ShopViewModel)
    viewModelOf(::BookingViewModel)
    viewModelOf(::EducationViewModel)
}
