package com.palash.voicebridge.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.palash.voicebridge.PalashApp
import com.palash.voicebridge.ml.ModelManager
import com.palash.voicebridge.ml.tts.OlChikiTtsEngine
import com.palash.voicebridge.ui.components.OfflineIndicator
import com.palash.voicebridge.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: PalashApp,
    onBack: () -> Unit
) {
    val modelStatuses = remember { app.modelManager.getModelStatuses() }
    
    // TTS Preferences
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tts_prefs", Context.MODE_PRIVATE) }
    val ttsEngine = remember { app.modelManager.ttsEngine as? OlChikiTtsEngine }
    val availableVoices = remember { ttsEngine?.getAvailableVoices() ?: emptyList() }

    var ttsSpeed by remember { mutableFloatStateOf(prefs.getFloat("tts_speed", 0.95f)) }
    var ttsPitch by remember { mutableFloatStateOf(prefs.getFloat("tts_pitch", 1.1f)) }
    var ttsVoiceName by remember { mutableStateOf(prefs.getString("tts_voice_name", null) ?: "") }
    var expandedVoiceDropdown by remember { mutableStateOf(false) }

    fun saveTtsSettings() {
        prefs.edit().apply {
            putFloat("tts_speed", ttsSpeed)
            putFloat("tts_pitch", ttsPitch)
            if (ttsVoiceName.isNotEmpty()) {
                putString("tts_voice_name", ttsVoiceName)
            }
        }.apply()
        ttsEngine?.reloadSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ सेटिंग्स", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { OfflineIndicator() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TTS Settings Section
            Text(
                text = "आवाज़ सेटिंग्स (Voice Settings)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Voice Selection
                    Text("Voice Pack", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    ExposedDropdownMenuBox(
                        expanded = expandedVoiceDropdown,
                        onExpandedChange = { expandedVoiceDropdown = !expandedVoiceDropdown }
                    ) {
                        OutlinedTextField(
                            value = if (ttsVoiceName.isEmpty()) "System Default (Hindi)" else ttsVoiceName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVoiceDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVoiceDropdown,
                            onDismissRequest = { expandedVoiceDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("System Default (Hindi)") },
                                onClick = {
                                    ttsVoiceName = ""
                                    expandedVoiceDropdown = false
                                    saveTtsSettings()
                                }
                            )
                            availableVoices.forEach { voice ->
                                DropdownMenuItem(
                                    text = { Text(voice.name) },
                                    onClick = {
                                        ttsVoiceName = voice.name
                                        expandedVoiceDropdown = false
                                        saveTtsSettings()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Speed Slider
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Speed (गति)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("${(ttsSpeed * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = ttsSpeed,
                        onValueChange = { ttsSpeed = it },
                        onValueChangeFinished = { saveTtsSettings() },
                        valueRange = 0.3f..2.0f
                    )

                    // Pitch Slider
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Pitch (आवाज़ की गहराई)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("${(ttsPitch * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = ttsPitch,
                        onValueChange = { ttsPitch = it },
                        onValueChangeFinished = { saveTtsSettings() },
                        valueRange = 0.3f..2.0f
                    )
                }
            }

            Divider()

            // Model Status Section
            Text(
                text = "मॉडल स्थिति",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            modelStatuses.forEach { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val icon = when (status.type) {
                            ModelManager.ModelType.TRANSLATION -> Icons.Filled.Translate
                            ModelManager.ModelType.ASR -> Icons.Filled.Mic
                            ModelManager.ModelType.TTS -> Icons.Filled.VolumeUp
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (status.isAvailable) OfflineGreen else StatusRed
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Engine: ${status.engineName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (status.isAvailable)
                                OfflineGreen.copy(alpha = 0.12f)
                            else
                                StatusRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (status.isAvailable) "Ready" else "Placeholder",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (status.isAvailable) OfflineGreen else StatusRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Divider()

            // Language Info
            Text(
                text = "भाषा जानकारी",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Source", "Hindi (हिंदी) — hin_Deva")
                    InfoRow("Target", "Santali (ᱥᱟᱱᱛᱟᱲᱤ) — sat_Olck")
                    InfoRow("Script", "Ol Chiki (ᱚᱞ ᱪᱤᱠᱤ)")
                    InfoRow("Mode", "Offline — No internet required")
                }
            }

            Divider()

            // App Info
            Text(
                text = "ऐप जानकारी",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("App", "PALASH VoiceBridge")
                    InfoRow("Version", "1.0.0-phase1")
                    InfoRow("Phase", "Phase 1 — Foundation + UI + Content")
                    InfoRow("Min SDK", "Android 9 (API 28)")
                    InfoRow("Architecture", "MVVM + Repository Pattern")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
