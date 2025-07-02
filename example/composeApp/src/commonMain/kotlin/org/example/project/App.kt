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

@Composable
@Preview
fun App() {
    MaterialTheme {
        AudioRecorderPlayerApp()
    }
}

@Composable
fun AudioRecorderPlayerApp() {
    val viewModel: AudioViewModel = viewModel()
    
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