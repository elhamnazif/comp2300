package com.group8.comp2300.domain.model.session

import com.group8.comp2300.domain.model.user.User

sealed interface AuthSession {
    data object Restoring : AuthSession

    data object SignedOut : AuthSession

    data class SignedIn(val user: User) : AuthSession
}

val AuthSession.userOrNull: User?
    get() = (this as? AuthSession.SignedIn)?.user

val AuthSession.isSignedIn: Boolean
    get() = this is AuthSession.SignedIn
