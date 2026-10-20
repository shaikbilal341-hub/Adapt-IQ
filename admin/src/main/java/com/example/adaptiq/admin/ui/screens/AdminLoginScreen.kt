package com.example.adaptiq.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.adaptiq.admin.BuildConfig
import com.example.adaptiq.admin.sync.AdminApiClient
import com.example.adaptiq.admin.sync.AdminLoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdminLoginScreen(
    initialServerUrl: String = AdminApiClient.baseUrl,
    onSaveServerUrl: (String) -> Unit = {},
    onLoginSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("nandakishore.christ@gmail.com") }
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatusMessage by remember { mutableStateOf<String?>(null) }
    var isConnectionSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Adapt-IQ Admin",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authorized Student Researcher Portal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            AdminApiClient.setServerUrl(it)
                            onSaveServerUrl(it)
                            connectionStatusMessage = null
                            errorMessage = null
                        },
                        label = { Text("Backend Server URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick URL Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = serverUrl == BuildConfig.DEFAULT_DEVICE_URL,
                            onClick = {
                                serverUrl = BuildConfig.DEFAULT_DEVICE_URL
                                AdminApiClient.setServerUrl(serverUrl)
                                onSaveServerUrl(serverUrl)
                                connectionStatusMessage = null
                                errorMessage = null
                            },
                            label = { Text("Physical (LAN)", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = serverUrl == BuildConfig.DEFAULT_EMULATOR_URL,
                            onClick = {
                                serverUrl = BuildConfig.DEFAULT_EMULATOR_URL
                                AdminApiClient.setServerUrl(serverUrl)
                                onSaveServerUrl(serverUrl)
                                connectionStatusMessage = null
                                errorMessage = null
                            },
                            label = { Text("Emulator", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = serverUrl == "http://localhost:4000/",
                            onClick = {
                                serverUrl = "http://localhost:4000/"
                                AdminApiClient.setServerUrl(serverUrl)
                                onSaveServerUrl(serverUrl)
                                connectionStatusMessage = null
                                errorMessage = null
                            },
                            label = { Text("USB Proxy", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    // Test Connection Button & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                connectionStatusMessage = null
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        AdminApiClient.setServerUrl(serverUrl)
                                        onSaveServerUrl(serverUrl)
                                        val response = withContext(Dispatchers.IO) {
                                            AdminApiClient.api.checkHealth()
                                        }
                                        isTestingConnection = false
                                        if (response.isSuccessful && response.body() != null) {
                                            isConnectionSuccess = true
                                            val status = response.body()?.get("status")?.asString ?: "healthy"
                                            connectionStatusMessage = "Connected to Backend ($status)"
                                        } else {
                                            isConnectionSuccess = false
                                            connectionStatusMessage = "Server returned code ${response.code()}"
                                        }
                                    } catch (e: Exception) {
                                        isTestingConnection = false
                                        isConnectionSuccess = false
                                        connectionStatusMessage = "Unreachable: ${e.localizedMessage ?: "timeout"}"
                                    }
                                }
                            },
                            enabled = !isTestingConnection && serverUrl.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Text("Test Connection", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        connectionStatusMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = if (isConnectionSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Researcher Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter email and password"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null

                            coroutineScope.launch {
                                try {
                                    AdminApiClient.setServerUrl(serverUrl)
                                    onSaveServerUrl(serverUrl)
                                    val response = withContext(Dispatchers.IO) {
                                        AdminApiClient.api.adminLogin(AdminLoginRequest(email.trim(), password))
                                    }
                                    isLoading = false
                                    if (response.isSuccessful && response.body() != null) {
                                        onLoginSuccess(response.body()!!.token)
                                    } else {
                                        errorMessage = "Invalid credentials or unauthorized"
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Network unavailable or server unreachable (${e.localizedMessage})"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Secure Researcher Login", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
