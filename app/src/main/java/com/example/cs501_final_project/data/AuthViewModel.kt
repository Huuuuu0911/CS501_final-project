package com.example.cs501_final_project.data

import android.app.Application
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("care_route_auth", Application.MODE_PRIVATE)
    private val gson = Gson()

    var session by mutableStateOf<AuthSession?>(loadSession())
        private set

    var isBusy by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun register(
        displayName: String,
        identifier: String,
        password: String,
        confirmPassword: String
    ) {
        val safeName = displayName.trim()
        val safeIdentifier = identifier.trim().lowercase()

        when {
            safeName.isBlank() -> {
                errorMessage = "Please enter your name."
                return
            }
            !isValidIdentifier(safeIdentifier) -> {
                errorMessage = "Please enter a valid email or phone number."
                return
            }
            password.length < 6 -> {
                errorMessage = "Password must be at least 6 characters."
                return
            }
            password != confirmPassword -> {
                errorMessage = "Passwords do not match."
                return
            }
        }

        isBusy = true
        val users = loadUsers().toMutableList()
        val exists = users.any { it.identifier == safeIdentifier }

        if (exists) {
            isBusy = false
            errorMessage = "This email or phone is already registered."
            return
        }

        val newUser = StoredUser(
            id = UUID.randomUUID().toString(),
            displayName = safeName,
            identifier = safeIdentifier,
            passwordHash = sha256(password),
            createdAt = System.currentTimeMillis()
        )

        users.add(newUser)
        saveUsers(users)

        session = AuthSession(
            userId = newUser.id,
            displayName = newUser.displayName,
            identifier = newUser.identifier,
            isEmergencyMode = false,
            signedInAt = System.currentTimeMillis()
        )
        saveSession(session)
        isBusy = false
        errorMessage = null
    }

    fun login(identifier: String, password: String) {
        val safeIdentifier = identifier.trim().lowercase()

        when {
            !isValidIdentifier(safeIdentifier) -> {
                errorMessage = "Please enter a valid email or phone number."
                return
            }
            password.isBlank() -> {
                errorMessage = "Please enter your password."
                return
            }
        }

        isBusy = true
        val hashedPassword = sha256(password)
        val user = loadUsers().firstOrNull {
            it.identifier == safeIdentifier && it.passwordHash == hashedPassword
        }

        if (user == null) {
            isBusy = false
            errorMessage = "Wrong account or password."
            return
        }

        session = AuthSession(
            userId = user.id,
            displayName = user.displayName,
            identifier = user.identifier,
            isEmergencyMode = false,
            signedInAt = System.currentTimeMillis()
        )
        saveSession(session)
        isBusy = false
        errorMessage = null
    }

    fun enterEmergencyMode() {
        session = AuthSession(
            userId = "emergency_guest",
            displayName = "Emergency Guest",
            identifier = "guest",
            isEmergencyMode = true,
            signedInAt = System.currentTimeMillis()
        )
        saveSession(session)
        errorMessage = null
    }

    fun logout() {
        session = null
        prefs.edit().remove(KEY_SESSION).apply()
        errorMessage = null
    }

    fun clearError() {
        errorMessage = null
    }

    private fun isValidIdentifier(value: String): Boolean {
        if (value.isBlank()) return false
        val looksLikeEmail = Patterns.EMAIL_ADDRESS.matcher(value).matches()
        val digitCount = value.filter { it.isDigit() }.length
        val looksLikePhone = digitCount >= 8
        return looksLikeEmail || looksLikePhone
    }

    private fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun loadUsers(): List<StoredUser> {
        val json = prefs.getString(KEY_USERS, null) ?: return emptyList()
        val type = object : TypeToken<List<StoredUser>>() {}.type
        return runCatching { gson.fromJson<List<StoredUser>>(json, type) }.getOrDefault(emptyList())
    }

    private fun saveUsers(users: List<StoredUser>) {
        prefs.edit().putString(KEY_USERS, gson.toJson(users)).apply()
    }

    private fun loadSession(): AuthSession? {
        val json = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { gson.fromJson(json, AuthSession::class.java) }.getOrNull()
    }

    private fun saveSession(session: AuthSession?) {
        if (session == null) {
            prefs.edit().remove(KEY_SESSION).apply()
        } else {
            prefs.edit().putString(KEY_SESSION, gson.toJson(session)).apply()
        }
    }

    private companion object {
        const val KEY_USERS = "registered_users"
        const val KEY_SESSION = "active_session"
    }
}
