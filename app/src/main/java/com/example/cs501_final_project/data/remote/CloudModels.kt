package com.example.cs501_final_project.data.remote

data class CloudUser(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val birthday: String = "",
    val phone: String = "",
    val selectedFamilyId: String = "",
    val familyIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CloudFamily(
    val id: String = "",
    val familyName: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val roles: Map<String, String> = emptyMap(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CloudFamilyMember(
    val id: String = "",
    val familyId: String = "",
    val name: String = "",
    val relationship: String = "",
    val birthday: String = "",
    val linkedUserUid: String = "",
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
    val requesterUid: String = "",
    val requesterEmail: String = "",
    val requesterName: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)