package com.palash.voicebridge.ui.worksheet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palash.voicebridge.PalashApp
import com.palash.voicebridge.domain.model.LanguagePack
import com.palash.voicebridge.ui.theme.*
import java.io.File
import java.io.FileOutputStream

data class WorksheetStep(
    val stepNumber: Int,
    val titleEnglish: String,
    val subtitleEnglish: String,
    val titleHindi: String,
    val descriptionEnglish: String,
    val santaliOlChiki: String,
    val santaliDesc: String,
    val mundariDevanagari: String,
    val mundariDesc: String,
    val iconEmoji: String,
    val cardBgColor: Color
)

data class WorksheetTopic(
    val id: String,
    val titleEnglish: String,
    val taglineEnglish: String,
    val titleHindi: String,
    val iconEmoji: String,
    val steps: List<WorksheetStep>
)

val sampleTopics = listOf(
    WorksheetTopic(
        id = "water_cycle",
        titleEnglish = "The Water Cycle",
        taglineEnglish = "Water moves, changes and comes back!",
        titleHindi = "जल चक्र (Water Cycle)",
        iconEmoji = "💧",
        steps = listOf(
            WorksheetStep(
                stepNumber = 1,
                titleEnglish = "Water in Rivers/Lakes",
                subtitleEnglish = "Water is found in rivers, lakes, seas and ponds.",
                titleHindi = "नदी / झील का पानी",
                descriptionEnglish = "Water is found in rivers, lakes, seas and ponds.",
                santaliOlChiki = "ᱫᱟᱜ ᱜᱟᱰᱟ",
                santaliDesc = "ᱫᱟᱜ ᱫᱚ ᱜᱟᱰᱟ, ᱯᱩᱠᱷᱨᱤ ᱨᱮ ᱧᱟᱢᱚᱜᱼᱟ᱾",
                mundariDevanagari = "गाडा जापाल दाः",
                mundariDesc = "दाः दो गाडा ओंडोः पोखइरे नमोःआ।",
                iconEmoji = "🏞️",
                cardBgColor = Color(0xFFE3F2FD)
            ),
            WorksheetStep(
                stepNumber = 2,
                titleEnglish = "Evaporation",
                subtitleEnglish = "The sun heats the water into vapor.",
                titleHindi = "भाप बनना",
                descriptionEnglish = "The sun heats the water and it changes into vapor (gas).",
                santaliOlChiki = "ᱫᱟᱜ ᱵᱟᱥᱯᱚ",
                santaliDesc = "ᱥᱤᱛᱩᱝ ᱛᱮ ᱫᱟᱜ ᱵᱟᱥᱯᱚᱜᱼᱟ᱾",
                mundariDevanagari = "दाः भाप ओड़ों",
                mundariDesc = "जेठे ते दाः भाप ओड़ोंओःआ।",
                iconEmoji = "☀️",
                cardBgColor = Color(0xFFFFF8E1)
            ),
            WorksheetStep(
                stepNumber = 3,
                titleEnglish = "Water Vapor Rises",
                subtitleEnglish = "The warm water vapor rises into the sky.",
                titleHindi = "भाप ऊपर उठती है",
                descriptionEnglish = "The warm water vapor rises up into the sky.",
                santaliOlChiki = "ᱵᱟᱥᱯᱚ ᱪᱮᱛᱟᱱ ᱨᱟᱠᱟᱵ",
                santaliDesc = "ᱞᱚᱞᱚ ᱵᱟᱥᱯᱚ ᱥᱮᱨᱢᱟ ᱛᱮ ᱨᱟᱠᱟᱵᱼᱟ᱾",
                mundariDevanagari = "भाप चेतान राकाब",
                mundariDesc = "लोलो भाप सिरमा ते राकाबोःआ।",
                iconEmoji = "☁️",
                cardBgColor = Color(0xFFE8EAF6)
            ),
            WorksheetStep(
                stepNumber = 4,
                titleEnglish = "Condensation",
                subtitleEnglish = "Vapor cools down and forms clouds.",
                titleHindi = "बादल बनना",
                descriptionEnglish = "High in the sky, the vapor cools down and forms clouds.",
                santaliOlChiki = "ᱨᱤᱢᱤᱞ ᱵᱮᱱᱟᱣ",
                santaliDesc = "ᱥᱮᱨᱢᱟ ᱨᱮ ᱵᱟᱥᱯᱚ ᱨᱮᱭᱟᱲ ᱠᱟᱛᱮ ᱨᱤᱢᱤᱞ ᱵᱮᱱᱟᱣᱜᱼᱟ᱾",
                mundariDevanagari = "रामायल बाइओः",
                mundariDesc = "सिरमा रे भाप रियाड़ काते रामायल बाइओःआ।",
                iconEmoji = "🌤️",
                cardBgColor = Color(0xFFE0F7FA)
            ),
            WorksheetStep(
                stepNumber = 5,
                titleEnglish = "Precipitation",
                subtitleEnglish = "Clouds become heavy and fall as rain.",
                titleHindi = "वर्षा (बारिश)",
                descriptionEnglish = "The clouds become heavy and the water falls back to Earth as rain.",
                santaliOlChiki = "ᱫᱟᱜ ᱡᱟᱹᱲᱤ",
                santaliDesc = "ᱨᱤᱢᱤᱞ ᱦᱟᱢᱟᱞ ᱠᱟᱛᱮ ᱫᱟᱜ ᱡᱟᱹᱲᱤᱜᱼᱟ᱾",
                mundariDevanagari = "दाः जाड़ि",
                mundariDesc = "रामायल हमाल काते दाः जाड़िओःआ।",
                iconEmoji = "🌧️",
                cardBgColor = Color(0xFFFFEBEE)
            ),
            WorksheetStep(
                stepNumber = 6,
                titleEnglish = "Collection",
                subtitleEnglish = "Rainwater flows back into lakes & rivers.",
                titleHindi = "पानी फिर से इकट्ठा होना",
                descriptionEnglish = "The rainwater flows back into rivers, lakes and the ground.",
                santaliOlChiki = "ᱫᱟᱜ ᱡᱟᱣᱨᱟ",
                santaliDesc = "ᱡᱟᱹᱲᱤ ᱫᱟᱜ ᱟᱨᱦᱚᱸ ᱜᱟᱰᱟ ᱨᱮ ᱡᱟᱣᱨᱟᱜᱼᱟ᱾",
                mundariDevanagari = "दाः जावरा",
                mundariDesc = "जाड़ि दाः ओरहो गाडा रे जावराओःआ।",
                iconEmoji = "🌊",
                cardBgColor = Color(0xFFE8F5E9)
            )
        )
    ),
    WorksheetTopic(
        id = "human_body",
        titleEnglish = "Parts of the Human Body",
        taglineEnglish = "Let's learn the important parts of our body!",
        titleHindi = "मानव शरीर के अंग",
        iconEmoji = "🧍",
        steps = listOf(
            WorksheetStep(
                stepNumber = 1,
                titleEnglish = "Eyes",
                subtitleEnglish = "(We see the world)",
                titleHindi = "आँख",
                descriptionEnglish = "We use our eyes to see colors, people and things.",
                santaliOlChiki = "ᱢᱮᱫ",
                santaliDesc = "ᱢᱮᱫ ᱛᱮ ᱵᱚᱱ ᱧᱮᱞᱟ᱾",
                mundariDevanagari = "मेद (आँख)",
                mundariDesc = "मेद ते आबू लेलोःआ।",
                iconEmoji = "👁️",
                cardBgColor = Color(0xFFFFF3E0)
            ),
            WorksheetStep(
                stepNumber = 2,
                titleEnglish = "Ears",
                subtitleEnglish = "(We hear sounds)",
                titleHindi = "कान",
                descriptionEnglish = "We use our ears to hear sounds like music and birds.",
                santaliOlChiki = "ᱞᱩᱛᱩᱨ",
                santaliDesc = "ᱞᱩᱛᱩᱨ ᱛᱮ ᱵᱚᱱ ᱟᱧᱡᱚᱢᱟ᱾",
                mundariDevanagari = "लुतुर (कान)",
                mundariDesc = "लुतुर ते आबू आंजोमओःआ।",
                iconEmoji = "👂",
                cardBgColor = Color(0xFFE1F5FE)
            ),
            WorksheetStep(
                stepNumber = 3,
                titleEnglish = "Nose",
                subtitleEnglish = "(We smell)",
                titleHindi = "नाक",
                descriptionEnglish = "We use our nose to smell nice things like flowers and food.",
                santaliOlChiki = "ᱢᱩ",
                santaliDesc = "ᱢᱩ ᱛᱮ ᱵᱚᱱ ᱥᱚᱧᱟ᱾",
                mundariDevanagari = "मुँ (नाक)",
                mundariDesc = "मुँ ते आबू सोओःआ।",
                iconEmoji = "👃",
                cardBgColor = Color(0xFFE8F5E9)
            ),
            WorksheetStep(
                stepNumber = 4,
                titleEnglish = "Mouth",
                subtitleEnglish = "(We speak and eat)",
                titleHindi = "मुँह",
                descriptionEnglish = "We use our mouth to eat food and speak.",
                santaliOlChiki = "ᱢᱚᱪᱟ",
                santaliDesc = "ᱢᱚᱪᱟ ᱛᱮ ᱵᱚᱱ ᱡᱚᱢᱟ ᱟᱨ ᱵᱚᱱ ᱨᱚᱲᱟ᱾",
                mundariDevanagari = "मोचा (मुँह)",
                mundariDesc = "मोचा ते आबू जोमओःआ ओंडोः कजिओःआ।",
                iconEmoji = "👄",
                cardBgColor = Color(0xFFF3E5F5)
            ),
            WorksheetStep(
                stepNumber = 5,
                titleEnglish = "Hands",
                subtitleEnglish = "(We work and play)",
                titleHindi = "हाथ",
                descriptionEnglish = "We use our hands to hold, write, play and work.",
                santaliOlChiki = "ᱛᱤ",
                santaliDesc = "ᱛᱤ ᱛᱮ ᱵᱚᱱ ᱚᱞᱟ ᱟᱨ ᱵᱚᱱ ᱠᱟᱹᱢᱤᱭᱟ᱾",
                mundariDevanagari = "ती (हाथ)",
                mundariDesc = "ती ते आबू ओलओःआ ओंडोः कामीओःआ।",
                iconEmoji = "✋",
                cardBgColor = Color(0xFFFFF8E1)
            ),
            WorksheetStep(
                stepNumber = 6,
                titleEnglish = "Legs",
                subtitleEnglish = "(We move)",
                titleHindi = "पाँव",
                descriptionEnglish = "We use our legs to walk, run, jump and explore.",
                santaliOlChiki = "ᱠᱟᱴᱟ",
                santaliDesc = "ᱠᱟᱴᱟ ᱛᱮ ᱵᱚᱱ ᱛᱟᱲᱟᱢᱟ ᱟᱨ ᱵᱚᱱ ᱫᱟᱹᱲᱟ᱾",
                mundariDevanagari = "काटा (पाँव)",
                mundariDesc = "काटा ते आबू ताड़ामओःआ ओंडोः निरओःआ।",
                iconEmoji = "🦵",
                cardBgColor = Color(0xFFE8EAF6)
            )
        )
    ),
    WorksheetTopic(
        id = "seed_growth",
        titleEnglish = "How a Seed Grows into a Plant",
        taglineEnglish = "Learn step-by-step how plants grow!",
        titleHindi = "पौधे का विकास (Seed Growth)",
        iconEmoji = "🌱",
        steps = listOf(
            WorksheetStep(
                stepNumber = 1,
                titleEnglish = "Seed",
                subtitleEnglish = "(बीज)",
                titleHindi = "बीज (Seed)",
                descriptionEnglish = "This is a seed. It holds life inside.",
                santaliOlChiki = "ᱡᱟᱝ",
                santaliDesc = "ᱱᱚᱣᱟ ᱫᱚ ᱢᱤᱫᱴᱟᱝ ᱡᱟᱝ ᱠᱟᱱᱟ᱾",
                mundariDevanagari = "जांग (बीज)",
                mundariDesc = "नेया मियाद जांग तना।",
                iconEmoji = "🌰",
                cardBgColor = Color(0xFFFFF3E0)
            ),
            WorksheetStep(
                stepNumber = 2,
                titleEnglish = "Planting",
                subtitleEnglish = "(बोना)",
                titleHindi = "बोना (Planting)",
                descriptionEnglish = "We plant the seed deep in fertile soil.",
                santaliOlChiki = "Er / ᱨᱚᱦᱚᱭ",
                santaliDesc = "ᱵᱚᱱ ᱡᱟᱝ ᱦᱟᱥᱟ ᱨᱮ ᱵᱚᱱ ᱨᱚᱦᱚᱭᱟ᱾",
                mundariDevanagari = "रोहोय (बोना)",
                mundariDesc = "आबू जांग हसा रे आबू रोहोयओःआ।",
                iconEmoji = "🌱",
                cardBgColor = Color(0xFFE8F5E9)
            ),
            WorksheetStep(
                stepNumber = 3,
                titleEnglish = "Water",
                subtitleEnglish = "(पानी)",
                titleHindi = "पानी (Watering)",
                descriptionEnglish = "We water the plant every day.",
                santaliOlChiki = "ᱫᱟᱜ",
                santaliDesc = "ᱵᱚᱱ ᱡᱟᱝ ᱨᱮ ᱫᱟᱜ ᱵᱚᱱ ᱫᱩᱞᱟ᱾",
                mundariDevanagari = "दाः (पानी)",
                mundariDesc = "आबू जांग रे दाः आबू दुलओःआ।",
                iconEmoji = "🚿",
                cardBgColor = Color(0xFFE3F2FD)
            ),
            WorksheetStep(
                stepNumber = 4,
                titleEnglish = "Sunlight",
                subtitleEnglish = "(धूप)",
                titleHindi = "धूप (Sunlight)",
                descriptionEnglish = "The sun gives warmth and light to grow.",
                santaliOlChiki = "ᱥᱤᱛᱩᱝ",
                santaliDesc = "ᱥᱤᱛᱩᱝ ᱛᱮ ᱦᱟᱨᱟᱜᱼᱟ᱾",
                mundariDevanagari = "जेठे (धूप)",
                mundariDesc = "जेठे ते आबू हराओःआ।",
                iconEmoji = "☀️",
                cardBgColor = Color(0xFFFFF8E1)
            ),
            WorksheetStep(
                stepNumber = 5,
                titleEnglish = "Sprout",
                subtitleEnglish = "(अंकुर)",
                titleHindi = "अंकुर (Sprout)",
                descriptionEnglish = "After a few days, a green sprout comes out.",
                santaliOlChiki = "ᱜᱮᱡᱮᱲ / ᱚᱢᱚᱱ",
                santaliDesc = "ᱛᱤᱱᱟᱹᱜ ᱫᱤᱱ ᱠᱷᱟᱱ ᱚᱢᱚᱱᱚᱜᱼᱟ᱾",
                mundariDevanagari = "ओमोन (अंकुर)",
                mundariDesc = "चीनां दिन रे ओमोनओःआ।",
                iconEmoji = "🌿",
                cardBgColor = Color(0xFFE0F7FA)
            ),
            WorksheetStep(
                stepNumber = 6,
                titleEnglish = "Plant",
                subtitleEnglish = "(पौधा)",
                titleHindi = "पौधा (Full Plant)",
                descriptionEnglish = "The sprout grows into a green healthy plant!",
                santaliOlChiki = "ᱫᱟᱨᱮ",
                santaliDesc = "ᱦᱟᱨᱟ ᱠᱟᱛᱮ ᱢᱤᱫᱴᱟᱝ ᱫᱟᱨᱮ ᱵᱮᱱᱟᱣᱜᱼᱟ᱾",
                mundariDevanagari = "दारु (पौधा)",
                mundariDesc = "हरा काते मियाद दारु बाइओःआ।",
                iconEmoji = "🌳",
                cardBgColor = Color(0xFFE8EAF6)
            )
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorksheetScreen(
    app: PalashApp,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(LanguagePack.SANTALI) }
    var selectedTopic by remember { mutableStateOf(sampleTopics[0]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PALASH Setu (पलाश सेतु)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Learning in the child's mother tongue",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryOrangeLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PrimaryBlue, PrimaryBlueDark)
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🏫 PALASH Setu Worksheets",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Interactive Bilingual Primary School Content",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )

                    // Language Selector Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        FilterChip(
                            selected = selectedLanguage == LanguagePack.SANTALI,
                            onClick = { selectedLanguage = LanguagePack.SANTALI },
                            label = { Text("Santali ( Ol Chiki ᱚᱞ ᱪᱤᱠᱤ )") },
                            leadingIcon = if (selectedLanguage == LanguagePack.SANTALI) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SecondaryOrange,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.2f),
                                labelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = selectedLanguage == LanguagePack.MUNDARI,
                            onClick = { selectedLanguage = LanguagePack.MUNDARI },
                            label = { Text("Mundari ( Devanagari मुंडारी )") },
                            leadingIcon = if (selectedLanguage == LanguagePack.MUNDARI) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SecondaryOrange,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.2f),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Topic Selector Row
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Select Lesson Topic:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleTopics.forEach { topic ->
                        val isSelected = topic.id == selectedTopic.id
                        Button(
                            onClick = { selectedTopic = topic },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(topic.iconEmoji, fontSize = 22.sp)
                                Text(
                                    text = topic.titleEnglish,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Topic Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SecondaryOrange.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryOrange.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedTopic.iconEmoji, fontSize = 40.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTopic.titleEnglish,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = selectedTopic.titleHindi,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedTopic.taglineEnglish,
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }

                    // Export PDF Button
                    Button(
                        onClick = {
                            generatePdfWorksheet(context, selectedTopic, selectedLanguage)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export PDF", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6-Step Worksheet Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                selectedTopic.steps.chunked(2).forEach { rowSteps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSteps.forEach { step ->
                            WorksheetCard(
                                step = step,
                                language = selectedLanguage,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowSteps.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WorksheetCard(
    step: WorksheetStep,
    language: LanguagePack,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = step.cardBgColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Number Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryBlue,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = step.stepNumber.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(step.iconEmoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step English & Hindi Title
            Text(
                text = step.titleEnglish,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                textAlign = TextAlign.Center
            )

            Text(
                text = step.titleHindi,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Native Language Translation Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val nativeText = if (language == LanguagePack.SANTALI) step.santaliOlChiki else step.mundariDevanagari
                    val nativeDesc = if (language == LanguagePack.SANTALI) step.santaliDesc else step.mundariDesc

                    Text(
                        text = nativeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = nativeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = step.descriptionEnglish,
                style = MaterialTheme.typography.labelSmall,
                color = Gray600,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

private fun generatePdfWorksheet(context: Context, topic: WorksheetTopic, language: LanguagePack) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 16f
            isFakeBoldText = true
        }

        val bluePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1E3A8A")
            textSize = 20f
            isFakeBoldText = true
        }

        canvas.drawText("PALASH Setu Worksheets", 180f, 40f, bluePaint)
        paint.textSize = 14f
        canvas.drawText("Topic: " + topic.titleEnglish + " (" + topic.titleHindi + ")", 40f, 70f, paint)
        canvas.drawText("Target Language: " + language.name + " (" + language.script + ")", 40f, 90f, paint)

        var y = 130f
        for (step in topic.steps) {
            paint.textSize = 12f
            paint.isFakeBoldText = true
            val lineTitle = "" + step.stepNumber + ". " + step.titleEnglish + " - " + step.titleHindi
            canvas.drawText(lineTitle, 40f, y, paint)

            paint.isFakeBoldText = false
            paint.textSize = 11f
            val nativeText = if (language == LanguagePack.SANTALI) step.santaliOlChiki else step.mundariDevanagari
            canvas.drawText("Mother Tongue: " + nativeText, 60f, y + 16f, paint)
            canvas.drawText("Explanation: " + step.descriptionEnglish, 60f, y + 32f, paint)

            y += 60f
        }

        pdfDocument.finishPage(page)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "PALASH_Worksheet_" + topic.id + "_" + language.bcp47 + ".pdf"
        val file = File(downloadsDir, fileName)

        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        Toast.makeText(context, "Saved Worksheet PDF to Download/" + fileName, Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "PDF Export error: " + e.message, Toast.LENGTH_SHORT).show()
    }
}
