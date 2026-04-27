package com.example.cs501_final_project.data

data class StoredUser(
    val id: String,
    val displayName: String,
    val identifier: String,
    val passwordHash: String,
    val createdAt: Long
)