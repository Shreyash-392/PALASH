package com.palash.voicebridge.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.palash.voicebridge.PalashApp
import com.palash.voicebridge.domain.model.LanguagePack
import com.palash.voicebridge.ui.components.*
import com.palash.voicebridge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTranslationScreen(
    app: PalashApp,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val viewModel: VoiceTranslationViewModel = viewModel(
        factory = VoiceTranslationViewModel.Factory(app)
    )
    val uiState by viewModel.uiState.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    val quickPhrases = listOf(
        "नमस्ते बच्चों!",
        "आज हम गिनती सीखेंगे।",
        "दो और तीन को जोड़ो।",
        "यह कौन सा आकार है?",
        "कितने सेब हैं?",
        "अपनी किताब खोलो।",
        "ध्यान से सुनो।",
        "बहुत अच्छा!"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "हिंदी → ${uiState.selectedLanguage.name} अनुवाद",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { OfflineIndicator() }
    ) { paddingValues ->
        if (isTablet) {
            // ═══════════════════════════════════════
            // TABLET / LANDSCAPE TWO-PANE LAYOUT
            // ═══════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // LEFT PANE: Input Controls & Voice Mic
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InputControlsSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        quickPhrases = quickPhrases,
                        hasAudioPermission = hasAudioPermission,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }

                // RIGHT PANE: Main Translation Output Slate
                Column(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TranslationOutputSection(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // ═══════════════════════════════════════
            // PHONE / PORTRAIT SINGLE-COLUMN LAYOUT
            // ═══════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InputControlsSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    quickPhrases = quickPhrases,
                    hasAudioPermission = hasAudioPermission,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )

                TranslationOutputSection(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputControlsSection(
    uiState: VoiceUiState,
    viewModel: VoiceTranslationViewModel,
    quickPhrases: List<String>,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    // LANGUAGE SELECTOR ROW (Santali vs Mundari)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = uiState.selectedLanguage == LanguagePack.SANTALI,
            onClick = { viewModel.selectLanguage(LanguagePack.SANTALI) },
            label = { Text("Santali (Ol Chiki)") },
            leadingIcon = if (uiState.selectedLanguage == LanguagePack.SANTALI) {
                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
        )
        Spacer(modifier = Modifier.width(12.dp))
        FilterChip(
            selected = uiState.selectedLanguage == LanguagePack.MUNDARI,
            onClick = { viewModel.selectLanguage(LanguagePack.MUNDARI) },
            label = { Text("Mundari (Devanagari)") },
            leadingIcon = if (uiState.selectedLanguage == LanguagePack.MUNDARI) {
                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
        )
    }

    // MICROPHONE BUTTON
    MicrophoneButton(
        isListening = uiState.isListening,
        isProcessing = uiState.isProcessing,
        amplitude = uiState.audioAmplitude,
        onClick = {
            if (hasAudioPermission) {
                viewModel.toggleListening()
            } else {
                onRequestPermission()
            }
        }
    )

    // Status text
    Text(
        text = when {
            uiState.isListening -> "🎙️ सुन रहे हैं… हिंदी में बोलें"
            uiState.isProcessing -> "⏳ अनुवाद हो रहा है…"
            uiState.santaliTranslation.isNotEmpty() -> "✅ अनुवाद तैयार — दाहिने/नीचे देखें"
            else -> "🎤 माइक दबाएँ और हिंदी में बोलें"
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    // TEXT INPUT (alternative to mic)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "✍️ या हिंदी में टाइप करें:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.customInputText,
                onValueChange = { viewModel.updateCustomInputText(it) },
                placeholder = { Text("जैसे: दो और तीन को जोड़ो") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { viewModel.translateCustomText() },
                enabled = uiState.customInputText.isNotEmpty() && !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("अनुवाद करें (Translate)")
            }
        }
    }

    // QUICK PHRASE CHIPS
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "कक्षा के वाक्य (Quick Phrases):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPhrases.forEach { phrase ->
                SuggestionChip(
                    onClick = {
                        viewModel.updateCustomInputText(phrase)
                        viewModel.translateCustomText(phrase)
                    },
                    label = { Text(phrase, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun TranslationOutputSection(
    uiState: VoiceUiState,
    viewModel: VoiceTranslationViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Title
        Text(
            text = "🎯 मुख्य अनुवाद बोर्ड (Main Translation Slate)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // HINDI TRANSCRIPT (what was recognized)
        if (uiState.hindiTranscript.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "हिंदी (Recognized Hindi):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.hindiTranscript,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // TARGET TRANSLATION OUTPUT (the main output)
        if (uiState.santaliTranslation.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryBlue.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val scriptHeader = if (uiState.selectedLanguage == LanguagePack.SANTALI) {
                        "संताली — ᱚᱞ ᱪᱤᱠᱤ (Santali Ol Chiki):"
                    } else {
                        "मुंडारी — देवनागरी (Mundari Devanagari):"
                    }

                    Text(
                        text = scriptHeader,
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.santaliTranslation,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp
                    )

                    if (uiState.isVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OfflineGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✅ सत्यापित अनुवाद (Verified)",
                                style = MaterialTheme.typography.labelSmall,
                                color = OfflineGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Play Audio Button
                    AudioPlayButton(
                        onClick = { viewModel.playSantaliAudio() },
                        isPlaying = uiState.isPlayingAudio,
                        enabled = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Clear button
            TextButton(
                onClick = { viewModel.clearResults() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("साफ़ करें (Clear)")
            }
        } else {
            // PLACEHOLDER SLATE FOR TABLET / SCREEN WHEN EMPTY
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "अनुवाद बोर्ड (Translation Slate)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (uiState.selectedLanguage == LanguagePack.SANTALI)
                            "Target Language: Santali ( Ol Chiki Script ᱚᱞ ᱪᱤᱠᱤ )"
                        else
                            "Target Language: Mundari ( Devanagari Script )",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )

                    Text(
                        text = "बायें / ऊपर माइक दबाएँ या हिंदी पाठ लिखें — यहाँ तुरंत ऑफ़लाइन अनुवाद दिखाई देगा।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ERROR MESSAGE
        if (uiState.error.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = uiState.error,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun MicrophoneButton(
    isListening: Boolean,
    isProcessing: Boolean,
    amplitude: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val dynamicScale = if (isListening) {
        1.0f + (amplitude * 0.35f)
    } else {
        1.0f
    }

    Box(contentAlignment = Alignment.Center) {
        if (isListening) {
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale * dynamicScale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {}
        }

        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(80.dp)
                .scale(dynamicScale),
            shape = CircleShape,
            containerColor = if (isListening)
                StatusRed
            else if (isProcessing)
                SecondaryOrange
            else
                MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = when {
                    isListening -> Icons.Filled.Stop
                    isProcessing -> Icons.Filled.HourglassTop
                    else -> Icons.Filled.Mic
                },
                contentDescription = "Microphone",
                modifier = Modifier.size(38.dp),
                tint = Color.White
            )
        }
    }
}
