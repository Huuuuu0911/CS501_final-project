package com.example.cs501_final_project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cs501_final_project.ui.components.AppCard
import io.github.sceneview.SceneView
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.cs501_final_project.data.GeminiRepository
import kotlinx.coroutines.launch

@Composable
fun BodyPart3DScreen(
    onBodyPartSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val rotationY = remember { mutableFloatStateOf(0f) }

    val repository = remember { GeminiRepository() }
    val scope = rememberCoroutineScope()

    var symptomText by remember { mutableStateOf("") }
    var assistantResponse by remember { mutableStateOf("No response yet.") }
    var isLoading by remember { mutableStateOf(false) }

    val selectedBodyPart = "General"

    val bgColor = Color(0xFFF6F8FC)
    val headerGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF5B8DEF),
            Color(0xFF7B61FF)
        )
    )

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

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = headerGradient,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(22.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Body Area Check",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )

                    Text(
                        text = "Rotate the model and choose the body area you want to describe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            // Model section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3D Body Viewer",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Use the buttons below to rotate the model. Body area selection can be added next.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666A73)
                    )

                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            SceneView(
                                modifier = Modifier.fillMaxSize(),
                                engine = engine,
                                modelLoader = modelLoader
                            ) {
                                rememberModelInstance(
                                    modelLoader = modelLoader,
                                    assetFileLocation = "models/male_model.glb"
                                )?.let { modelInstance ->
                                    ModelNode(
                                        modelInstance = modelInstance,
                                        scaleToUnits = 0.68f,
                                        rotation = Rotation(y = rotationY.floatValue)
                                    )
                                }
                            }

                            // block touch on model area for now
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent)
                                    .clickable(enabled = true, onClick = {})
                            )
                        }
                    }
                }
            }

            // Rotation controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Model Controls",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { rotationY.floatValue += 15f },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5B8DEF)
                            )
                        ) {
                            Text("Rotate Left")
                        }

                        Button(
                            onClick = { rotationY.floatValue -= 15f },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7B61FF)
                            )
                        ) {
                            Text("Rotate Right")
                        }
                    }
                }
            }

            // Small guide section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "How to use",
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider(color = Color(0xFFE8EAF0))

                    Text(
                        text = "1. Rotate the model left or right.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "2. Choose the body area you want to check.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "3. The assistant section below can later show follow-up questions and suggestions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Assistant Panel (REAL Gemini)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Assistant",
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider(color = Color(0xFFE8EAF0))

                    Text(
                        text = "Describe your symptom",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = symptomText,
                        onValueChange = { symptomText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. I feel sharp pain in my chest") },
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                assistantResponse = "Loading..."

                                try {
                                    val result = repository.askGemini(
                                        bodyPart = selectedBodyPart,
                                        symptomText = symptomText,
                                        age = "21",
                                        gender = "Female",
                                        height = "5'6\"",
                                        weight = "130",
                                        address = "Boston, MA"
                                    )
                                    assistantResponse = result
                                } catch (e: Exception) {
                                    assistantResponse = "Error: ${e::class.java.simpleName}: ${e.message}"
                                    e.printStackTrace()
                                }

                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = symptomText.isNotBlank()
                    ) {
                        Text("Ask Assistant")
                    }

                    if (isLoading) {
                        CircularProgressIndicator()
                    }

                    Text(
                        text = assistantResponse,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF44474F)
                    )
                }
            }
        }
    }
}