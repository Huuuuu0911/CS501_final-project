package com.example.cs501_final_project.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class RemoteFamilyRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun now(): Long {
        return System.currentTimeMillis()
    }

    private fun currentUid(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("Please sign in first.")
    }

    private fun currentEmail(): String {
        return auth.currentUser?.email ?: ""
    }

    private fun currentName(): String {
        val user = auth.currentUser
        return user?.displayName ?: user?.email ?: "User"
    }

    fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }

    fun currentUserUid(): String {
        return auth.currentUser?.uid ?: ""
    }

    fun currentUserEmail(): String {
        return currentEmail()
    }

    private fun isValidEmail(value: String): Boolean {
        val clean = value.trim()
        return clean.contains("@") && clean.substringAfter("@").contains(".")
    }

    private suspend fun saveUserDocument(
        user: FirebaseUser,
        nameFromInput: String? = null,
        isNewUser: Boolean = false
    ) {
        val time = now()
        val userRef = db.collection("users").document(user.uid)
        val snapshot = userRef.get().await()

        val finalName = when {
            !nameFromInput.isNullOrBlank() -> nameFromInput.trim()
            !user.displayName.isNullOrBlank() -> user.displayName ?: "User"
            !user.email.isNullOrBlank() -> user.email ?: "User"
            else -> "User"
        }

        val email = user.email ?: ""

        if (!snapshot.exists() || isNewUser) {
            val userData = mapOf(
                "uid" to user.uid,
                "email" to email,
                "name" to finalName,
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

            userRef.set(userData, SetOptions.merge()).await()
        } else {
            val updateData = mapOf(
                "uid" to user.uid,
                "email" to email,
                "name" to finalName,
                "lastLoginAt" to time,
                "updatedAt" to time
            )

            userRef.set(updateData, SetOptions.merge()).await()
        }
    }

    private suspend fun ensureUserDocument() {
        val user = auth.currentUser ?: return
        saveUserDocument(user = user, isNewUser = false)
    }

    suspend fun signUp(
        email: String,
        password: String,
        name: String
    ) {
        val cleanEmail = email.trim()
        val cleanName = name.trim().ifBlank { "User" }

        if (!isValidEmail(cleanEmail)) {
            throw IllegalArgumentException("Please use an email address.")
        }

        val result = auth.createUserWithEmailAndPassword(
            cleanEmail,
            password
        ).await()

        val user = result.user ?: return

        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(cleanName)
            .build()

        user.updateProfile(profileUpdate).await()

        saveUserDocument(
            user = user,
            nameFromInput = cleanName,
            isNewUser = true
        )
    }

    suspend fun signIn(
        email: String,
        password: String
    ) {
        if (!isValidEmail(email)) {
            throw IllegalArgumentException("Please use an email address.")
        }

        auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).await()

        ensureUserDocument()
    }

    fun signOut() {
        auth.signOut()
    }

    private suspend fun currentUserAlreadyHasFamily(uid: String = currentUid()): Boolean {
        val userSnapshot = db.collection("users")
            .document(uid)
            .get()
            .await()

        val selectedFamilyId = userSnapshot.getString("selectedFamilyId").orEmpty()
        if (selectedFamilyId.isNotBlank()) return true

        val familySnapshot = db.collection("families")
            .whereArrayContains("memberUids", uid)
            .limit(1)
            .get()
            .await()

        return familySnapshot.documents.isNotEmpty()
    }

    private suspend fun requireNoFamilyMembership() {
        if (currentUserAlreadyHasFamily()) {
            throw IllegalStateException("Each user can only belong to one family.")
        }
    }

    private suspend fun generateUniqueFamilyCode(): String {
        repeat(20) {
            val candidate = Random.nextInt(100000, 1000000).toString()
            val snapshot = db.collection("families")
                .whereEqualTo("familyCode", candidate)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isEmpty()) return candidate
        }

        throw IllegalStateException("Could not generate a family code. Please try again.")
    }

    private fun linkedMemberFromUserSnapshot(
        snapshot: DocumentSnapshot,
        familyId: String,
        relationship: String,
        createdByUid: String,
        time: Long,
        fallbackEmail: String = "",
        fallbackName: String = "User"
    ): CloudFamilyMember {
        val uid = snapshot.id
        val email = snapshot.getString("email").orEmpty().ifBlank { fallbackEmail }
        val name = snapshot.getString("name").orEmpty().ifBlank { fallbackName.ifBlank { email.ifBlank { "User" } } }

        return CloudFamilyMember(
            id = uid,
            familyId = familyId,
            name = name,
            email = email,
            relationship = relationship,
            birthDate = snapshot.getString("birthDate").orEmpty().ifBlank {
                snapshot.getString("birthday").orEmpty()
            },
            age = snapshot.getString("age").orEmpty(),
            gender = snapshot.getString("gender").orEmpty(),
            height = snapshot.getString("height").orEmpty(),
            weight = snapshot.getString("weight").orEmpty(),
            address = snapshot.getString("address").orEmpty(),
            allergies = snapshot.getString("allergies").orEmpty(),
            medications = snapshot.getString("medications").orEmpty(),
            conditions = snapshot.getString("conditions").orEmpty(),
            emergencyContact = snapshot.getString("emergencyContact").orEmpty(),
            linkedUserUid = uid,
            linkedUserEmail = email,
            createdByUid = createdByUid,
            createdAt = time,
            updatedAt = time
        )
    }

    private suspend fun resolveFamilyByCode(familyCode: String): CloudFamily {
        val cleanCode = familyCode.trim()

        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            throw IllegalArgumentException("Please enter a 6-digit family code.")
        }

        val snapshot = db.collection("families")
            .whereEqualTo("familyCode", cleanCode)
            .limit(1)
            .get()
            .await()

        val document = snapshot.documents.firstOrNull()
            ?: throw IllegalArgumentException("No family found with that code.")

        return document.toObject(CloudFamily::class.java)?.copy(id = document.id)
            ?: throw IllegalStateException("Could not read family data.")
    }

    private suspend fun getFamily(familyId: String): CloudFamily {
        val snapshot = db.collection("families")
            .document(familyId)
            .get()
            .await()

        return snapshot.toObject(CloudFamily::class.java)?.copy(id = snapshot.id)
            ?: throw IllegalArgumentException("Family not found.")
    }

    private suspend fun requireFamilyOwner(familyId: String): CloudFamily {
        val family = getFamily(familyId)
        if (family.ownerUid != currentUid()) {
            throw IllegalStateException("Only the family creator can manage join requests.")
        }
        return family
    }

    private suspend fun requireFamilyMember(familyId: String): CloudFamily {
        val family = getFamily(familyId)
        if (!family.memberUids.contains(currentUid())) {
            throw IllegalStateException("Only family members can invite people.")
        }
        return family
    }

    suspend fun createFamily(familyName: String): String {
        ensureUserDocument()
        requireNoFamilyMembership()

        val uid = currentUid()
        val time = now()
        val familyCode = generateUniqueFamilyCode()

        val familyRef = db.collection("families").document()
        val userRef = db.collection("users").document(uid)
        val userSnapshot = userRef.get().await()
        val memberRef = familyRef.collection("members").document(uid)

        val cleanFamilyName = familyName.trim().ifBlank {
            "${currentName()}'s Family"
        }

        val family = CloudFamily(
            id = familyRef.id,
            familyName = cleanFamilyName,
            familyCode = familyCode,
            ownerUid = uid,
            ownerEmail = currentEmail(),
            memberUids = listOf(uid),
            roles = mapOf(uid to "owner"),
            createdAt = time,
            updatedAt = time
        )

        val meMember = linkedMemberFromUserSnapshot(
            snapshot = userSnapshot,
            familyId = familyRef.id,
            relationship = "Me",
            createdByUid = uid,
            time = time,
            fallbackEmail = currentEmail(),
            fallbackName = currentName()
        )

        db.runBatch { batch ->
            batch.set(familyRef, family)
            batch.set(memberRef, meMember)
            batch.set(
                userRef,
                mapOf(
                    "selectedFamilyId" to familyRef.id,
                    "familyCode" to familyCode,
                    "lastLoginAt" to time,
                    "updatedAt" to time
                ),
                SetOptions.merge()
            )
        }.await()

        return familyCode
    }

    fun listenMyFamilies(): Flow<List<CloudFamily>> = callbackFlow {
        val user = auth.currentUser

        if (user == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection("families")
            .whereArrayContains("memberUids", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val families = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CloudFamily::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.createdAt } ?: emptyList()

                trySend(families.take(1))
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun addFamilyMember(
        familyId: String,
        name: String,
        relationship: String,
        birthDate: String
    ) {
        ensureUserDocument()
        requireFamilyMember(familyId)

        val uid = currentUid()
        val time = now()

        val memberRef = db.collection("families")
            .document(familyId)
            .collection("members")
            .document()

        val member = CloudFamilyMember(
            id = memberRef.id,
            familyId = familyId,
            name = name.trim(),
            relationship = relationship.trim(),
            birthDate = birthDate.trim(),
            linkedUserUid = "",
            linkedUserEmail = "",
            createdByUid = uid,
            createdAt = time,
            updatedAt = time
        )

        memberRef.set(member).await()
    }

    fun listenFamilyMembers(familyId: String): Flow<List<CloudFamilyMember>> = callbackFlow {
        val listener = db.collection("families")
            .document(familyId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val members = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CloudFamilyMember::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.name.lowercase() } ?: emptyList()

                trySend(members)
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun addHealthRecord(
        familyId: String,
        memberId: String,
        type: String,
        title: String,
        symptoms: String,
        bodyArea: String,
        painLevel: Int,
        urgency: String,
        notes: String
    ) {
        ensureUserDocument()
        requireFamilyMember(familyId)

        val uid = currentUid()
        val time = now()

        val recordRef = db.collection("families")
            .document(familyId)
            .collection("records")
            .document()

        val record = CloudHealthRecord(
            id = recordRef.id,
            familyId = familyId,
            memberId = memberId,
            type = type.trim(),
            title = title.trim(),
            symptoms = symptoms.trim(),
            bodyArea = bodyArea.trim(),
            painLevel = painLevel,
            urgency = urgency.trim(),
            notes = notes.trim(),
            createdByUid = uid,
            createdAt = time,
            updatedAt = time
        )

        recordRef.set(record).await()
    }

    fun listenHealthRecords(
        familyId: String,
        memberId: String? = null
    ): Flow<List<CloudHealthRecord>> = callbackFlow {
        var query = db.collection("families")
            .document(familyId)
            .collection("records")
            .limit(100)

        if (!memberId.isNullOrBlank()) {
            query = query.whereEqualTo("memberId", memberId)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val records = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(CloudHealthRecord::class.java)?.copy(id = doc.id)
            }?.sortedByDescending { it.createdAt } ?: emptyList()

            trySend(records)
        }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun requestToJoinFamily(familyCode: String) {
        ensureUserDocument()
        requireNoFamilyMembership()

        val family = resolveFamilyByCode(familyCode)
        val uid = currentUid()

        if (family.memberUids.contains(uid)) {
            throw IllegalStateException("You are already in this family.")
        }

        val time = now()
        val requestRef = db.collection("families")
            .document(family.id)
            .collection("joinRequests")
            .document(uid)

        val request = CloudJoinRequest(
            id = uid,
            familyId = family.id,
            familyCode = family.familyCode,
            requesterUid = uid,
            requesterEmail = currentEmail(),
            requesterName = currentName(),
            status = "pending",
            createdAt = time,
            updatedAt = time
        )

        requestRef.set(request).await()
    }

    fun listenJoinRequests(familyId: String): Flow<List<CloudJoinRequest>> = callbackFlow {
        val listener = db.collection("families")
            .document(familyId)
            .collection("joinRequests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CloudJoinRequest::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.createdAt } ?: emptyList()

                trySend(requests)
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun approveJoinRequest(
        familyId: String,
        request: CloudJoinRequest,
        role: String = "member"
    ) {
        val family = requireFamilyOwner(familyId)
        val time = now()

        val familyRef = db.collection("families").document(familyId)
        val requestRef = familyRef
            .collection("joinRequests")
            .document(request.requesterUid)
        val requesterUserRef = db.collection("users").document(request.requesterUid)
        val requesterUserSnapshot = requesterUserRef.get().await()

        val existingFamilyId = requesterUserSnapshot.getString("selectedFamilyId").orEmpty()
        if (existingFamilyId.isNotBlank() && existingFamilyId != familyId) {
            throw IllegalStateException("This user already belongs to another family.")
        }

        if (family.memberUids.contains(request.requesterUid)) {
            requestRef.update(
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to time
                )
            ).await()
            return
        }

        if (existingFamilyId.isBlank() && currentUserAlreadyHasFamily(request.requesterUid)) {
            throw IllegalStateException("This user already belongs to another family.")
        }

        val memberRef = familyRef
            .collection("members")
            .document(request.requesterUid)

        val linkedMember = linkedMemberFromUserSnapshot(
            snapshot = requesterUserSnapshot,
            familyId = familyId,
            relationship = "Family Member",
            createdByUid = currentUid(),
            time = time,
            fallbackEmail = request.requesterEmail,
            fallbackName = request.requesterName
        )

        db.runBatch { batch ->
            batch.update(
                familyRef,
                "memberUids",
                FieldValue.arrayUnion(request.requesterUid)
            )

            batch.update(
                familyRef,
                "updatedAt",
                time
            )

            batch.update(
                familyRef,
                FieldPath.of("roles", request.requesterUid),
                role
            )

            batch.set(memberRef, linkedMember, SetOptions.merge())

            batch.set(
                requesterUserRef,
                mapOf(
                    "selectedFamilyId" to familyId,
                    "familyCode" to family.familyCode,
                    "updatedAt" to time
                ),
                SetOptions.merge()
            )

            batch.update(
                requestRef,
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to time
                )
            )
        }.await()
    }

    suspend fun rejectJoinRequest(
        familyId: String,
        request: CloudJoinRequest
    ) {
        requireFamilyOwner(familyId)

        val requestRef = db.collection("families")
            .document(familyId)
            .collection("joinRequests")
            .document(request.requesterUid)

        requestRef.update(
            mapOf(
                "status" to "rejected",
                "updatedAt" to now()
            )
        ).await()
    }

    suspend fun leaveFamily(familyId: String) {
        ensureUserDocument()

        val family = requireFamilyMember(familyId)
        val uid = currentUid()

        if (family.ownerUid == uid) {
            throw IllegalStateException("The family owner should disband the family instead of leaving it.")
        }

        val time = now()
        val familyRef = db.collection("families").document(familyId)
        val userRef = db.collection("users").document(uid)
        val memberRef = familyRef.collection("members").document(uid)
        val requestRef = familyRef.collection("joinRequests").document(uid)

        val recordDocs = familyRef.collection("records")
            .whereEqualTo("memberId", uid)
            .get()
            .await()
            .documents

        db.runBatch { batch ->
            batch.update(
                familyRef,
                "memberUids",
                FieldValue.arrayRemove(uid)
            )
            batch.update(familyRef, "updatedAt", time)
            batch.update(
                familyRef,
                FieldPath.of("roles", uid),
                FieldValue.delete()
            )

            batch.set(
                userRef,
                mapOf(
                    "selectedFamilyId" to "",
                    "familyCode" to "",
                    "updatedAt" to time
                ),
                SetOptions.merge()
            )

            batch.delete(memberRef)
            batch.delete(requestRef)
            recordDocs.forEach { doc ->
                batch.delete(doc.reference)
            }
        }.await()
    }

    suspend fun disbandFamily(familyId: String) {
        val family = requireFamilyOwner(familyId)
        val time = now()
        val familyRef = db.collection("families").document(familyId)

        val memberDocs = familyRef.collection("members").get().await().documents
        val recordDocs = familyRef.collection("records").get().await().documents
        val requestDocs = familyRef.collection("joinRequests").get().await().documents
        val inviteDocs = familyRef.collection("emailInvites").get().await().documents

        db.runBatch { batch ->
            family.memberUids.forEach { memberUid ->
                batch.set(
                    db.collection("users").document(memberUid),
                    mapOf(
                        "selectedFamilyId" to "",
                        "familyCode" to "",
                        "updatedAt" to time
                    ),
                    SetOptions.merge()
                )
            }

            memberDocs.forEach { doc -> batch.delete(doc.reference) }
            recordDocs.forEach { doc -> batch.delete(doc.reference) }
            requestDocs.forEach { doc -> batch.delete(doc.reference) }
            inviteDocs.forEach { doc -> batch.delete(doc.reference) }
            batch.delete(familyRef)
        }.await()
    }

    suspend fun inviteByEmail(
        familyId: String,
        inviteeEmail: String
    ) {
        ensureUserDocument()
        val family = requireFamilyMember(familyId)
        val cleanEmail = inviteeEmail.trim().lowercase()

        if (!isValidEmail(cleanEmail)) {
            throw IllegalArgumentException("Please enter a valid email address.")
        }

        val time = now()
        val safeInviteId = cleanEmail.replace("/", "_")
        val inviteRef = db.collection("families")
            .document(familyId)
            .collection("emailInvites")
            .document(safeInviteId)

        val invite = CloudFamilyInvite(
            id = safeInviteId,
            familyId = familyId,
            familyCode = family.familyCode,
            familyName = family.familyName,
            inviterUid = currentUid(),
            inviterName = currentName(),
            inviterEmail = currentEmail(),
            inviteeEmail = cleanEmail,
            status = "sent",
            createdAt = time,
            updatedAt = time
        )

        val emailText = "${currentName()} invited you to join ${family.familyName} on CareRoute. " +
                "Open CareRoute, sign in with this email, then enter family code ${family.familyCode} in Family Hub."

        val mailDocument = mapOf(
            "to" to listOf(cleanEmail),
            "message" to mapOf(
                "subject" to "CareRoute family invitation",
                "text" to emailText,
                "html" to """
                    <p>${currentName()} invited you to join <strong>${family.familyName}</strong> on CareRoute.</p>
                    <p>Open CareRoute, sign in with this email, then enter this Family Code in Family Hub:</p>
                    <h2>${family.familyCode}</h2>
                """.trimIndent()
            ),
            "createdAt" to time
        )

        db.runBatch { batch ->
            batch.set(inviteRef, invite, SetOptions.merge())
        }.await()

        db.collection("mail").add(mailDocument).await()
    }
}