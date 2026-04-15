package com.example.cs501_final_project.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.Calendar
import java.util.UUID

class CareRouteViewModel : ViewModel() {

    var selfProfile by mutableStateOf(
        PatientProfile(
            name = "You",
            birthDate = "",
            age = "",
            gender = "",
            phone = "",
            height = "",
            weight = "",
            address = "",
            allergies = "",
            medications = "",
            conditions = "",
            emergencyContact = ""
        )
    )
        private set

    var settings by mutableStateOf(AppSettings())
        private set

    var selectedPersonId by mutableStateOf("self")
        private set

    val familyMembers = mutableStateListOf<FamilyMember>()
    val historyRecords = mutableStateListOf<SavedCheckRecord>()

    fun updateSelfProfile(profile: PatientProfile) {
        selfProfile = profile
    }

    fun updateSettings(updated: AppSettings) {
        settings = updated
    }

    fun toggleNotifications(enabled: Boolean) {
        settings = settings.copy(notificationsEnabled = enabled)
    }

    fun toggleDarkMode(enabled: Boolean) {
        settings = settings.copy(darkModeEnabled = enabled)
    }

    fun updateAccentTheme(accentThemeOption: AccentThemeOption) {
        settings = settings.copy(accentTheme = accentThemeOption)
    }

    fun selectPerson(personId: String) {
        selectedPersonId = personId
    }

    fun activePatientContext(): PatientContext {
        val member = familyMembers.firstOrNull { it.id == selectedPersonId }
        return if (selectedPersonId == "self" || member == null) {
            PatientContext(
                id = "self",
                displayName = selfProfile.name.ifBlank { "You" },
                group = "Mine",
                age = selfProfile.age.ifBlank { calculateAgeFromBirthDate(selfProfile.birthDate) },
                birthDate = selfProfile.birthDate,
                gender = selfProfile.gender,
                phone = selfProfile.phone,
                height = selfProfile.height,
                weight = selfProfile.weight,
                address = selfProfile.address,
                allergies = selfProfile.allergies,
                medications = selfProfile.medications,
                conditions = selfProfile.conditions,
                emergencyContact = selfProfile.emergencyContact
            )
        } else {
            PatientContext(
                id = member.id,
                displayName = member.name,
                group = "Family",
                age = member.age.ifBlank { calculateAgeFromBirthDate(member.birthDate) },
                birthDate = member.birthDate,
                gender = member.gender,
                phone = "",
                height = "",
                weight = "",
                address = selfProfile.address,
                allergies = member.allergies,
                medications = member.medications,
                conditions = member.conditions,
                emergencyContact = selfProfile.emergencyContact
            )
        }
    }

    fun upsertFamilyMember(member: FamilyMember) {
        val index = familyMembers.indexOfFirst { it.id == member.id }
        if (index >= 0) {
            familyMembers[index] = member
        } else {
            familyMembers.add(member)
        }
    }

    fun createFamilyMember(
        name: String,
        relation: String,
        birthDate: String,
        age: String,
        gender: String,
        allergies: String,
        medications: String,
        conditions: String,
        notes: String
    ) {
        familyMembers.add(
            FamilyMember(
                id = UUID.randomUUID().toString(),
                name = name,
                relation = relation,
                birthDate = birthDate,
                age = age,
                gender = gender,
                allergies = allergies,
                medications = medications,
                conditions = conditions,
                notes = notes
            )
        )
    }

    fun deleteFamilyMember(memberId: String) {
        familyMembers.removeAll { it.id == memberId }
        if (selectedPersonId == memberId) {
            selectedPersonId = "self"
        }
        historyRecords.removeAll { it.personId == memberId }
    }

    fun clearFamilyMembers() {
        familyMembers.clear()
        selectedPersonId = "self"
        historyRecords.removeAll { it.personId != "self" }
    }

    fun addHistoryRecord(record: SavedCheckRecord) {
        historyRecords.add(0, record)
    }

    fun deleteHistoryRecord(recordId: String) {
        historyRecords.removeAll { it.id == recordId }
    }

    fun clearHistory() {
        historyRecords.clear()
    }

    fun recentHistorySummariesFor(personId: String): List<String> {
        return historyRecords
            .filter { it.personId == personId }
            .sortedByDescending { it.createdAt }
            .take(3)
            .map { "${it.bodyPart}: ${it.symptomText} (${it.urgency})" }
    }

    fun suggestedCheckupFocus(): List<String> {
        val active = activePatientContext()
        val suggestions = mutableListOf<String>()

        val ageNumber = active.age.toIntOrNull()
        if (ageNumber != null && ageNumber >= 40) {
            suggestions.add("Routine blood pressure and cholesterol check")
        } else {
            suggestions.add("General preventive wellness visit")
        }

        if (active.conditions.contains("asthma", ignoreCase = true)) {
            suggestions.add("Watch breathing triggers and refill inhaler plan")
        }
        if (active.conditions.contains("diabetes", ignoreCase = true)) {
            suggestions.add("Track glucose patterns and foot checks")
        }
        if (active.allergies.isNotBlank()) {
            suggestions.add("Keep allergy list ready before buying OTC medicine")
        }
        if (historyRecords.any { it.personId == active.id && it.bodyPart.contains("Chest", ignoreCase = true) }) {
            suggestions.add("Review repeat chest symptoms with a clinician")
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Build a baseline profile before your next symptom check")
        }

        return suggestions.take(3)
    }

    private fun calculateAgeFromBirthDate(birthDate: String): String {
        val digits = birthDate.filter { it.isDigit() }
        if (digits.length != 8) return ""

        val month = digits.substring(0, 2).toIntOrNull() ?: return ""
        val day = digits.substring(2, 4).toIntOrNull() ?: return ""
        val year = digits.substring(4, 8).toIntOrNull() ?: return ""

        if (month !in 1..12 || day !in 1..31 || year !in 1900..2100) return ""

        val today = Calendar.getInstance()
        val age = today.get(Calendar.YEAR) - year - if (
            today.get(Calendar.MONTH) + 1 < month ||
            (today.get(Calendar.MONTH) + 1 == month && today.get(Calendar.DAY_OF_MONTH) < day)
        ) 1 else 0

        return if (age >= 0) age.toString() else ""
    }
}
