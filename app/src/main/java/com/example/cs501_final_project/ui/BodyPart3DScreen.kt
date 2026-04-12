package com.example.cs501_final_project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cs501_final_project.ui.components.AppCard
import io.github.sceneview.SceneView
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

@Composable
fun BodyPart3DScreen(
    onBodyPartSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val rotationY = remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "3D Body Viewer",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Model area
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
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

                // Transparent overlay to block touch interaction
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(enabled = true, onClick = {})
                )
            }
        }

        // Space area
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Please tap the body area in the image.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "This section can later show detailed questions for the selected body area.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        // Small button area at the very bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { rotationY.floatValue += 15f },
                modifier = Modifier.sizeIn(minWidth = 120.dp)
            ) {
                Text("Rotate Left")
            }

            Button(
                onClick = { rotationY.floatValue -= 15f },
                modifier = Modifier.sizeIn(minWidth = 120.dp)
            ) {
                Text("Rotate Right")
            }
        }
    }
}