package com.example.cs501_final_project.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_final_project.data.local.CareRouteDatabase
import com.example.cs501_final_project.data.local.toEntity
import com.example.cs501_final_project.data.local.toModel
import com.example.cs501_final_project.data.preferences.AppPreferencesRepository
import com.example.cs501_final_project.data.remote.CloudFamilyMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class CareRouteViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CareRouteDatabase.getInstance(application).careRouteDao()
    private val preferencesRepository = AppPreferencesRepository(application)
    private val legacyPrefs = application.getSharedPreferences(LEGACY_PREFS_NAME, Application.MODE_PRIVATE)
    private val gson = Gson()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var cloudUserId: String? = null
    private var dailyTipsByPerson: Map<String, DailyHealthTip> = emptyMap()

    var selfProfile by mutableStateOf(PatientProfile(name = "You"))
        private set

    var settings by mutableStateOf(AppSettings())
        private set

    var selectedPersonId by mutableStateOf("self")
        private set

    var dailyHealthTip by mutableStateOf<DailyHealthTip?>(null)
        private set

    private val _familyMembers = mutableStateListOf<FamilyMember>()
    val familyMembers: List<FamilyMember> get() = _familyMembers

    private val _historyRecords = mutableStateListOf<SavedCheckRecord>()
    val historyRecords: List<SavedCheckRecord> get() = _historyRecords

    private val _importedMedicalRecords = mutableStateListOf<ImportedMedicalRecord>()
    val importedMedicalRecords: List<ImportedMedicalRecord> get() = _importedMedicalRecords

    private val _checkupSuggestions = mutableStateListOf<PersonalizedCheckupSuggestion>()
    val checkupSuggestions: List<PersonalizedCheckupSuggestion> get() = _checkupSuggestions

    init {
        observeLocalData()
        viewModelScope.launch(Dispatchers.IO) {
            migrateLegacyDataIfNeeded()
        }
    }

    fun updateSelfProfile(profile: PatientProfile) {
        selfProfile = profile
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertSelfProfile(profile.toEntity())
            syncSelfProfileToCloud(profile)
        }
    }

    fun updateSettings(updated: AppSettings) {
        settings = updated
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertSettings(updated.toEntity())
            syncSettingsToCloud(updated)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        updateSettings(settings.copy(notificationsEnabled = enabled))
    }

    fun toggleDarkMode(enabled: Boolean) {
        updateSettings(settings.copy(darkModeEnabled = enabled))
    }

    fun updateAccentTheme(accentThemeOption: AccentThemeOption) {
        updateSettings(settings.copy(accentTheme = accentThemeOption))
    }

    fun selectPerson(personId: String) {
        selectedPersonId = personId
        dailyHealthTip = dailyTipsByPerson[personId]
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.setSelectedPersonId(personId)
        }
    }

    fun activePatientContext(): PatientContext {
        val member = _familyMembers.firstOrNull { it.id == selectedPersonId }
        return if (selectedPersonId == "self" || member == null) {
            PatientContext(
                id = "self",
                displayName = selfProfile.name.ifBlank { "You" },
                group = "Mine",
                age = selfProfile.age.ifBlank { calculateAgeFromBirthDate(selfProfile.birthDate) },
                birthDate = selfProfile.birthDate,
                gender = selfProfile.gender,
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
        val index = _familyMembers.indexOfFirst { it.id == member.id }
        if (index >= 0) {
            _familyMembers[index] = member
        } else {
            _familyMembers.add(member)
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertFamilyMember(member.toEntity())
            syncFamilyMemberToCloud(member)
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
        upsertFamilyMember(
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
        _familyMembers.removeAll { it.id == memberId }
        _historyRecords.removeAll { it.personId == memberId }
        _importedMedicalRecords.removeAll { it.personId == memberId }
        _checkupSuggestions.removeAll { it.personId == memberId }
        if (dailyHealthTip?.personId == memberId) {
            dailyHealthTip = null
        }
        dailyTipsByPerson = dailyTipsByPerson - memberId

        if (selectedPersonId == memberId) {
            selectPerson("self")
        }

        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteFamilyMember(memberId)
            dao.deleteHistoryRecordsForPerson(memberId)
            dao.deleteImportedMedicalRecordsForPerson(memberId)
            dao.deleteCheckupSuggestionsForPerson(memberId)
            dao.deleteDailyHealthTipForPerson(memberId)
            deleteCloudDocument("familyProfiles", memberId)
            deleteCloudDocumentsWherePersonId("historyRecords", memberId)
            deleteCloudDocumentsWherePersonId("importedMedicalRecords", memberId)
            deleteCloudDocumentsWherePersonId("checkupSuggestions", memberId)
            deleteCloudDocument("dailyHealthTips", memberId)
        }
    }

    fun addHistoryRecord(record: SavedCheckRecord) {
        _historyRecords.removeAll { it.id == record.id }
        _historyRecords.add(0, record)
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertHistoryRecord(record.toEntity())
            syncHistoryRecordToCloud(record)
        }
    }

    fun deleteHistoryRecord(recordId: String) {
        _historyRecords.removeAll { it.id == recordId }
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteHistoryRecord(recordId)
            deleteCloudDocument("historyRecords", recordId)
        }
    }

    fun addImportedMedicalRecord(
        person: PatientContext,
        sourceType: MedicalRecordSourceType,
        sourceLabel: String,
        title: String,
        summary: String,
        findings: List<String>,
        recommendedFollowUp: List<String>,
        rawText: String = ""
    ) {
        val record = ImportedMedicalRecord(
            id = UUID.randomUUID().toString(),
            personId = person.id,
            personName = person.displayName,
            sourceType = sourceType,
            sourceLabel = sourceLabel,
            title = title,
            summary = summary,
            findings = findings,
            recommendedFollowUp = recommendedFollowUp,
            rawText = rawText
        )
        _importedMedicalRecords.removeAll { it.id == record.id }
        _importedMedicalRecords.add(0, record)
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertImportedMedicalRecord(record.toEntity())
            syncImportedMedicalRecordToCloud(record)
        }
    }

    fun deleteImportedMedicalRecord(recordId: String) {
        _importedMedicalRecords.removeAll { it.id == recordId }
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteImportedMedicalRecord(recordId)
            deleteCloudDocument("importedMedicalRecords", recordId)
        }
    }

    fun clearHistoryArchive() {
        _historyRecords.clear()
        _importedMedicalRecords.clear()
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearHistoryRecords()
            dao.clearImportedMedicalRecords()
            clearCloudCollection("historyRecords")
            clearCloudCollection("importedMedicalRecords")
        }
    }

    fun recentHistorySummariesFor(personId: String): List<String> {
        return _historyRecords
            .filter { it.personId == personId }
            .sortedByDescending { it.createdAt }
            .take(4)
            .map { "${it.bodyPart}: ${it.symptomText} (${it.urgency})" }
    }

    fun recentImportedRecordSummariesFor(personId: String): List<String> {
        return _importedMedicalRecords
            .filter { it.personId == personId }
            .sortedByDescending { it.createdAt }
            .take(4)
            .map { "${it.title}: ${it.summary}" }
    }

    fun latestHistoryRecordFor(personId: String): SavedCheckRecord? {
        return _historyRecords
            .filter { it.personId == personId }
            .maxByOrNull { it.createdAt }
    }

    fun latestImportedRecordFor(personId: String): ImportedMedicalRecord? {
        return _importedMedicalRecords
            .filter { it.personId == personId }
            .maxByOrNull { it.createdAt }
    }

    fun updateDailyHealthTip(tip: DailyHealthTip) {
        dailyTipsByPerson = dailyTipsByPerson + (tip.personId to tip)
        if (tip.personId == selectedPersonId) {
            dailyHealthTip = tip
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertDailyHealthTip(tip.toEntity())
            syncDailyHealthTipToCloud(tip)
        }
    }

    fun shouldRefreshDailyTip(personId: String): Boolean {
        val cachedTip = dailyTipsByPerson[personId]
        val today = todayKey()
        return cachedTip == null || cachedTip.generatedDate != today
    }

    fun updateCheckupSuggestions(personId: String, suggestions: List<PersonalizedCheckupSuggestion>) {
        _checkupSuggestions.removeAll { it.personId == personId }
        _checkupSuggestions.addAll(0, suggestions)
        viewModelScope.launch(Dispatchers.IO) {
            dao.replaceCheckupSuggestionsForPerson(
                personId = personId,
                suggestions = suggestions.map { it.toEntity() }
            )
            deleteCloudDocumentsWherePersonId("checkupSuggestions", personId)
            suggestions.forEach { syncCheckupSuggestionToCloud(it) }
        }
    }

    fun getCheckupSuggestionsFor(personId: String): List<PersonalizedCheckupSuggestion> {
        return _checkupSuggestions
            .filter { it.personId == personId }
            .sortedByDescending { it.priority }
            .take(3)
    }

    fun shouldRefreshCheckupSuggestions(personId: String): Boolean {
        val suggestions = getCheckupSuggestionsFor(personId)
        return suggestions.isEmpty() || suggestions.first().generatedDate != todayKey()
    }

    fun suggestedCheckupFocus(): List<String> {
        val active = activePatientContext()
        val cached = getCheckupSuggestionsFor(active.id)
        if (cached.isNotEmpty()) {
            return cached.map { "${it.title} · ${it.timeframe}" }
        }

        val fallback = mutableListOf<String>()
        val ageNumber = active.age.toIntOrNull()
        if (ageNumber != null && ageNumber >= 40) {
            fallback.add("Routine blood pressure and cholesterol check")
        } else {
            fallback.add("General preventive wellness visit")
        }
        if (active.conditions.contains("asthma", ignoreCase = true)) {
            fallback.add("Breathing review and trigger plan")
        }
        if (active.conditions.contains("diabetes", ignoreCase = true)) {
            fallback.add("Glucose review and foot check")
        }
        if (_historyRecords.any { it.personId == active.id && it.bodyPart.contains("Chest", ignoreCase = true) }) {
            fallback.add("Review repeat chest symptoms with a clinician")
        }
        return fallback.take(3)
    }

    fun profileCompletionScore(): Int {
        var score = 0
        if (selfProfile.name.isNotBlank()) score += 20
        if (selfProfile.birthDate.isNotBlank()) score += 15
        if (selfProfile.gender.isNotBlank()) score += 15
        if (selfProfile.conditions.isNotBlank()) score += 15
        if (selfProfile.allergies.isNotBlank()) score += 15
        if (selfProfile.medications.isNotBlank()) score += 10
        if (selfProfile.emergencyContact.isNotBlank()) score += 5
        return score.coerceAtMost(100)
    }

    fun archiveRecordCountFor(personId: String): Int {
        return _historyRecords.count { it.personId == personId } +
                _importedMedicalRecords.count { it.personId == personId }
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            preferencesRepository.selectedPersonIdFlow.collect { personId ->
                selectedPersonId = personId.ifBlank { "self" }
                dailyHealthTip = dailyTipsByPerson[selectedPersonId]
            }
        }

        viewModelScope.launch {
            dao.observeSelfProfile().collect { entity ->
                selfProfile = entity?.toModel() ?: PatientProfile(name = "You")
            }
        }

        viewModelScope.launch {
            dao.observeSettings().collect { entity ->
                settings = entity?.toModel() ?: AppSettings()
            }
        }

        viewModelScope.launch {
            dao.observeFamilyMembers().collect { members ->
                _familyMembers.replaceAllWith(members.map { it.toModel() })
            }
        }

        viewModelScope.launch {
            dao.observeHistoryRecords().collect { records ->
                _historyRecords.replaceAllWith(records.map { it.toModel() })
            }
        }

        viewModelScope.launch {
            dao.observeImportedMedicalRecords().collect { records ->
                _importedMedicalRecords.replaceAllWith(records.map { it.toModel() })
            }
        }

        viewModelScope.launch {
            dao.observeDailyHealthTips().collect { tips ->
                dailyTipsByPerson = tips
                    .map { it.toModel() }
                    .associateBy { it.personId }
                dailyHealthTip = dailyTipsByPerson[selectedPersonId]
            }
        }

        viewModelScope.launch {
            dao.observeCheckupSuggestions().collect { suggestions ->
                _checkupSuggestions.replaceAllWith(
                    suggestions.map { it.toModel() }
                )
            }
        }
    }

    private suspend fun migrateLegacyDataIfNeeded() {
        if (preferencesRepository.isLegacyMigrationCompleted()) return

        val hasLegacyData = legacyPrefs.contains(KEY_SELF_PROFILE) ||
                legacyPrefs.contains(KEY_SETTINGS) ||
                legacyPrefs.contains(KEY_SELECTED_PERSON) ||
                legacyPrefs.contains(KEY_DAILY_TIP) ||
                legacyPrefs.contains(KEY_FAMILY_MEMBERS) ||
                legacyPrefs.contains(KEY_HISTORY_RECORDS) ||
                legacyPrefs.contains(KEY_IMPORTED_RECORDS) ||
                legacyPrefs.contains(KEY_CHECKUP_SUGGESTIONS)

        if (hasLegacyData) {
            dao.upsertSelfProfile(loadLegacySelfProfile().toEntity())
            dao.upsertSettings(loadLegacySettings().toEntity())

            val familyMembers = loadLegacyFamilyMembers()
            if (familyMembers.isNotEmpty()) {
                dao.upsertFamilyMembers(familyMembers.map { it.toEntity() })
            }

            val historyRecords = loadLegacyHistoryRecords()
            if (historyRecords.isNotEmpty()) {
                dao.upsertHistoryRecords(historyRecords.map { it.toEntity() })
            }

            val importedRecords = loadLegacyImportedMedicalRecords()
            if (importedRecords.isNotEmpty()) {
                dao.upsertImportedMedicalRecords(importedRecords.map { it.toEntity() })
            }

            loadLegacyDailyHealthTip()?.let { dao.upsertDailyHealthTip(it.toEntity()) }

            val suggestions = loadLegacyCheckupSuggestions()
            suggestions.groupBy { it.personId }.forEach { (personId, personSuggestions) ->
                dao.replaceCheckupSuggestionsForPerson(
                    personId = personId,
                    suggestions = personSuggestions.map { it.toEntity() }
                )
            }

            preferencesRepository.setSelectedPersonId(loadLegacySelectedPersonId())
        }

        preferencesRepository.markLegacyMigrationCompleted()
    }


    fun connectCloudUser(
        userId: String,
        email: String,
        displayName: String
    ) {
        if (userId.isBlank() || userId.startsWith("emergency_guest")) {
            disconnectCloudUser()
            return
        }

        if (cloudUserId == userId) return
        cloudUserId = userId

        viewModelScope.launch(Dispatchers.IO) {
            ensureCloudUserDocument(
                userId = userId,
                email = email,
                displayName = displayName
            )
            loadSelfProfileFromCloud(userId)
            syncAllUserDataToCloud(userId)
        }
    }

    fun disconnectCloudUser() {
        cloudUserId = null
    }

    fun mimicCloudFamilyMember(member: CloudFamilyMember) {
        val memberId = member.id.ifBlank {
            member.linkedUserUid.ifBlank { UUID.randomUUID().toString() }
        }

        val localMember = FamilyMember(
            id = memberId,
            name = member.name.ifBlank { member.email.ifBlank { "Family Member" } },
            relation = member.relationship.ifBlank { "Family" },
            birthDate = member.birthDate,
            age = member.age,
            gender = member.gender,
            allergies = member.allergies,
            medications = member.medications,
            conditions = member.conditions,
            notes = member.linkedUserEmail.ifBlank { member.email }
        )

        upsertFamilyMember(localMember)
        selectPerson(localMember.id)
    }

    private fun activeCloudUid(): String? {
        return cloudUserId ?: auth.currentUser?.uid
    }

    private suspend fun safeCloudWrite(action: suspend () -> Unit) {
        try {
            action()
        } catch (_: Exception) {
            // Local Room data remains the source of offline truth if cloud sync is temporarily unavailable.
        }
    }

    private suspend fun ensureCloudUserDocument(
        userId: String,
        email: String,
        displayName: String
    ) = safeCloudWrite {
        val userRef = db.collection("users").document(userId)
        val snapshot = userRef.get().await()
        val time = System.currentTimeMillis()
        val cleanName = displayName.ifBlank { email.ifBlank { "User" } }

        val data = if (snapshot.exists()) {
            mapOf(
                "uid" to userId,
                "email" to email,
                "name" to cleanName,
                "lastLoginAt" to time,
                "updatedAt" to time
            )
        } else {
            mapOf(
                "uid" to userId,
                "email" to email,
                "name" to cleanName,
                "birthDate" to "",
                "age" to "",
                "gender" to "",
                "height" to "",
                "weight" to "",
                "address" to "",
                "allergies" to "",
                "medications" to "",
                "conditions" to "",
                "emergencyContact" to "",
                "selectedFamilyId" to "",
                "familyCode" to "",
                "createdAt" to time,
                "lastLoginAt" to time,
                "updatedAt" to time
            )
        }

        userRef.set(data, SetOptions.merge()).await()
    }

    private suspend fun loadSelfProfileFromCloud(userId: String) = safeCloudWrite {
        val snapshot = db.collection("users").document(userId).get().await()
        if (!snapshot.exists()) return@safeCloudWrite

        val cloudProfile = PatientProfile(
            name = snapshot.getString("name").orEmpty().ifBlank { selfProfile.name },
            birthDate = snapshot.getString("birthDate").orEmpty().ifBlank {
                snapshot.getString("birthday").orEmpty().ifBlank { selfProfile.birthDate }
            },
            age = snapshot.getString("age").orEmpty().ifBlank { selfProfile.age },
            gender = snapshot.getString("gender").orEmpty().ifBlank { selfProfile.gender },
            height = snapshot.getString("height").orEmpty().ifBlank { selfProfile.height },
            weight = snapshot.getString("weight").orEmpty().ifBlank { selfProfile.weight },
            address = snapshot.getString("address").orEmpty().ifBlank { selfProfile.address },
            allergies = snapshot.getString("allergies").orEmpty().ifBlank { selfProfile.allergies },
            medications = snapshot.getString("medications").orEmpty().ifBlank { selfProfile.medications },
            conditions = snapshot.getString("conditions").orEmpty().ifBlank { selfProfile.conditions },
            emergencyContact = snapshot.getString("emergencyContact").orEmpty().ifBlank { selfProfile.emergencyContact }
        )

        withContext(Dispatchers.Main) {
            selfProfile = cloudProfile
        }
        dao.upsertSelfProfile(cloudProfile.toEntity())
    }

    private suspend fun syncAllUserDataToCloud(userId: String) = safeCloudWrite {
        syncSelfProfileToCloud(selfProfile, userId)
        syncSettingsToCloud(settings, userId)
        _familyMembers.forEach { syncFamilyMemberToCloud(it, userId) }
        _historyRecords.forEach { syncHistoryRecordToCloud(it, userId) }
        _importedMedicalRecords.forEach { syncImportedMedicalRecordToCloud(it, userId) }
        dailyTipsByPerson.values.forEach { syncDailyHealthTipToCloud(it, userId) }
        _checkupSuggestions.forEach { syncCheckupSuggestionToCloud(it, userId) }
    }

    private suspend fun syncSelfProfileToCloud(
        profile: PatientProfile,
        explicitUserId: String? = null
    ) = safeCloudWrite {
        val uid = explicitUserId ?: activeCloudUid() ?: return@safeCloudWrite
        val user = auth.currentUser
        val email = user?.email.orEmpty()
        val time = System.currentTimeMillis()
        val cleanName = profile.name.ifBlank { user?.displayName ?: email.ifBlank { "User" } }

        val data = mapOf(
            "uid" to uid,
            "email" to email,
            "name" to cleanName,
            "birthDate" to profile.birthDate,
            "age" to profile.age,
            "gender" to profile.gender,
            "height" to profile.height,
            "weight" to profile.weight,
            "address" to profile.address,
            "allergies" to profile.allergies,
            "medications" to profile.medications,
            "conditions" to profile.conditions,
            "emergencyContact" to profile.emergencyContact,
            "updatedAt" to time
        )

        val userRef = db.collection("users").document(uid)
        userRef.set(data, SetOptions.merge()).await()

        val selectedFamilyId = userRef.get().await().getString("selectedFamilyId").orEmpty()
        if (selectedFamilyId.isNotBlank()) {
            db.collection("families")
                .document(selectedFamilyId)
                .collection("members")
                .document(uid)
                .set(
                    profile.toLinkedFamilyMemberCloudMap(
                        uid = uid,
                        email = email,
                        name = cleanName,
                        updatedAt = time
                    ),
                    SetOptions.merge()
                )
                .await()
        }
    }

    private fun PatientProfile.toLinkedFamilyMemberCloudMap(
        uid: String,
        email: String,
        name: String,
        updatedAt: Long
    ): Map<String, Any> {
        return mapOf(
            "id" to uid,
            "name" to name,
            "email" to email,
            "birthDate" to birthDate,
            "age" to age,
            "gender" to gender,
            "height" to height,
            "weight" to weight,
            "address" to address,
            "allergies" to allergies,
            "medications" to medications,
            "conditions" to conditions,
            "emergencyContact" to emergencyContact,
            "linkedUserUid" to uid,
            "linkedUserEmail" to email,
            "updatedAt" to updatedAt
        )
    }

    private suspend fun syncSettingsToCloud(
        updated: AppSettings,
        explicitUserId: String? = null
    ) = safeCloudWrite {
        val uid = explicitUserId ?: activeCloudUid() ?: return@safeCloudWrite
        db.collection("users")
            .document(uid)
            .collection("settings")
            .document("default")
            .set(
                mapOf(
                    "notificationsEnabled" to updated.notificationsEnabled,
                    "darkModeEnabled" to updated.darkModeEnabled,
                    "accentTheme" to updated.accentTheme.name,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun syncFamilyMemberToCloud(
        member: FamilyMember,
        explicitUserId: String? = null
    ) = safeCloudWrite {
        val uid = explicitUserId ?: activeCloudUid() ?: return@safeCloudWrite
        db.collection("users")
            .document(uid)
            .collection("familyProfiles")
            .document(member.id)
            .set(
                mapOf(
                    "id" to member.id,
                    "name" to member.name,
                    "relation" to member.relation,
                    "birthDate" to member.birthDate,
                    "age" to member.age,
                    "gender" to member.gender,
                    "allergies" to member.allergies,
                    "medications" to member.medications,
                    "conditions" to member.conditions,
                    "notes" to member.notes,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun syncHistoryRecordToCloud(
        record: SavedCheckRecord,
        explicitUserId: String? = null
    ) = safeCloudWrite {
        val uid = explicitUserId ?: activeCloudUid() ?: return@safeCloudWrite
        db.collection("users")
            .document(uid)
            .collection("historyRecords")
            .document(record.id)
            .set(
                mapOf(
                    "id" to record.id,
                    "personId" to record.personId,
                    "personName" to record.personName,
                    "personGroup" to record.personGroup,
                    "bodyPart" to record.bodyPart,
                    "symptomText" to record.symptomText,
                    "painLevel" to record.painLevel,
                    "urgency" to record.urgency,
                    "careLevel" to record.careLevel,
                    "summary" to record.summary,
                    "mapQuery" to record.mapQuery,
                    "createdAt" to record.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun syncImportedMedicalRecordToCloud(
        record: ImportedMedicalRecord,
        explicitUserId: String? = null
    ) = safeCloudWrite {
        val uid = explicitUserId ?: activeCloudUid() ?: return@safeCloudWrite
        db.collection("users")
            .document(uid)
            .collection("importedMedicalRecords")
            .document(record.id)
            .set(
                mapOf(
                    "id" to record.id,
                    "personId" to record.personId,
                    "personName" to record.personName,
                    "sourceType" to record.sourceType.name,
                    "sourceLabel" to record.sourceLabel,
                    "title" to record.title,
                    "summary" to record.summary,
                    "findings" to record.findings,
                    "recommendedFollowUp" to record.recommendedFollowUp,
                    "rawText" to record.rawText,
                    "createdAt" to record.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun syncDailyHealthTipToCloud(
        tip: DailyHealthTip,
        explicitUserId: String? = null
    ) = safeCloudWrite {
        val uid = explicitUserId ?: activeCloudUid() ?: return@safeCloudWrite
        db.collection("users")
            .document(uid)
            .collection("dailyHealthTips")
            .document(tip.personId)
            .set(
                mapOf(
                    "personId" to tip.personId,
                    "title" to tip.title,
                    "message" to tip.message,
                    "focusArea" to tip.focusArea,
                    "caution" to tip.caution,
                    "generatedDate" to tip.generatedDate,
                    "source" to tip.source,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun syncCheckupSuggestionToCloud(
        suggestion: PersonalizedCheckupSuggestion,
        explicitUserId: String? = null
    ) = safeCloudWrite {
        val uid = explicitUserId ?: activeCloudUid() ?: return@safeCloudWrite
        db.collection("users")
            .document(uid)
            .collection("checkupSuggestions")
            .document(suggestion.id)
            .set(
                mapOf(
                    "id" to suggestion.id,
                    "title" to suggestion.title,
                    "reason" to suggestion.reason,
                    "timeframe" to suggestion.timeframe,
                    "priority" to suggestion.priority,
                    "personId" to suggestion.personId,
                    "generatedDate" to suggestion.generatedDate,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun deleteCloudDocument(
        collection: String,
        documentId: String
    ) = safeCloudWrite {
        val uid = activeCloudUid() ?: return@safeCloudWrite
        db.collection("users")
            .document(uid)
            .collection(collection)
            .document(documentId)
            .delete()
            .await()
    }

    private suspend fun deleteCloudDocumentsWherePersonId(
        collection: String,
        personId: String
    ) = safeCloudWrite {
        val uid = activeCloudUid() ?: return@safeCloudWrite
        val snapshot = db.collection("users")
            .document(uid)
            .collection(collection)
            .whereEqualTo("personId", personId)
            .get()
            .await()

        val batch = db.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    private suspend fun clearCloudCollection(collection: String) = safeCloudWrite {
        val uid = activeCloudUid() ?: return@safeCloudWrite
        val snapshot = db.collection("users")
            .document(uid)
            .collection(collection)
            .get()
            .await()

        val batch = db.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    private fun todayKey(): String {
        val calendar = Calendar.getInstance()
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val year = calendar.get(Calendar.YEAR)
        return "$year-$month-$day"
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

    private fun loadLegacySelfProfile(): PatientProfile {
        val json = legacyPrefs.getString(KEY_SELF_PROFILE, null) ?: return PatientProfile(name = "You")
        return runCatching { gson.fromJson(json, PatientProfile::class.java) }
            .getOrDefault(PatientProfile(name = "You"))
    }

    private fun loadLegacySettings(): AppSettings {
        val json = legacyPrefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
        return runCatching { gson.fromJson(json, AppSettings::class.java) }
            .getOrDefault(AppSettings())
    }

    private fun loadLegacySelectedPersonId(): String {
        return legacyPrefs.getString(KEY_SELECTED_PERSON, "self") ?: "self"
    }

    private fun loadLegacyDailyHealthTip(): DailyHealthTip? {
        val json = legacyPrefs.getString(KEY_DAILY_TIP, null) ?: return null
        return runCatching { gson.fromJson(json, DailyHealthTip::class.java) }.getOrNull()
    }

    private fun loadLegacyFamilyMembers(): List<FamilyMember> {
        val json = legacyPrefs.getString(KEY_FAMILY_MEMBERS, null) ?: return emptyList()
        val type = object : TypeToken<List<FamilyMember>>() {}.type
        return runCatching { gson.fromJson<List<FamilyMember>>(json, type) }.getOrDefault(emptyList())
    }

    private fun loadLegacyHistoryRecords(): List<SavedCheckRecord> {
        val json = legacyPrefs.getString(KEY_HISTORY_RECORDS, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedCheckRecord>>() {}.type
        return runCatching { gson.fromJson<List<SavedCheckRecord>>(json, type) }.getOrDefault(emptyList())
    }

    private fun loadLegacyImportedMedicalRecords(): List<ImportedMedicalRecord> {
        val json = legacyPrefs.getString(KEY_IMPORTED_RECORDS, null) ?: return emptyList()
        val type = object : TypeToken<List<ImportedMedicalRecord>>() {}.type
        return runCatching { gson.fromJson<List<ImportedMedicalRecord>>(json, type) }.getOrDefault(emptyList())
    }

    private fun loadLegacyCheckupSuggestions(): List<PersonalizedCheckupSuggestion> {
        val json = legacyPrefs.getString(KEY_CHECKUP_SUGGESTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<PersonalizedCheckupSuggestion>>() {}.type
        return runCatching { gson.fromJson<List<PersonalizedCheckupSuggestion>>(json, type) }
            .getOrDefault(emptyList())
    }

    private fun <T> SnapshotStateList<T>.replaceAllWith(items: List<T>) {
        clear()
        addAll(items)
    }

    private companion object {
        const val LEGACY_PREFS_NAME = "care_route_data"
        const val KEY_SELF_PROFILE = "self_profile"
        const val KEY_SETTINGS = "app_settings"
        const val KEY_SELECTED_PERSON = "selected_person_id"
        const val KEY_DAILY_TIP = "daily_health_tip"
        const val KEY_FAMILY_MEMBERS = "family_members"
        const val KEY_HISTORY_RECORDS = "history_records"
        const val KEY_IMPORTED_RECORDS = "imported_medical_records"
        const val KEY_CHECKUP_SUGGESTIONS = "checkup_suggestions"
    }
}