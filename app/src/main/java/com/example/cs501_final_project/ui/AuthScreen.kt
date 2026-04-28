package com.example.cs501_final_project.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cs501_final_project.data.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel
) {
    var isLoginMode by rememberSaveable { mutableStateOf(true) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }

    val bgColor = Color(0xFFF5F7FC)
    val heroGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF3568FF),
            Color(0xFF6A4DFF)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(heroGradient)
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "CareRoute",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isLoginMode) "Sign in to continue" else "Create your account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }

                    Text(
                        text = "Use an email or phone number and password. Emergency Mode is available for immediate access without an account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.96f)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AuthModeChip(
                            selected = isLoginMode,
                            text = "Login",
                            icon = Icons.Default.Lock,
                            onClick = {
                                isLoginMode = true
                                authViewModel.clearError()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        AuthModeChip(
                            selected = !isLoginMode,
                            text = "Register",
                            icon = Icons.Default.PersonAddAlt1,
                            onClick = {
                                isLoginMode = false
                                authViewModel.clearError()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                authViewModel.clearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Full name") },
                            placeholder = { Text("e.g. Alex Chen") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words
                            )
                        )
                    }

                    OutlinedTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it
                            authViewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email or phone") },
                        placeholder = { Text("e.g. alex@email.com or 6175551234") },
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            authViewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        placeholder = { Text("At least 6 characters") },
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = { showPassword = !showPassword }
                            ) {
                                Icon(
                                    imageVector = if (showPassword) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (showPassword) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    }
                                )
                            }
                        }
                    )

                    if (isLoginMode) {
                        TextButton(
                            onClick = {
                                authViewModel.resetPassword(identifier)
                            },
                            enabled = !authViewModel.isBusy,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Forgot password?")
                        }
                    }

                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                authViewModel.clearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm password") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            visualTransformation = if (showConfirmPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(
                                    onClick = { showConfirmPassword = !showConfirmPassword }
                                ) {
                                    Icon(
                                        imageVector = if (showConfirmPassword) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (showConfirmPassword) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        }
                                    )
                                }
                            }
                        )
                    }

                    authViewModel.errorMessage.takeIf { it.isNotBlank() }?.let { message ->
                        val isSuccessMessage =
                            message.contains("sent", ignoreCase = true) ||
                                    message.contains("Signed in", ignoreCase = true) ||
                                    message.contains("Account created", ignoreCase = true) ||
                                    message.contains("Emergency mode", ignoreCase = true)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuccessMessage) {
                                    Color(0xFFEFFAF3)
                                } else {
                                    Color(0xFFFFF1F1)
                                }
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                color = if (isSuccessMessage) {
                                    Color(0xFF067647)
                                } else {
                                    Color(0xFFB42318)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isLoginMode) {
                                authViewModel.login(identifier, password)
                            } else {
                                authViewModel.register(
                                    displayName = displayName,
                                    identifier = identifier,
                                    password = password,
                                    confirmPassword = confirmPassword
                                )
                            }
                        },
                        enabled = !authViewModel.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = when {
                                authViewModel.isBusy && isLoginMode -> "Signing in..."
                                authViewModel.isBusy && !isLoginMode -> "Creating account..."
                                isLoginMode -> "Login"
                                else -> "Create account"
                            }
                        )
                    }

                    TextButton(
                        onClick = {
                            isLoginMode = !isLoginMode
                            authViewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLoginMode) {
                                "No account yet? Register here"
                            } else {
                                "Already have an account? Log in"
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE3A3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFB54708)
                            )
                        }

                        Column {
                            Text(
                                text = "Emergency Mode",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7A2E0E)
                            )
                            Text(
                                text = "Immediate access without creating an account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7A2E0E)
                            )
                        }
                    }

                    Text(
                        text = "Use this when speed matters. You can enter the app instantly, but account-based history and cross-device sync are not available in this mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7A2E0E)
                    )

                    HorizontalDivider(color = Color(0xFFF4D9A2))

                    Button(
                        onClick = { authViewModel.enterEmergencyMode() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFA000)
                        )
                    ) {
                        Text("Continue in Emergency Mode")
                    }
                }
            }

            Text(
                text = "Prototype note: this version stores accounts on the device for now. Real cross-device login should be connected to a backend auth service next.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AuthModeChip(
    selected: Boolean,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color(0xFFEAE6FF) else Color(0xFFF6F8FC)
    val fg = if (selected) Color(0xFF4B3BC8) else Color(0xFF48556A)

    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = text)
    }
}