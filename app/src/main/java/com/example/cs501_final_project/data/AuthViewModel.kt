package com.example.cs501_final_project.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_final_project.data.remote.CloudUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthSession(
    val userId: String,
    val displayName: String,
    val email: String = "",
    val identifier: String = email,
    val isEmergencyMode: Boolean = false,
    val signedInAt: Long = System.currentTimeMillis()
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var session by mutableStateOf<AuthSession?>(null)
        private set

    var message by mutableStateOf("")
        private set

    val errorMessage: String
        get() = message

    var isLoading by mutableStateOf(false)
        private set

    val isBusy: Boolean
        get() = isLoading

    init {
        loadCurrentUser()
    }

    private fun now(): Long {
        return System.currentTimeMillis()
    }

    private fun loadCurrentUser() {
        val user = auth.currentUser

        if (user == null) {
            session = null
            return
        }

        session = AuthSession(
            userId = user.uid,
            displayName = user.displayName ?: user.email ?: "User",
            email = user.email ?: "",
            identifier = user.email ?: "",
            isEmergencyMode = false,
            signedInAt = now()
        )

        viewModelScope.launch {
            ensureUserDocument()
        }
    }

    private suspend fun ensureUserDocument() {
        val user = auth.currentUser ?: return

        val userRef = db.collection("users").document(user.uid)
        val snapshot = userRef.get().await()

        val name = user.displayName ?: user.email ?: "User"
        val email = user.email ?: ""

        if (!snapshot.exists()) {
            val cloudUser = CloudUser(
                uid = user.uid,
                email = email,
                name = name,
                createdAt = now(),
                lastLoginAt = now(),
                updatedAt = now()
            )

            userRef.set(cloudUser).await()
        } else {
            val update = mapOf(
                "uid" to user.uid,
                "email" to email,
                "name" to name,
                "lastLoginAt" to now(),
                "updatedAt" to now()
            )

            userRef.set(update, SetOptions.merge()).await()
        }
    }

    fun login(
        email: String,
        password: String
    ) {
        if (email.isBlank() || password.isBlank()) {
            message = "Please enter email and password."
            return
        }

        viewModelScope.launch {
            isLoading = true
            message = ""

            try {
                val result = auth.signInWithEmailAndPassword(
                    email.trim(),
                    password
                ).await()

                val user = result.user

                if (user != null) {
                    ensureUserDocument()

                    session = AuthSession(
                        userId = user.uid,
                        displayName = user.displayName ?: user.email ?: "User",
                        email = user.email ?: "",
                        identifier = user.email ?: "",
                        isEmergencyMode = false,
                        signedInAt = now()
                    )

                    message = "Signed in."
                }
            } catch (e: Exception) {
                message = e.message ?: "Login failed."
            } finally {
                isLoading = false
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String
    ) {
        register(
            displayName = name,
            identifier = email,
            password = password,
            confirmPassword = password
        )
    }

    fun register(
        displayName: String,
        identifier: String,
        password: String,
        confirmPassword: String
    ) {
        if (displayName.isBlank() || identifier.isBlank() || password.isBlank()) {
            message = "Please fill in all fields."
            return
        }

        if (password != confirmPassword) {
            message = "Passwords do not match."
            return
        }

        viewModelScope.launch {
            isLoading = true
            message = ""

            try {
                val result = auth.createUserWithEmailAndPassword(
                    identifier.trim(),
                    password
                ).await()

                val user = result.user

                if (user != null) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()

                    user.updateProfile(profileUpdate).await()

                    val cloudUser = CloudUser(
                        uid = user.uid,
                        email = identifier.trim(),
                        name = displayName.trim(),
                        createdAt = now(),
                        lastLoginAt = now(),
                        updatedAt = now()
                    )

                    db.collection("users")
                        .document(user.uid)
                        .set(cloudUser)
                        .await()

                    session = AuthSession(
                        userId = user.uid,
                        displayName = displayName.trim(),
                        email = identifier.trim(),
                        identifier = identifier.trim(),
                        isEmergencyMode = false,
                        signedInAt = now()
                    )

                    message = "Account created."
                }
            } catch (e: Exception) {
                message = e.message ?: "Register failed."
            } finally {
                isLoading = false
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            message = "Please enter your email first."
            return
        }

        if (!email.contains("@")) {
            message = "Password reset requires an email address."
            return
        }

        viewModelScope.launch {
            isLoading = true
            message = ""

            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                message = "Password reset email sent. Please check your inbox."
            } catch (e: Exception) {
                message = e.message ?: "Could not send reset email."
            } finally {
                isLoading = false
            }
        }
    }

    fun updateCloudProfile(
        name: String,
        birthday: String = "",
        phone: String = ""
    ) {
        val user = auth.currentUser

        if (user == null) {
            message = "Please sign in first."
            return
        }

        viewModelScope.launch {
            isLoading = true
            message = ""

            try {
                val cleanName = name.trim().ifBlank {
                    user.displayName ?: user.email ?: "User"
                }

                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()

                user.updateProfile(profileUpdate).await()

                val update = mapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "name" to cleanName,
                    "birthday" to birthday.trim(),
                    "phone" to phone.trim(),
                    "updatedAt" to now()
                )

                db.collection("users")
                    .document(user.uid)
                    .set(update, SetOptions.merge())
                    .await()

                session = session?.copy(
                    displayName = cleanName
                )

                message = "Profile saved."
            } catch (e: Exception) {
                message = e.message ?: "Could not save profile."
            } finally {
                isLoading = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        session = null
        message = "Signed out."
    }

    fun continueAsGuest() {
        auth.signOut()

        session = AuthSession(
            userId = "emergency_guest_${now()}",
            displayName = "Emergency Guest",
            email = "",
            identifier = "emergency_guest",
            isEmergencyMode = true,
            signedInAt = now()
        )

        message = "Emergency mode enabled."
    }

    fun enterEmergencyMode() {
        continueAsGuest()
    }

    fun continueAsEmergencyGuest() {
        continueAsGuest()
    }

    fun signIn(
        email: String,
        password: String
    ) {
        login(email, password)
    }

    fun signUp(
        name: String,
        email: String,
        password: String
    ) {
        register(name, email, password)
    }

    fun createAccount(
        name: String,
        email: String,
        password: String
    ) {
        register(name, email, password)
    }

    fun clearMessage() {
        message = ""
    }

    fun clearError() {
        clearMessage()
    }
}