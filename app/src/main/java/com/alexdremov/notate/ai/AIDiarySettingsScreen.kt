package com.alexdremov.notate.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.alexdremov.notate.ai.provider.ProviderDescriptor
import com.alexdremov.notate.ai.provider.ProviderSettings
import com.alexdremov.notate.ai.provider.TestFailureCategory
import com.alexdremov.notate.ai.provider.TestResult
import kotlinx.coroutines.launch

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIDiarySettingsDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val providers = remember { AIDiaryPreferences.getAllProviders() }
    var providerId by remember { mutableStateOf(AIDiaryPreferences.getProviderId(context)) }
    var baseUrl by remember { mutableStateOf(AIDiaryPreferences.getBaseUrl(context)) }
    var model by remember { mutableStateOf(AIDiaryPreferences.getModel(context)) }
    var apiKey by remember { mutableStateOf(AIDiaryPreferences.getApiKey(context, providerId) ?: "") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var captureDelaySec by remember { mutableStateOf(AIDiaryPreferences.getCaptureDelayMs(context) / 1000f) }
    var systemPrompt by remember { mutableStateOf(AIDiaryPreferences.getSystemPrompt(context)) }

    var testState by remember { mutableStateOf<TestUiState>(TestUiState.Idle) }

    fun applyProviderDefaults(id: String) {
        val descriptor = providers.find { it.id == id } ?: return
        providerId = id
        baseUrl = descriptor.defaultBaseUrl
        model = descriptor.defaultModel
        apiKey = AIDiaryPreferences.getApiKey(context, id) ?: ""
        testState = TestUiState.Idle
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Diary 设置") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Provider dropdown
                var expanded by remember { mutableStateOf(false) }
                val selectedDescriptor = providers.find { it.id == providerId } ?: providers.last()

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedDescriptor.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        providers.forEach { descriptor ->
                            DropdownMenuItem(
                                text = { Text(descriptor.displayName) },
                                onClick = {
                                    applyProviderDefaults(descriptor.id)
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                if (selectedDescriptor.note.isNotBlank()) {
                    Text(
                        text = selectedDescriptor.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; testState = TestUiState.Idle },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = providerId == "custom",
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; testState = TestUiState.Idle },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = providerId == "custom",
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; testState = TestUiState.Idle },
                    label = { Text("API Key") },
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (apiKeyVisible) "Hide API Key" else "Show API Key",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Test connection
                Button(
                    onClick = {
                        scope.launch {
                            testState = TestUiState.Running
                            val client =
                                AIDiaryApiClient(
                                    ProviderSettings(
                                        baseUrl = baseUrl,
                                        apiKey = apiKey.trim(),
                                        model = model,
                                    ),
                                )
                            testState =
                                when (val result = client.testConnection()) {
                                    is TestResult.Success -> TestUiState.Success(result.summary)
                                    is TestResult.Failure -> TestUiState.Failure(result.category, result.message)
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("测试连接")
                }

                when (val state = testState) {
                    is TestUiState.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(state.summary, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is TestUiState.Failure -> {
                        val (title, color) =
                            when (state.category) {
                                TestFailureCategory.NETWORK -> "网络错误" to MaterialTheme.colorScheme.error
                                TestFailureCategory.AUTH -> "鉴权失败 (401)" to MaterialTheme.colorScheme.error
                                TestFailureCategory.BAD_REQUEST -> "不支持图片输入 (400)" to MaterialTheme.colorScheme.tertiary
                                TestFailureCategory.OTHER -> "其他错误" to MaterialTheme.colorScheme.error
                            }
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = null, tint = color)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(title, color = color, style = MaterialTheme.typography.labelLarge)
                                Text(state.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    TestUiState.Running -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("正在测试...")
                        }
                    }

                    TestUiState.Idle -> Unit
                }

                Text(
                    "Capture delay: ${captureDelaySec.toInt()}s",
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = captureDelaySec,
                    onValueChange = { captureDelaySec = it },
                    valueRange = 1f..5f,
                    steps = 3,
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("人格 / System Prompt") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    maxLines = 5,
                )

                TextButton(
                    onClick = { systemPrompt = AIDiarySession.DEFAULT_SYSTEM_PROMPT },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("恢复默认人格")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    AIDiaryPreferences.setProviderId(context, providerId)
                    AIDiaryPreferences.setBaseUrl(context, baseUrl)
                    AIDiaryPreferences.setModel(context, model)
                    AIDiaryPreferences.setApiKey(context, providerId, apiKey)
                    AIDiaryPreferences.setCaptureDelayMs(context, (captureDelaySec * 1000).toLong())
                    AIDiaryPreferences.setSystemPrompt(context, systemPrompt)
                    onDismiss()
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private sealed class TestUiState {
    object Idle : TestUiState()
    object Running : TestUiState()
    data class Success(val summary: String) : TestUiState()
    data class Failure(val category: TestFailureCategory, val message: String) : TestUiState()
}
