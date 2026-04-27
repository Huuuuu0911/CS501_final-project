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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cs501_final_project.data.remote.CloudFamilyMember

@Composable
fun FamilyHubScreen(
    viewModel: FamilyHubViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.start()
    }

    if (!viewModel.isSignedIn) {
        FamilyAuthContent(viewModel = viewModel)
    } else {
        FamilyHubContent(viewModel = viewModel)
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
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Sign in to sync family health data across devices.",
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
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.signIn(email, password)
                }
            ) {
                Text("Sign In")
            }

            OutlinedButton(
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
    viewModel: FamilyHubViewModel
) {
    var familyName by remember { mutableStateOf("") }
    var joinFamilyId by remember { mutableStateOf("") }

    var memberName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }

    var selectedMember by remember { mutableStateOf<CloudFamilyMember?>(null) }
    var symptoms by remember { mutableStateOf("") }
    var painLevel by remember { mutableFloatStateOf(3f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Family Hub",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Signed in as ${viewModel.userEmail}",
                style = MaterialTheme.typography.bodySmall
            )

            if (viewModel.message.isNotBlank()) {
                Text(
                    text = viewModel.message,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.signOut() }
            ) {
                Text("Sign Out")
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Create a Family",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = familyName,
                        onValueChange = { familyName = it },
                        label = { Text("Family name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
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
                        text = "Join an Existing Family",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Ask the family owner for the Family ID.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = joinFamilyId,
                        onValueChange = { joinFamilyId = it },
                        label = { Text("Family ID") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.requestToJoinFamily(joinFamilyId)
                            joinFamilyId = ""
                        }
                    ) {
                        Text("Send Join Request")
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
                        text = "My Families",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (viewModel.families.isEmpty()) {
                        Text("No family yet.")
                    } else {
                        viewModel.families.forEach { family ->
                            AssistChip(
                                onClick = { viewModel.selectFamily(family) },
                                label = {
                                    Text(family.familyName)
                                }
                            )
                        }
                    }

                    viewModel.selectedFamily?.let { family ->
                        Divider()
                        Text("Selected: ${family.familyName}")
                        Text(
                            text = "Family ID: ${family.id}",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                        text = "Add Family Member",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text("Relationship, for example Dad") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = birthday,
                        onValueChange = { birthday = it },
                        label = { Text("Birthday, for example 05/20/1975") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.addMember(memberName, relationship, birthday)
                            memberName = ""
                            relationship = ""
                            birthday = ""
                        }
                    ) {
                        Text("Add Member")
                    }
                }
            }
        }

        item {
            Text(
                text = "Family Members",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(viewModel.members) { member ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Relationship: ${member.relationship.ifBlank { "Not set" }}")
                    Text("Birthday: ${member.birthday.ifBlank { "Not set" }}")

                    Button(
                        onClick = {
                            selectedMember = member
                        }
                    ) {
                        Text("Select for Record")
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
                        text = "Add Quick Symptom Record",
                        style = MaterialTheme.typography.titleMedium
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
                        onClick = {
                            selectedMember?.let { member ->
                                viewModel.addQuickRecord(
                                    member = member,
                                    symptoms = symptoms,
                                    painLevel = painLevel.toInt()
                                )
                                symptoms = ""
                                painLevel = 3f
                            }
                        },
                        enabled = selectedMember != null
                    ) {
                        Text("Save Record")
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Family Records",
                style = MaterialTheme.typography.titleMedium
            )
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
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Member: $memberNameForRecord")
                    Text("Symptoms: ${record.symptoms}")
                    Text("Pain Level: ${record.painLevel}")
                    Text("Urgency: ${record.urgency}")
                }
            }
        }

        item {
            if (viewModel.joinRequests.isNotEmpty()) {
                Text(
                    text = "Pending Join Requests",
                    style = MaterialTheme.typography.titleMedium
                )
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.approveRequest(request)
                            }
                        ) {
                            Text("Approve")
                        }

                        OutlinedButton(
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
}