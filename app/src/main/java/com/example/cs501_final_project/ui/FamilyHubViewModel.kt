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

    var currentUserUid by mutableStateOf(repo.currentUserUid())
        private set

    var userEmail by mutableStateOf(repo.currentUserEmail())
        private set

    var message by mutableStateOf("")
        private set

    var isBusy by mutableStateOf(false)
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

    val hasFamily: Boolean
        get() = selectedFamily != null

    val canManageJoinRequests: Boolean
        get() = selectedFamily?.ownerUid == currentUserUid

    private var familyJob: Job? = null
    private var memberJob: Job? = null
    private var recordJob: Job? = null
    private var joinRequestJob: Job? = null

    fun start() {
        isSignedIn = repo.isSignedIn()
        currentUserUid = repo.currentUserUid()
        userEmail = repo.currentUserEmail()

        if (!isSignedIn) return
        if (familyJob != null) return

        familyJob = viewModelScope.launch {
            repo.listenMyFamilies()
                .catch { e ->
                    message = e.message ?: "Could not load family."
                }
                .collect { list ->
                    families = list.take(1)

                    val nextFamily = families.firstOrNull { it.id == selectedFamily?.id }
                        ?: families.firstOrNull()

                    if (nextFamily == null) {
                        clearActiveFamily()
                    } else {
                        selectFamily(nextFamily)
                    }
                }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            runBusy {
                repo.signIn(email, password)
                isSignedIn = true
                currentUserUid = repo.currentUserUid()
                userEmail = repo.currentUserEmail()
                message = "Signed in."
                start()
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            runBusy {
                repo.signUp(email, password, name)
                isSignedIn = true
                currentUserUid = repo.currentUserUid()
                userEmail = repo.currentUserEmail()
                message = "Account created."
                start()
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
        currentUserUid = ""
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
            runBusy {
                val familyCode = repo.createFamily(name)
                message = "Family created. Code: $familyCode"
            }
        }
    }

    fun selectFamily(family: CloudFamily) {
        val previousFamilyId = selectedFamily?.id
        selectedFamily = family

        if (previousFamilyId != family.id) {
            memberJob?.cancel()
            recordJob?.cancel()
            joinRequestJob?.cancel()
            memberJob = null
            recordJob = null
            joinRequestJob = null
            members = emptyList()
            records = emptyList()
            joinRequests = emptyList()
        } else {
            refreshJoinRequestListener(family)
            return
        }

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

        refreshJoinRequestListener(family)
    }

    private fun refreshJoinRequestListener(family: CloudFamily) {
        joinRequestJob?.cancel()
        joinRequestJob = null
        joinRequests = emptyList()

        if (family.ownerUid != currentUserUid) return

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

    private fun clearActiveFamily() {
        selectedFamily = null
        members = emptyList()
        records = emptyList()
        joinRequests = emptyList()
        memberJob?.cancel()
        recordJob?.cancel()
        joinRequestJob?.cancel()
        memberJob = null
        recordJob = null
        joinRequestJob = null
    }

    fun addMember(name: String, relation: String, birthday: String) {
        val family = selectedFamily ?: run {
            message = "Create or join a family first."
            return
        }

        viewModelScope.launch {
            runBusy {
                repo.addFamilyMember(family.id, name, relation, birthday)
                message = "Member profile added."
            }
        }
    }

    fun requestToJoinFamily(familyCode: String) {
        viewModelScope.launch {
            runBusy {
                repo.requestToJoinFamily(familyCode)
                message = "Join request sent. The family creator can approve it."
            }
        }
    }

    fun inviteByEmail(email: String) {
        val family = selectedFamily ?: run {
            message = "Create or join a family first."
            return
        }

        viewModelScope.launch {
            runBusy {
                repo.inviteByEmail(family.id, email)
                message = "Invitation email queued."
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
            runBusy {
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
            }
        }
    }

    fun approveRequest(request: CloudJoinRequest) {
        val family = selectedFamily ?: return

        viewModelScope.launch {
            runBusy {
                repo.approveJoinRequest(family.id, request)
                message = "Request approved."
            }
        }
    }

    fun rejectRequest(request: CloudJoinRequest) {
        val family = selectedFamily ?: return

        viewModelScope.launch {
            runBusy {
                repo.rejectJoinRequest(family.id, request)
                message = "Request rejected."
            }
        }
    }

    fun confirmMimic(memberName: String) {
        message = "Now asking as $memberName."
    }

    private suspend fun runBusy(block: suspend () -> Unit) {
        isBusy = true
        message = ""
        try {
            block()
        } catch (e: Exception) {
            message = e.message ?: "Family Hub action failed."
        } finally {
            isBusy = false
        }
    }
}
