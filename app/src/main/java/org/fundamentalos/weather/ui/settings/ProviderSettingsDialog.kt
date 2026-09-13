package org.fundamentalos.weather.ui.settings

import org.fundamentalos.weather.ui.text.providerDisplayName
import org.fundamentalos.weather.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.weather.provider.ProviderRegistry
import org.fundamentalos.weather.weather.provider.WeatherService
import org.fundamentalos.weather.weather.provider.qweather.QWeatherCredentialStore
import org.fundamentalos.weather.weather.provider.qweather.QWeatherCredentials
import org.fundamentalos.weather.weather.provider.qweather.QWeatherProvider
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ProviderSettingsDialog(
    onDismiss: () -> Unit,
    weatherService: WeatherService = koinInject(),
    qWeatherCredentialStore: QWeatherCredentialStore = koinInject(),
    qWeatherProvider: QWeatherProvider = koinInject(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val languageTag = resources.configuration.locales.toLanguageTags()
    val scope = rememberCoroutineScope()
    val existingCredentials = remember {
        runCatching { qWeatherCredentialStore.getCredentials() }.getOrNull()
    }

    var selectedProviderId by remember { mutableStateOf(weatherService.selectedProviderId) }
    var host by remember { mutableStateOf(existingCredentials?.host ?: "devapi.qweather.com") }
    var projectId by remember { mutableStateOf(existingCredentials?.projectId.orEmpty()) }
    var keyId by remember { mutableStateOf(existingCredentials?.keyId.orEmpty()) }
    var privateKeyInput by remember { mutableStateOf("") }
    var statusText by remember(languageTag) {
        mutableStateOf(
            qWeatherCredentialStore.fingerprint()?.let { resources.getString(R.string.qweather_configured, it.take(23)) }.orEmpty()
        )
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            privateKeyInput = content.getOrNull().orEmpty()
            statusText = if (privateKeyInput.isBlank()) resources.getString(R.string.key_read_failed) else resources.getString(R.string.key_read)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resources.getString(R.string.weather_api)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(resources.getString(R.string.source), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weatherService.providers.forEach { provider ->
                        FilterChip(
                            selected = selectedProviderId == provider.id,
                            onClick = {
                                weatherService.selectProvider(provider.id)
                                selectedProviderId = provider.id
                                statusText = resources.getString(R.string.provider_selected, providerDisplayName(resources, provider.id, provider.name))
                            },
                            label = { Text(providerDisplayName(resources, provider.id, provider.name)) },
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(resources.getString(R.string.qweather_direct), style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(resources.getString(R.string.api_host)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = projectId,
                    onValueChange = { projectId = it },
                    label = { Text(resources.getString(R.string.project_id)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = keyId,
                    onValueChange = { keyId = it },
                    label = { Text(resources.getString(R.string.key_id)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = privateKeyInput,
                    onValueChange = { privateKeyInput = it },
                    label = { Text(resources.getString(R.string.private_key)) },
                    minLines = 3,
                    maxLines = 5,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Text(resources.getString(R.string.select_pem))
                    }
                    TextButton(onClick = {
                        runCatching {
                            qWeatherCredentialStore.save(
                                QWeatherCredentials(
                                    host = host,
                                    projectId = projectId,
                                    keyId = keyId,
                                    privateKeyPem = privateKeyInput.ifBlank {
                                        existingCredentials?.privateKeyPem.orEmpty()
                                    },
                                )
                            )
                        }.fold(
                            onSuccess = {
                                privateKeyInput = ""
                                statusText = qWeatherCredentialStore.fingerprint()
                                    ?.let { resources.getString(R.string.saved_fingerprint, it.take(23)) }
                                    ?: resources.getString(R.string.saved)
                            },
                            onFailure = { statusText = resources.getString(R.string.save_failed) },
                        )
                    }) {
                        Text(resources.getString(R.string.save))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            statusText = resources.getString(R.string.connection_testing)
                            statusText = runCatching {
                                qWeatherProvider.testConnection()
                            }.fold(
                                onSuccess = { resources.getString(R.string.connection_success) },
                                onFailure = { resources.getString(R.string.connection_failed) },
                            )
                        }
                    }) {
                        Text(resources.getString(R.string.test_connection))
                    }
                    Button(
                        onClick = {
                            qWeatherCredentialStore.clear()
                            privateKeyInput = ""
                            statusText = resources.getString(R.string.credentials_cleared)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(resources.getString(R.string.clear_credentials))
                    }
                }

                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (selectedProviderId == ProviderRegistry.QWeatherProviderId && !qWeatherCredentialStore.hasCredentials()) {
                    Text(
                        text = resources.getString(R.string.credentials_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(resources.getString(R.string.done))
            }
        },
    )
}
