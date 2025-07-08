package org.example.project.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.hyochan.audio.createAudioRecorderPlayer
import io.github.hyochan.audio.AudioRecorderPlayerProperties
import io.github.hyochan.audio.RecorderAudioSet

class AudioViewModel : ViewModel() {
    
    private val audioRecorderPlayer = createAudioRecorderPlayer()
    
    private val _recordTime = MutableStateFlow("00:00:00")
    val recordTime: StateFlow<String> = _recordTime.asStateFlow()
    
    private val _playTime = MutableStateFlow("00:00:00")
    val playTime: StateFlow<String> = _playTime.asStateFlow()
    
    private val _duration = MutableStateFlow("00:00:00")
    val duration: StateFlow<String> = _duration.asStateFlow()
    
    private val _playProgress = MutableStateFlow(0f)
    val playProgress: StateFlow<Float> = _playProgress.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _isRecordingPaused = MutableStateFlow(false)
    val isRecordingPaused: StateFlow<Boolean> = _isRecordingPaused.asStateFlow()
    
    private val _isPlayingPaused = MutableStateFlow(false)
    val isPlayingPaused: StateFlow<Boolean> = _isPlayingPaused.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _recordingInfo = MutableStateFlow<String?>(null)
    val recordingInfo: StateFlow<String?> = _recordingInfo.asStateFlow()
    
    private val _meteringLevel = MutableStateFlow(0f)
    val meteringLevel: StateFlow<Float> = _meteringLevel.asStateFlow()
    
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    private var recordedFilePath: String? = null
    
    init {
        setupListeners()
        loadRecordingInfo()
        // Enable metering for demo
        audioRecorderPlayer.setPlayerProperties(
            AudioRecorderPlayerProperties(
                updateIntervalMs = 25L,
                meteringEnabled = true
            )
        )
    }
    
    private fun setupListeners() {
        audioRecorderPlayer.addRecordingListener { progress ->
            _recordTime.value = progress.formattedTime
        }
        
        audioRecorderPlayer.addPlaybackListener { progress ->
            _playTime.value = progress.formattedCurrentTime
            _duration.value = progress.formattedDuration
            _playProgress.value = if (progress.duration > 0) {
                progress.currentPosition.toFloat() / progress.duration.toFloat()
            } else 0f
            
            // Check if playback completed (reached the end)
            if (progress.duration > 0 && progress.currentPosition >= progress.duration) {
                println("🎵 ViewModel detected playback completion - keeping final state")
                _isPlaying.value = false
                _isPlayingPaused.value = false
                // Keep the progress at 100% and time at duration
                _playProgress.value = 1f
                _playTime.value = progress.formattedDuration
            }
        }
        
        // Add metering listener
        audioRecorderPlayer.addAudioMeteringListener { meteringInfo ->
            // Android now sends normalized values (0-1), iOS sends dB values
            // Check if the value is already normalized (between 0 and 1) or needs conversion from dB
            val normalizedLevel = if (meteringInfo.averagePower >= 0f && meteringInfo.averagePower <= 1f) {
                // Already normalized (Android)
                meteringInfo.averagePower
            } else {
                // dB value (iOS), convert to 0-1 range
                // -60dB = 0, 0dB = 1
                ((meteringInfo.averagePower + 60f) / 60f).coerceIn(0f, 1f)
            }
            println("📊 ViewModel Metering: raw=${meteringInfo.averagePower}, normalized=$normalizedLevel")
            _meteringLevel.value = normalizedLevel
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            audioRecorderPlayer.startRecording().fold(
                onSuccess = { filePath ->
                    recordedFilePath = filePath
                    _isRecording.value = true
                    _isRecordingPaused.value = false
                    _errorMessage.value = null
                },
                onFailure = { exception ->
                    _errorMessage.value = "Recording failed: ${exception.message}"
                }
            )
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            audioRecorderPlayer.stopRecording().fold(
                onSuccess = { filePath ->
                    recordedFilePath = filePath
                    _isRecording.value = false
                    _isRecordingPaused.value = false
                    _recordTime.value = "00:00:00"
                    loadRecordingInfo() // Update recording info after stopping
                },
                onFailure = { exception ->
                    _errorMessage.value = "Stop recording failed: ${exception.message}"
                }
            )
        }
    }
    
    fun pauseRecording() {
        viewModelScope.launch {
            audioRecorderPlayer.pauseRecording().fold(
                onSuccess = {
                    _isRecordingPaused.value = true
                },
                onFailure = { exception ->
                    _errorMessage.value = "Pause recording failed: ${exception.message}"
                }
            )
        }
    }
    
    fun resumeRecording() {
        viewModelScope.launch {
            audioRecorderPlayer.resumeRecording().fold(
                onSuccess = {
                    _isRecordingPaused.value = false
                },
                onFailure = { exception ->
                    _errorMessage.value = "Resume recording failed: ${exception.message}"
                }
            )
        }
    }
    
    fun startPlaying() {
        viewModelScope.launch {
            audioRecorderPlayer.startPlaying(recordedFilePath).fold(
                onSuccess = {
                    _isPlaying.value = true
                    _isPlayingPaused.value = false
                    _errorMessage.value = null
                },
                onFailure = { exception ->
                    _errorMessage.value = "Playback failed: ${exception.message}"
                }
            )
        }
    }
    
    fun stopPlaying() {
        viewModelScope.launch {
            audioRecorderPlayer.stopPlaying().fold(
                onSuccess = {
                    _isPlaying.value = false
                    _isPlayingPaused.value = false
                    _playTime.value = "00:00:00"
                    _playProgress.value = 0f
                },
                onFailure = { exception ->
                    _errorMessage.value = "Stop playback failed: ${exception.message}"
                }
            )
        }
    }
    
    fun pausePlaying() {
        viewModelScope.launch {
            audioRecorderPlayer.pausePlaying().fold(
                onSuccess = {
                    _isPlayingPaused.value = true
                },
                onFailure = { exception ->
                    _errorMessage.value = "Pause playback failed: ${exception.message}"
                }
            )
        }
    }
    
    fun resumePlaying() {
        viewModelScope.launch {
            audioRecorderPlayer.resumePlaying().fold(
                onSuccess = {
                    _isPlayingPaused.value = false
                },
                onFailure = { exception ->
                    _errorMessage.value = "Resume playback failed: ${exception.message}"
                }
            )
        }
    }
    
    fun seekTo(position: Float) {
        if (_duration.value != "00:00:00") {
            val durationMs = parseDuration(_duration.value)
            val seekPositionMs = (position * durationMs).toLong()
            
            viewModelScope.launch {
                audioRecorderPlayer.seekTo(seekPositionMs).fold(
                    onSuccess = {
                        // Seek successful
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Seek failed: ${exception.message}"
                    }
                )
            }
        }
    }
    
    fun setVolume(volume: Float) {
        viewModelScope.launch {
            audioRecorderPlayer.setVolume(volume).fold(
                onSuccess = {
                    // Volume set successfully
                },
                onFailure = { exception ->
                    _errorMessage.value = "Set volume failed: ${exception.message}"
                }
            )
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun setUpdateInterval(intervalMs: Long) {
        audioRecorderPlayer.setPlayerProperties(
            AudioRecorderPlayerProperties(updateIntervalMs = intervalMs)
        )
    }
    
    fun setRecorderAudioSettings(audioSet: RecorderAudioSet) {
        audioRecorderPlayer.setRecorderProperties(audioSet)
    }
    
    private fun parseDuration(formattedTime: String): Long {
        return try {
            val parts = formattedTime.split(":")
            if (parts.size == 3) {
                val minutes = parts[0].toLong()
                val seconds = parts[1].toLong()
                val centiseconds = parts[2].toLong() // 0.01초 단위
                (minutes * 60 + seconds) * 1000 + centiseconds * 10
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun loadRecordingInfo() {
        viewModelScope.launch {
            try {
                val result = audioRecorderPlayer.getRecordingInfo()
                result.fold(
                    onSuccess = { info ->
                        _recordingInfo.value = info?.let { 
                            "Duration: ${it.formattedDuration} | Size: ${it.formattedFileSize}"
                        }
                    },
                    onFailure = { 
                        // Ignore error, just don't show info
                    }
                )
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            audioRecorderPlayer.setPlaybackSpeed(speed).fold(
                onSuccess = {
                    _playbackSpeed.value = speed
                },
                onFailure = { exception ->
                    _errorMessage.value = "Set playback speed failed: ${exception.message}"
                }
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        audioRecorderPlayer.removeListeners()
    }
}
