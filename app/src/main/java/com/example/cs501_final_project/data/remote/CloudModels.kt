package com.example.cs501_final_project.data.remote

data class CloudUser(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val birthDate: String = "",
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val address: String = "",
    val allergies: String = "",
    val medications: String = "",
    val conditions: String = "",
    val emergencyContact: String = "",
    val selectedFamilyId: String = "",
    val familyCode: String = "",
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CloudFamily(
    val id: String = "",
    val familyName: String = "",
    val familyCode: String = "",
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val memberUids: List<String> = emptyList(),
    val roles: Map<String, String> = emptyMap(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CloudFamilyMember(
    val id: String = "",
    val familyId: String = "",
    val name: String = "",
    val email: String = "",
    val relationship: String = "",
    val birthDate: String = "",
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val address: String = "",
    val allergies: String = "",
    val medications: String = "",
    val conditions: String = "",
    val emergencyContact: String = "",
    val linkedUserUid: String = "",
    val linkedUserEmail: String = "",
    val createdByUid: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CloudHealthRecord(
    val id: String = "",
    val familyId: String = "",
    val memberId: String = "",
    val type: String = "",
    val title: String = "",
    val symptoms: String = "",
    val bodyArea: String = "",
    val painLevel: Int = 0,
    val urgency: String = "",
    val notes: String = "",
    val createdByUid: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CloudJoinRequest(
    val id: String = "",
    val familyId: String = "",
    val familyCode: String = "",
    val requesterUid: String = "",
    val requesterEmail: String = "",
    val requesterName: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CloudFamilyInvite(
    val id: String = "",
    val familyId: String = "",
    val familyCode: String = "",
    val familyName: String = "",
    val inviterUid: String = "",
    val inviterName: String = "",
    val inviterEmail: String = "",
    val inviteeEmail: String = "",
    val status: String = "sent",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
