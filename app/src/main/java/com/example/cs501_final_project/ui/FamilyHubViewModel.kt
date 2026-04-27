package com.example.cs501_final_project.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_final_project.data.remote.CloudFamily
import com.example.cs501_final_project.data.remote.CloudFamilyMember
import com.example.cs501_final_project.data.remote.CloudHealthRecord
import com.example.cs501_final_project.data.remote.CloudJoinRequest
import com.example.cs501_final_project.data.remote.RemoteFamilyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FamilyHubViewModel : ViewModel() {

    private val repo = RemoteFamilyRepository()

    var isSignedIn by mutableStateOf(repo.isSignedIn())
        private set

    var userEmail by mutableStateOf(repo.currentUserEmail())
        private set

    var message by mutableStateOf("")
        private set

    var families by mutableStateOf<List<CloudFamily>>(emptyList())
        private set

    var selectedFamily by mutableStateOf<CloudFamily?>(null)
        private set

    var members by mutableStateOf<List<CloudFamilyMember>>(emptyList())
        private set

    var records by mutableStateOf<List<CloudHealthRecord>>(emptyList())
        private set

    var joinRequests by mutableStateOf<List<CloudJoinRequest>>(emptyList())
        private set

    private var familyJob: Job? = null
    private var memberJob: Job? = null
    private var recordJob: Job? = null
    private var joinRequestJob: Job? = null

    fun start() {
        if (!isSignedIn) return
        if (familyJob != null) return

        familyJob = viewModelScope.launch {
            repo.listenMyFamilies().collect { list ->
                families = list

                if (selectedFamily == null && list.isNotEmpty()) {
                    selectFamily(list.first())
                }
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            try {
                repo.signUp(email, password, name)
                isSignedIn = true
                userEmail = repo.currentUserEmail()
                message = "Account created."
                start()
            } catch (e: Exception) {
                message = e.message ?: "Sign up failed."
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                repo.signIn(email, password)
                isSignedIn = true
                userEmail = repo.currentUserEmail()
                message = "Signed in."
                start()
            } catch (e: Exception) {
                message = e.message ?: "Sign in failed."
            }
        }
    }

    fun signOut() {
        repo.signOut()
        isSignedIn = false
        userEmail = ""
        message = "Signed out."
        families = emptyList()
        selectedFamily = null
        members = emptyList()
        records = emptyList()
        joinRequests = emptyList()

        familyJob?.cancel()
        memberJob?.cancel()
        recordJob?.cancel()
        joinRequestJob?.cancel()

        familyJob = null
        memberJob = null
        recordJob = null
        joinRequestJob = null
    }

    fun createFamily(familyName: String) {
        if (familyName.isBlank()) {
            message = "Family name is required."
            return
        }

        viewModelScope.launch {
            try {
                repo.createFamily(familyName)
                message = "Family created."
            } catch (e: Exception) {
                message = e.message ?: "Could not create family."
            }
        }
    }

    fun selectFamily(family: CloudFamily) {
        selectedFamily = family

        memberJob?.cancel()
        recordJob?.cancel()
        joinRequestJob?.cancel()

        memberJob = viewModelScope.launch {
            repo.listenFamilyMembers(family.id).collect {
                members = it
            }
        }

        recordJob = viewModelScope.launch {
            repo.listenHealthRecords(family.id).collect {
                records = it
            }
        }

        joinRequestJob = viewModelScope.launch {
            repo.listenJoinRequests(family.id).collect {
                joinRequests = it
            }
        }
    }

    fun addMember(name: String, relationship: String, birthday: String) {
        val family = selectedFamily ?: return

        if (name.isBlank()) {
            message = "Member name is required."
            return
        }

        viewModelScope.launch {
            try {
                repo.addFamilyMember(
                    familyId = family.id,
                    name = name,
                    relationship = relationship,
                    birthday = birthday
                )
                message = "Family member added."
            } catch (e: Exception) {
                message = e.message ?: "Could not add member."
            }
        }
    }

    fun addQuickRecord(member: CloudFamilyMember, symptoms: String, painLevel: Int) {
        val family = selectedFamily ?: return

        if (symptoms.isBlank()) {
            message = "Symptoms are required."
            return
        }

        val urgency = when {
            painLevel >= 8 -> "Emergency"
            painLevel >= 5 -> "Urgent Care"
            else -> "Primary Care"
        }

        viewModelScope.launch {
            try {
                repo.addHealthRecord(
                    familyId = family.id,
                    memberId = member.id,
                    type = "symptom",
                    title = "Symptom Check",
                    symptoms = symptoms,
                    bodyArea = "",
                    painLevel = painLevel,
                    urgency = urgency,
                    notes = "Added from Family Hub."
                )
                message = "Health record added."
            } catch (e: Exception) {
                message = e.message ?: "Could not add record."
            }
        }
    }

    fun requestToJoinFamily(familyId: String) {
        if (familyId.isBlank()) {
            message = "Family ID is required."
            return
        }

        viewModelScope.launch {
            try {
                repo.requestToJoinFamily(familyId)
                message = "Join request sent."
            } catch (e: Exception) {
                message = e.message ?: "Could not send join request."
            }
        }
    }

    fun approveRequest(request: CloudJoinRequest) {
        val family = selectedFamily ?: return

        viewModelScope.launch {
            try {
                repo.approveJoinRequest(
                    familyId = family.id,
                    request = request,
                    role = "editor"
                )
                message = "Request approved."
            } catch (e: Exception) {
                message = e.message ?: "Could not approve request."
            }
        }
    }

    fun rejectRequest(request: CloudJoinRequest) {
        val family = selectedFamily ?: return

        viewModelScope.launch {
            try {
                repo.rejectJoinRequest(family.id, request)
                message = "Request rejected."
            } catch (e: Exception) {
                message = e.message ?: "Could not reject request."
            }
        }
    }
}