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
import kotlinx.coroutines.flow.catch
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
            repo.listenMyFamilies()
                .catch { e ->
                    message = e.message ?: "Could not load families."
                }
                .collect { list ->
                    families = list

                    if (selectedFamily == null && list.isNotEmpty()) {
                        selectFamily(list.first())
                    }
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

    fun signOut() {
        repo.signOut()

        familyJob?.cancel()
        memberJob?.cancel()
        recordJob?.cancel()
        joinRequestJob?.cancel()

        familyJob = null
        memberJob = null
        recordJob = null
        joinRequestJob = null

        isSignedIn = false
        userEmail = ""
        message = "Signed out."
        families = emptyList()
        selectedFamily = null
        members = emptyList()
        records = emptyList()
        joinRequests = emptyList()
    }

    fun createFamily(name: String) {
        viewModelScope.launch {
            try {
                val familyId = repo.createFamily(name)
                message = "Family created: $familyId"
            } catch (e: Exception) {
                message = e.message ?: "Create family failed."
            }
        }
    }

    fun selectFamily(family: CloudFamily) {
        selectedFamily = family

        memberJob?.cancel()
        recordJob?.cancel()
        joinRequestJob?.cancel()

        memberJob = viewModelScope.launch {
            repo.listenFamilyMembers(family.id)
                .catch { e ->
                    message = e.message ?: "Could not load members."
                }
                .collect {
                    members = it
                }
        }

        recordJob = viewModelScope.launch {
            repo.listenHealthRecords(family.id)
                .catch { e ->
                    message = e.message ?: "Could not load records."
                }
                .collect {
                    records = it
                }
        }

        joinRequestJob = viewModelScope.launch {
            repo.listenJoinRequests(family.id)
                .catch { e ->
                    message = e.message ?: "Could not load join requests."
                }
                .collect {
                    joinRequests = it
                }
        }
    }

    fun addMember(name: String, relation: String, birthday: String) {
        val family = selectedFamily ?: return

        viewModelScope.launch {
            try {
                repo.addFamilyMember(family.id, name, relation, birthday)
                message = "Member added."
            } catch (e: Exception) {
                message = e.message ?: "Add member failed."
            }
        }
    }

    fun requestToJoinFamily(familyId: String) {
        viewModelScope.launch {
            try {
                repo.requestToJoinFamily(familyId)
                message = "Join request sent."
            } catch (e: Exception) {
                message = e.message ?: "Join failed."
            }
        }
    }

    fun addQuickRecord(member: CloudFamilyMember, symptoms: String, pain: Int) {
        val family = selectedFamily ?: return

        val urgency = when {
            pain >= 8 -> "Emergency"
            pain >= 5 -> "Urgent"
            else -> "Normal"
        }

        viewModelScope.launch {
            try {
                repo.addHealthRecord(
                    familyId = family.id,
                    memberId = member.id,
                    type = "symptom",
                    title = "Quick Record",
                    symptoms = symptoms,
                    bodyArea = "",
                    painLevel = pain,
                    urgency = urgency,
                    notes = ""
                )

                message = "Record saved."
            } catch (e: Exception) {
                message = e.message ?: "Save record failed."
            }
        }
    }

    fun approveRequest(request: CloudJoinRequest) {
        val family = selectedFamily ?: return

        viewModelScope.launch {
            try {
                repo.approveJoinRequest(family.id, request)
                message = "Request approved."
            } catch (e: Exception) {
                message = e.message ?: "Approve failed."
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
                message = e.message ?: "Reject failed."
            }
        }
    }
}