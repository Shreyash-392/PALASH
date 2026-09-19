package com.palash.voicebridge.ui.video

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.palash.voicebridge.PalashApp
import com.palash.voicebridge.R
import com.palash.voicebridge.ui.theme.*

data class VideoItem(
    val id: String,
    val titleEnglish: String,
    val titleHindi: String,
    val description: String,
    val durationText: String,
    val rawResId: Int?,
    val iconEmoji: String
)

val videoLibrary = listOf(
    VideoItem(
        id = "intro_video",
        titleEnglish = "PALASH Setu Mother-Tongue Learning Video",
        titleHindi = "पलाश सेतु मातृभाषा शिक्षण वीडियो",
        description = "Official classroom demonstration video showing interactive teaching in Santali & Mundari.",
        durationText = "Demonstration",
        rawResId = R.raw.palash_intro_video,
        iconEmoji = "🎬"
    ),
    VideoItem(
        id = "santali_alphabets",
        titleEnglish = "Santali Ol Chiki Letters & Pronunciation",
        titleHindi = "संताली ओल चिकी वर्णमाला एवं उच्चारण",
        description = "Learn the 30 characters of Ol Chiki script step-by-step.",
        durationText = "Educational Lesson",
        rawResId = null,
        iconEmoji = "🔤"
    ),
    VideoItem(
        id = "mundari_numbers",
        titleEnglish = "Mundari Numbers & Classroom Phrases",
        titleHindi = "मुंडारी गिनती एवं कक्षा वाक्य",
        description = "Learn numbers 1-20 and common classroom expressions in Mundari Devanagari.",
        durationText = "Classroom Practice",
        rawResId = null,
        iconEmoji = "🔢"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    app: PalashApp,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedVideo by remember { mutableStateOf(videoLibrary[0]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Video Lessons (वीडियो शिक्षण)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Mother-Tongue Classroom Videos",
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
            // Video Player Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (selectedVideo.rawResId != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)

                                val videoUri = Uri.parse("android.resource://" + ctx.packageName + "/" + selectedVideo.rawResId)
                                setVideoURI(videoUri)
                                start()
                            }
                        },
                        update = { view ->
                            val videoUri = Uri.parse("android.resource://" + context.packageName + "/" + selectedVideo.rawResId)
                            view.setVideoURI(videoUri)
                            view.start()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircleOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = selectedVideo.titleEnglish,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Video preview available in full classroom package.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Video Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = selectedVideo.iconEmoji,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedVideo.titleEnglish,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Text(
                                text = selectedVideo.titleHindi,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SecondaryOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = selectedVideo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Video Playlist Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Video Library (वीडियो संग्रह)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                videoLibrary.forEach { video ->
                    val isSelected = video.id == selectedVideo.id
                    Card(
                        onClick = { selectedVideo = video },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(video.iconEmoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.titleEnglish,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = video.titleHindi,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = if (isSelected) Icons.Filled.PlayArrow else Icons.Filled.PlayCircleOutline,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryBlue else Gray400,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
