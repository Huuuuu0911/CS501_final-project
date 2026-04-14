package com.example.cs501_final_project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class HistoryRecord(
    val personName: String,
    val group: String,
    val date: String,
    val concern: String,
    val area: String,
    val urgency: String,
    val summary: String
)

@Composable
fun HistoryScreen() {
    val bgColor = Color(0xFFF6F8FC)
    var filter by remember { mutableStateOf("All") }

    val records = remember {
        listOf(
            HistoryRecord(
                personName = "You",
                group = "Mine",
                date = "Today · 10:40 AM",
                concern = "Chest tightness",
                area = "Center Chest",
                urgency = "Primary Care",
                summary = "Pain happens mainly during deep breathing and has been stable."
            ),
            HistoryRecord(
                personName = "You",
                group = "Mine",
                date = "Apr 10 · 8:15 PM",
                concern = "Lower back pain",
                area = "Lower Back",
                urgency = "Self Care",
                summary = "Likely related to sitting posture and muscle strain after study hours."
            ),
            HistoryRecord(
                personName = "Mom",
                group = "Family",
                date = "Apr 08 · 6:30 PM",
                concern = "Knee swelling",
                area = "Right Knee",
                urgency = "Urgent Care",
                summary = "Swelling increased after walking and family wanted a faster check."
            ),
            HistoryRecord(
                personName = "Dad",
                group = "Family",
                date = "Apr 05 · 7:10 AM",
                concern = "Shoulder soreness",
                area = "Left Shoulder",
                urgency = "Self Care",
                summary = "Pain was mild and improved after rest and limited activity."
            )
        )
    }

    val filteredRecords = remember(filter, records) {
        when (filter) {
            "Mine" -> records.filter { it.group == "Mine" }
            "Family" -> records.filter { it.group == "Family" }
            else -> records
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = "View your own checks and family records in one timeline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF667085)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCountCard(
                    modifier = Modifier.weight(1f),
                    title = "Mine",
                    value = records.count { it.group == "Mine" }.toString(),
                    icon = Icons.Default.Person,
                    accent = Color(0xFF4F8EEB)
                )
                SummaryCountCard(
                    modifier = Modifier.weight(1f),
                    title = "Family",
                    value = records.count { it.group == "Family" }.toString(),
                    icon = Icons.Default.Groups,
                    accent = Color(0xFF12B76A)
                )
                SummaryCountCard(
                    modifier = Modifier.weight(1f),
                    title = "Recent",
                    value = "7d",
                    icon = Icons.Default.Schedule,
                    accent = Color(0xFF7B61FF)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF7B61FF)
                        )
                        Text(
                            text = "Filter Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Mine", "Family").forEach { option ->
                            FilterChip(
                                selected = filter == option,
                                onClick = { filter = option },
                                label = { Text(option) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEAE6FF),
                                    selectedLabelColor = Color(0xFF4B3BC8),
                                    containerColor = Color(0xFFF6F8FC),
                                    labelColor = Color(0xFF48556A)
                                )
                            )
                        }
                    }
                }
            }

            filteredRecords.forEach { record ->
                HistoryRecordCard(record = record)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Next Upgrade",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "When you connect this screen to saved Follow-up results, these cards can become a real timeline with search, family filters, and repeat-symptom trends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF667085)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCountCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085)
            )
        }
    }
}

@Composable
private fun HistoryRecordCard(record: HistoryRecord) {
    val urgencyColor = when (record.urgency) {
        "Urgent Care" -> Color(0xFFF79009)
        "Primary Care" -> Color(0xFF2E90FA)
        else -> Color(0xFF12B76A)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(urgencyColor.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = record.urgency,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor
                    )
                }
            }

            Text(
                text = record.concern,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7B61FF))
                )
                Text(
                    text = record.area,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF344054)
                )
            }

            Text(
                text = record.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF667085)
            )
        }
    }
}
