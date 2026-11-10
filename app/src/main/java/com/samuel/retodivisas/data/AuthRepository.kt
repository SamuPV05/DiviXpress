package com.samuel.retodivisas.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun hasFirebaseUser(): Boolean = auth.currentUser != null

    suspend fun checkAndLoadActiveSession(): AppUser? {
        val fbUser = auth.currentUser ?: return null
        val uid = fbUser.uid
        val doc = firestore.collection("users").document(uid).get().await()
        return if (doc.exists()) {
            val user = doc.toObject(AppUser::class.java)?.copy(uid = uid)
            SessionManager.currentUser = user
            user
        } else {
            val newUser = AppUser(
                uid = uid,
                email = fbUser.email ?: "",
                role = ROLE_USER,
                balanceUsd = 10000.0
            )
            firestore.collection("users").document(uid).set(newUser).await()
            SessionManager.currentUser = newUser
            newUser
        }
    }

    suspend fun login(email: String, password: String): Result<AppUser> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val result = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val fbUser = result.user ?: throw IllegalStateException("No se obtuvo información del usuario")
            val uid = fbUser.uid

            val docRef = firestore.collection("users").document(uid)
            val doc = docRef.get().await()

            val appUser = if (doc.exists()) {
                doc.toObject(AppUser::class.java)?.copy(uid = uid)
                    ?: AppUser(uid = uid, email = cleanEmail, role = ROLE_USER)
            } else {
                val newUser = AppUser(
                    uid = uid,
                    email = cleanEmail,
                    role = ROLE_USER,
                    balanceUsd = 10000.0
                )
                docRef.set(newUser).await()
                newUser
            }

            SessionManager.currentUser = appUser
            Result.success(appUser)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthErrorMessage(e)))
        }
    }

    suspend fun register(
        email: String,
        password: String,
        selectedRole: String = ROLE_USER
    ): Result<AppUser> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val fbUser = result.user ?: throw IllegalStateException("Error al registrar usuario")
            val uid = fbUser.uid

            val role = if (selectedRole == ROLE_ADMIN) ROLE_ADMIN else ROLE_USER
            val appUser = AppUser(
                uid = uid,
                email = cleanEmail,
                role = role,
                balanceUsd = 10000.0
            )

            firestore.collection("users").document(uid).set(appUser).await()

            SessionManager.currentUser = appUser
            Result.success(appUser)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthErrorMessage(e)))
        }
    }

    fun logout() {
        auth.signOut()
        SessionManager.logout()
    }

    private fun mapAuthErrorMessage(e: Exception): String {
        return when (e) {
            is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado"
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
            is FirebaseAuthInvalidUserException -> "Correo o contraseña incorrectos"
            is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres"
            else -> e.localizedMessage ?: "Ocurrió un error al autenticar"
        }
    }
}
