package com.example.cs501_final_project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cs501_final_project.data.CareRouteViewModel
import com.example.cs501_final_project.data.remote.CloudFamilyMember

@Composable
fun FamilyHubScreen(
    careRouteViewModel: CareRouteViewModel,
    onAskAsMember: () -> Unit,
    hubViewModel: FamilyHubViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        hubViewModel.start()
    }

    if (!hubViewModel.isSignedIn) {
        FamilyAuthContent(viewModel = hubViewModel)
    } else {
        FamilyHubContent(
            viewModel = hubViewModel,
            careRouteViewModel = careRouteViewModel,
            onAskAsMember = onAskAsMember
        )
    }
}

@Composable
private fun FamilyAuthContent(
    viewModel: FamilyHubViewModel
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Family Hub Login",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Sign in with email to sync family health data across devices.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name for new account") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !viewModel.isBusy,
                onClick = {
                    viewModel.signIn(email, password)
                }
            ) {
                Text("Sign In")
            }

            OutlinedButton(
                enabled = !viewModel.isBusy,
                onClick = {
                    viewModel.signUp(email, password, name)
                }
            ) {
                Text("Create Account")
            }
        }

        if (viewModel.message.isNotBlank()) {
            Text(
                text = viewModel.message,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FamilyHubContent(
    viewModel: FamilyHubViewModel,
    careRouteViewModel: CareRouteViewModel,
    onAskAsMember: () -> Unit
) {
    var familyName by remember { mutableStateOf("") }
    var joinFamilyCode by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }

    var memberName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }

    var selectedMember by remember { mutableStateOf<CloudFamilyMember?>(null) }
    var symptoms by remember { mutableStateOf("") }
    var painLevel by remember { mutableFloatStateOf(3f) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDisbandConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Family Hub",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Signed in as ${viewModel.userEmail}",
                style = MaterialTheme.typography.bodySmall
            )

            if (viewModel.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = viewModel.message,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!viewModel.hasFamily) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Create a Family",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Each user can belong to one family. The creator becomes the owner and can approve join requests.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = familyName,
                            onValueChange = { familyName = it },
                            label = { Text("Family name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            enabled = !viewModel.isBusy,
                            onClick = {
                                viewModel.createFamily(familyName)
                                familyName = ""
                            }
                        ) {
                            Text("Create Family")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Join by Family Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Enter the 6-digit code from the family owner or invitation email.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = joinFamilyCode,
                            onValueChange = { joinFamilyCode = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Family code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Button(
                            enabled = !viewModel.isBusy && joinFamilyCode.length == 6,
                            onClick = {
                                viewModel.requestToJoinFamily(joinFamilyCode)
                                joinFamilyCode = ""
                            }
                        ) {
                            Text("Send Join Request")
                        }
                    }
                }
            }
        }

        viewModel.selectedFamily?.let { family ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = family.familyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text("Family Code: ${family.familyCode}")
                        Text("Role: ${family.roles[viewModel.currentUserUid] ?: "member"}")

                        if (viewModel.canManageJoinRequests) {
                            Text(
                                text = "You created this family, so you can approve or reject join requests.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            OutlinedButton(
                                enabled = !viewModel.isBusy,
                                onClick = {
                                    showDisbandConfirm = true
                                    showLeaveConfirm = false
                                }
                            ) {
                                Text("Disband Family")
                            }

                            if (showDisbandConfirm) {
                                Text(
                                    text = "This will remove the family for everyone and clear all family records in the cloud.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        enabled = !viewModel.isBusy,
                                        onClick = {
                                            careRouteViewModel.selectPerson("self")
                                            selectedMember = null
                                            symptoms = ""
                                            painLevel = 3f
                                            showDisbandConfirm = false
                                            viewModel.disbandFamily()
                                        }
                                    ) {
                                        Text("Confirm Disband")
                                    }

                                    OutlinedButton(
                                        onClick = { showDisbandConfirm = false }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "You can leave this family at any time. Your account will no longer appear in this family.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            OutlinedButton(
                                enabled = !viewModel.isBusy,
                                onClick = {
                                    showLeaveConfirm = true
                                    showDisbandConfirm = false
                                }
                            ) {
                                Text("Leave Family")
                            }

                            if (showLeaveConfirm) {
                                Text(
                                    text = "This will remove your account from this family and clear your family link.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        enabled = !viewModel.isBusy,
                                        onClick = {
                                            careRouteViewModel.selectPerson("self")
                                            selectedMember = null
                                            symptoms = ""
                                            painLevel = 3f
                                            showLeaveConfirm = false
                                            viewModel.leaveFamily()
                                        }
                                    ) {
                                        Text("Confirm Leave")
                                    }

                                    OutlinedButton(
                                        onClick = { showLeaveConfirm = false }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Invite by Email",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Any family member can invite someone. The email includes this family's 6-digit code.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = inviteEmail,
                            onValueChange = { inviteEmail = it },
                            label = { Text("Email address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Button(
                            enabled = !viewModel.isBusy && inviteEmail.isNotBlank(),
                            onClick = {
                                viewModel.inviteByEmail(inviteEmail)
                                inviteEmail = ""
                            }
                        ) {
                            Text("Send Invite Email")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Add Offline Family Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Use this for children or relatives who do not have their own account yet.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = memberName,
                            onValueChange = { memberName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = relationship,
                            onValueChange = { relationship = it },
                            label = { Text("Relationship, for example Dad") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = birthday,
                            onValueChange = { birthday = it },
                            label = { Text("Birth date, for example 05/20/1975") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            enabled = !viewModel.isBusy && memberName.isNotBlank(),
                            onClick = {
                                viewModel.addMember(memberName, relationship, birthday)
                                memberName = ""
                                relationship = ""
                                birthday = ""
                            }
                        ) {
                            Text("Add Profile")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Family Members",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (viewModel.members.isEmpty()) {
            item {
                Text("No family members yet.")
            }
        }

        items(viewModel.members) { member ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.name.ifBlank { "Family Member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text("Relationship: ${member.relationship.ifBlank { "Not set" }}")
                    Text("Email: ${(member.linkedUserEmail.ifBlank { member.email }).ifBlank { "Offline profile" }}")
                    Text("Birth Date: ${member.birthDate.ifBlank { "Not set" }}")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                selectedMember = member
                            }
                        ) {
                            Text("Record Symptom")
                        }

                        OutlinedButton(
                            onClick = {
                                careRouteViewModel.mimicCloudFamilyMember(member)
                                viewModel.confirmMimic(member.name.ifBlank { "Family Member" })
                                onAskAsMember()
                            }
                        ) {
                            Text("Mimic & Ask")
                        }
                    }
                }
            }
        }

        viewModel.selectedFamily?.let {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Add Quick Symptom Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = selectedMember?.name ?: "No member selected",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            value = symptoms,
                            onValueChange = { symptoms = it },
                            label = { Text("Symptoms") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Pain Level: ${painLevel.toInt()}")

                        Slider(
                            value = painLevel,
                            onValueChange = { painLevel = it },
                            valueRange = 0f..10f
                        )

                        Button(
                            enabled = !viewModel.isBusy && selectedMember != null && symptoms.isNotBlank(),
                            onClick = {
                                selectedMember?.let { member ->
                                    viewModel.addQuickRecord(
                                        member = member,
                                        symptoms = symptoms,
                                        pain = painLevel.toInt()
                                    )
                                    symptoms = ""
                                    painLevel = 3f
                                }
                            }
                        ) {
                            Text("Save Record")
                        }
                    }
                }
            }
        }

        if (viewModel.records.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Family Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(viewModel.records) { record ->
            val memberNameForRecord = viewModel.members
                .firstOrNull { it.id == record.memberId }
                ?.name ?: "Unknown member"

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text("Member: $memberNameForRecord")
                    Text("Symptoms: ${record.symptoms}")
                    Text("Pain Level: ${record.painLevel}")
                    Text("Urgency: ${record.urgency}")
                }
            }
        }

        if (viewModel.canManageJoinRequests) {
            item {
                HorizontalDivider()
                Text(
                    text = "Pending Join Requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (viewModel.joinRequests.isEmpty()) {
                    Text("No pending requests.")
                }
            }

            items(viewModel.joinRequests) { request ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Request from: ${request.requesterName}")
                        Text("Email: ${request.requesterEmail}")
                        Text("Family Code: ${request.familyCode}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = !viewModel.isBusy,
                                onClick = {
                                    viewModel.approveRequest(request)
                                }
                            ) {
                                Text("Approve")
                            }

                            OutlinedButton(
                                enabled = !viewModel.isBusy,
                                onClick = {
                                    viewModel.rejectRequest(request)
                                }
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
