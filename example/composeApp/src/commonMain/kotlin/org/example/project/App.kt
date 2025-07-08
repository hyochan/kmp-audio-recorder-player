package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.audio.AudioViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
@Preview
fun App() {
    MaterialTheme {
        AudioRecorderPlayerApp()
    }
}

@Composable
fun AudioRecorderPlayerApp() {
    val viewModel: AudioViewModel = viewModel { AudioViewModel() }
    
    // Collect state from ViewModel
    val recordTime by viewModel.recordTime.collectAsState()
    val playTime by viewModel.playTime.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playProgress by viewModel.playProgress.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isRecordingPaused by viewModel.isRecordingPaused.collectAsState()
    val isPlayingPaused by viewModel.isPlayingPaused.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val recordingInfo by viewModel.recordingInfo.collectAsState()
    val meteringLevel by viewModel.meteringLevel.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF455A64))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Audio Recorder Player!!",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 84.dp)
        )

        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Record time display
        Text(
            text = recordTime,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.W200,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(top = 32.dp)
        )
        
        // Audio metering display (only show when recording)
        if (isRecording) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recording Level",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Metering bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFF263238), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(meteringLevel)
                            .background(
                                when {
                                    meteringLevel > 0.8f -> Color.Red
                                    meteringLevel > 0.6f -> Color.Yellow
                                    else -> Color.Green
                                },
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
                
                Text(
                    text = "${(meteringLevel * 100).roundToInt()}%",
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Recording info display
        recordingInfo?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F))
            ) {
                Text(
                    text = info,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Recording controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AudioButton(
                    text = "Record",
                    onClick = { viewModel.startRecording() },
                    enabled = !isRecording && !isPlaying
                )
                AudioButton(
                    text = "Pause",
                    onClick = { viewModel.pauseRecording() },
                    enabled = isRecording && !isRecordingPaused
                )
                AudioButton(
                    text = "Resume",
                    onClick = { viewModel.resumeRecording() },
                    enabled = isRecording && isRecordingPaused
                )
                AudioButton(
                    text = "Stop",
                    onClick = { viewModel.stopRecording() },
                    enabled = isRecording
                )
            }
        }

        // Player section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .height(4.dp)
                    .background(Color(0xFFCCCCCC))
                    .clickable { 
                        // TODO: Calculate position based on click and seek
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(playProgress)
                        .background(Color.White)
                )
            }  

            // Time display
            Text(
                text = "$playTime / $duration",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // Playback speed control
            if (isPlaying) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                    .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Playback Speed: ${playbackSpeed}x",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Slider(
                        value = playbackSpeed,
                        onValueChange = { speed ->
                            viewModel.setPlaybackSpeed(speed)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5, // 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF37474F)
                        )
                    )
                    
                    // Speed preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                            TextButton(
                                onClick = { viewModel.setPlaybackSpeed(speed) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (playbackSpeed == speed) Color.White else Color(0xFFCCCCCC)
                                )
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Player controls
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AudioButton(
                    text = "Play",
                    onClick = { viewModel.startPlaying() },
                    enabled = !isRecording && !isPlaying
                )
                AudioButton(
                    text = "Pause",
                    onClick = { viewModel.pausePlaying() },
                    enabled = isPlaying && !isPlayingPaused
                )
                AudioButton(
                    text = "Resume",
                    onClick = { viewModel.resumePlaying() },
                    enabled = isPlaying && isPlayingPaused
                )
                AudioButton(
                    text = "Stop",
                    onClick = { viewModel.stopPlaying() },
                    enabled = isPlaying
                )
            }
        }
    }
}

@Composable
fun AudioButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minWidth = 88.dp, minHeight = 48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.Gray
        )
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}