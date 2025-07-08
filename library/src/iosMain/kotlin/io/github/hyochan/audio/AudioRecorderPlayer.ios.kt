package io.github.hyochan.audio

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import platform.AVFAudio.*
import platform.AVFoundation.*
import platform.Foundation.*
import platform.AudioToolbox.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
/**
 * iOS implementation of AudioRecorderPlayer using AVAudioRecorder and AVAudioPlayer
 */
class IOSAudioRecorderPlayer : AudioRecorderPlayer {
    
    private var audioRecorder: AVAudioRecorder? = null
    private var audioPlayer: AVAudioPlayer? = null
    
    private var recordingListener: ((RecordingProgress) -> Unit)? = null
    private var playbackListener: ((PlaybackProgress) -> Unit)? = null
    private var audioMeteringListener: ((AudioMeteringInfo) -> Unit)? = null
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private var meteringJob: Job? = null
    
    private var isRecording = false
    private var isPlaying = false
    private var recordingStartTime = 0L
    private var lastRecordedFileURL: NSURL? = null
    
    // Configuration properties
    private var properties = AudioRecorderPlayerProperties()
    private var recorderAudioSet = RecorderAudioSet()
    
    override suspend fun startRecording(filePath: String?): Result<String> {
        return try {
            if (isRecording) {
                return Result.failure(Exception("Already recording"))
            }
            
            // Request microphone permission
            val audioSession = AVAudioSession.sharedInstance()
            memScoped {
                val sessionError = alloc<ObjCObjectVar<NSError?>>()
                sessionError.value = null
                
                audioSession.setCategory(AVAudioSessionCategoryRecord, error = sessionError.ptr)
                if (sessionError.value != null) {
                    return Result.failure(Exception("Failed to set audio session category: ${sessionError.value?.localizedDescription}"))
                }
                
                audioSession.setActive(true, error = sessionError.ptr)
                if (sessionError.value != null) {
                    return Result.failure(Exception("Failed to activate audio session: ${sessionError.value?.localizedDescription}"))
                }
            }
            
            val outputURL = filePath?.let { NSURL.fileURLWithPath(it) } 
                ?: getDefaultRecordingURL()
            
            // Build settings from RecorderAudioSet
            val settings = mutableMapOf<Any?, Any?>()
            
            // Apply format settings
            when (recorderAudioSet.avFormatIDKeyIOS) {
                AVEncodingType.AAC -> settings[AVFormatIDKey] = 1633772320u // kAudioFormatMPEG4AAC
                AVEncodingType.LPCM -> settings[AVFormatIDKey] = 1819304813u // kAudioFormatLinearPCM
                else -> settings[AVFormatIDKey] = 1633772320u // Default to AAC
            }
            
            // Apply sample rate
            settings[AVSampleRateKey] = recorderAudioSet.avSampleRateKeyIOS?.toDouble() ?: 44100.0
            
            // Apply number of channels
            settings[AVNumberOfChannelsKey] = recorderAudioSet.avNumberOfChannelsKeyIOS ?: 2
            
            // Apply audio quality
            val audioQuality = when (recorderAudioSet.avEncoderAudioQualityKeyIOS) {
                AVEncoderAudioQualityIOSType.MIN -> AVAudioQualityMin
                AVEncoderAudioQualityIOSType.LOW -> AVAudioQualityLow
                AVEncoderAudioQualityIOSType.MEDIUM -> AVAudioQualityMedium
                AVEncoderAudioQualityIOSType.HIGH -> AVAudioQualityHigh
                AVEncoderAudioQualityIOSType.MAX -> AVAudioQualityMax
                else -> AVAudioQualityHigh
            }
            settings[AVEncoderAudioQualityKey] = audioQuality.toLong()
            
            // Apply bit rate if specified
            recorderAudioSet.avEncoderBitRateKeyIOS?.let { bitRate ->
                settings[AVEncoderBitRateKey] = bitRate
            }
            
            // Apply Linear PCM settings if specified
            recorderAudioSet.avLinearPCMBitDepthKeyIOS?.let { bitDepth ->
                settings[AVLinearPCMBitDepthKey] = bitDepth
            }
            
            recorderAudioSet.avLinearPCMIsBigEndianKeyIOS?.let { isBigEndian ->
                settings[AVLinearPCMIsBigEndianKey] = isBigEndian
            }
            
            recorderAudioSet.avLinearPCMIsFloatKeyIOS?.let { isFloat ->
                settings[AVLinearPCMIsFloatKey] = isFloat
            }
            
            recorderAudioSet.avLinearPCMIsNonInterleavedIOS?.let { isNonInterleaved ->
                settings[AVLinearPCMIsNonInterleavedKey] = isNonInterleaved
            }
            
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                error.value = null
                
                val recorder = AVAudioRecorder(outputURL, settings, error.ptr)
                
                if (error.value == null && recorder != null) {
                    if (recorder.prepareToRecord()) {
                        // Enable metering if requested
                        if (properties.meteringEnabled) {
                            recorder.meteringEnabled = true
                        }
                        
                        recorder.record()
                        audioRecorder = recorder
                        isRecording = true
                        recordingStartTime = Clock.System.now().toEpochMilliseconds()
                        lastRecordedFileURL = outputURL
                        
                        startRecordingProgressUpdates()
                        
                        // Start metering if enabled
                        if (properties.meteringEnabled && audioMeteringListener != null) {
                            startMeteringUpdates()
                        }
                        
                        Result.success(outputURL.path ?: "")
                    } else {
                        Result.failure(Exception("Failed to prepare audio recorder"))
                    }
                } else {
                    Result.failure(Exception("Failed to create audio recorder: ${error.value?.localizedDescription}"))
                }
            }
        } catch (e: Exception) {
            stopRecordingInternal()
            Result.failure(e)
        }
    }
    
    override suspend fun pauseRecording(): Result<Unit> {
        return try {
            audioRecorder?.pause()
            recordingJob?.cancel()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resumeRecording(): Result<Unit> {
        return try {
            audioRecorder?.record()
            startRecordingProgressUpdates()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopRecording(): Result<String> {
        return try {
            val filePath = lastRecordedFileURL?.path ?: ""
            stopRecordingInternal()
            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun startPlaying(filePath: String?): Result<Unit> {
        return try {
            if (isPlaying) {
                return Result.failure(Exception("Already playing"))
            }
            
            val fileURL = filePath?.let { NSURL.fileURLWithPath(it) } 
                ?: lastRecordedFileURL 
                ?: return Result.failure(Exception("No file to play"))
            
            // Set audio session for playback
            val audioSession = AVAudioSession.sharedInstance()
            memScoped {
                val sessionError = alloc<ObjCObjectVar<NSError?>>()
                sessionError.value = null
                
                audioSession.setCategory(AVAudioSessionCategoryPlayback, error = sessionError.ptr)
                if (sessionError.value != null) {
                    return Result.failure(Exception("Failed to set audio session category: ${sessionError.value?.localizedDescription}"))
                }
                
                audioSession.setActive(true, error = sessionError.ptr)
                if (sessionError.value != null) {
                    return Result.failure(Exception("Failed to activate audio session: ${sessionError.value?.localizedDescription}"))
                }
            }
            
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                error.value = null
                
                val player = AVAudioPlayer(fileURL, error.ptr)
                
                if (error.value == null && player != null) {
                    if (player.prepareToPlay()) {
                        player.play()
                        audioPlayer = player
                        isPlaying = true
                        
                        startPlaybackProgressUpdates()
                        
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to prepare audio player"))
                    }
                } else {
                    Result.failure(Exception("Failed to create audio player: ${error.value?.localizedDescription}"))
                }
            }
        } catch (e: Exception) {
            stopPlayingInternal()
            Result.failure(e)
        }
    }
    
    override suspend fun pausePlaying(): Result<Unit> {
        return try {
            audioPlayer?.pause()
            playbackJob?.cancel()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resumePlaying(): Result<Unit> {
        return try {
            audioPlayer?.play()
            startPlaybackProgressUpdates()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopPlaying(): Result<Unit> {
        return try {
            stopPlayingInternal()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun seekTo(position: Long): Result<Unit> {
        return try {
            val positionInSeconds = position.toDouble() / 1000.0
            audioPlayer?.currentTime = positionInSeconds
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        return try {
            audioPlayer?.volume = volume
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecordingInfo(): Result<RecordingInfo?> {
        return try {
            val fileURL = lastRecordedFileURL ?: return Result.success(null)
            
            // Get file attributes to calculate size and duration
            val fileManager = NSFileManager.defaultManager()
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                error.value = null
                
                val fileAttributes = fileManager.attributesOfItemAtPath(fileURL.path ?: "", error = error.ptr)
                if (error.value != null) {
                    return Result.failure(Exception("Failed to get file attributes: ${error.value?.localizedDescription}"))
                }
                
                val fileSize = fileAttributes?.get(NSFileSize) as? NSNumber
                val fileSizeBytes = fileSize?.longLongValue ?: 0L
                
                // Get audio duration using AVAudioPlayer
                val playerError = alloc<ObjCObjectVar<NSError?>>()
                playerError.value = null
                
                val tempPlayer = AVAudioPlayer(fileURL, playerError.ptr)
                val duration = if (playerError.value == null && tempPlayer != null) {
                    (tempPlayer.duration * 1000).toLong() // Convert to milliseconds
                } else {
                    0L
                }
                
                Result.success(
                    RecordingInfo(
                        filePath = fileURL.path ?: "",
                        duration = duration,
                        fileSize = fileSizeBytes,
                        formattedDuration = formatTime(duration),
                        formattedFileSize = formatFileSize(fileSizeBytes)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun addRecordingListener(listener: (RecordingProgress) -> Unit) {
        recordingListener = listener
    }
    
    override fun addPlaybackListener(listener: (PlaybackProgress) -> Unit) {
        playbackListener = listener
    }
    
    override fun addAudioMeteringListener(listener: (AudioMeteringInfo) -> Unit) {
        audioMeteringListener = listener
        // Start metering if recording
        if (isRecording && properties.meteringEnabled) {
            startMeteringUpdates()
        }
    }
    
    override fun removeAudioMeteringListener() {
        audioMeteringListener = null
        meteringJob?.cancel()
        meteringJob = null
    }
    
    override fun removeListeners() {
        recordingListener = null
        playbackListener = null
        audioMeteringListener = null
        recordingJob?.cancel()
        playbackJob?.cancel()
        meteringJob?.cancel()
        stopRecordingInternal()
        stopPlayingInternal()
    }
    
    override fun setPlayerProperties(properties: AudioRecorderPlayerProperties) {
        this.properties = properties
    }
    
    override fun setRecorderProperties(audioSet: RecorderAudioSet) {
        this.recorderAudioSet = audioSet
    }
    
    private fun startRecordingProgressUpdates() {
        recordingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isRecording) {
                delay(properties.updateIntervalMs) // Use configurable update interval
                val elapsed = Clock.System.now().toEpochMilliseconds() - recordingStartTime
                recordingListener?.invoke(
                    RecordingProgress(
                        currentPosition = elapsed,
                        formattedTime = formatTime(elapsed)
                    )
                )
            }
        }
    }
    
    private fun startPlaybackProgressUpdates() {
        playbackJob = CoroutineScope(Dispatchers.Default).launch {
            while (isPlaying) {
                delay(properties.updateIntervalMs) // Use configurable update interval
                try {
                    val player = audioPlayer ?: break
                    val currentPosition = (player.currentTime * 1000).toLong()
                    val duration = (player.duration * 1000).toLong()
                    
                    playbackListener?.invoke(
                        PlaybackProgress(
                            currentPosition = currentPosition,
                            duration = duration,
                            formattedCurrentTime = formatTime(currentPosition),
                            formattedDuration = formatTime(duration)
                        )
                    )
                    
                    // Check if playback has completed
                    if (!player.playing) {
                        // If we're at the end, keep the player in a paused state instead of stopping
                        if (currentPosition >= duration && duration > 0) {
                            println("🎵 iOS Playback completed - keeping in paused state at end")
                            isPlaying = false
                            // Don't call stopPlayingInternal() - keep the player instance
                            // This allows resuming from the end or seeking
                        } else {
                            // Player was paused, not completed
                            isPlaying = false
                        }
                        break
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }
    
    private fun stopRecordingInternal() {
        try {
            isRecording = false
            recordingJob?.cancel()
            meteringJob?.cancel()
            audioRecorder?.stop()
            audioRecorder = null
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
    
    private fun stopPlayingInternal() {
        try {
            isPlaying = false
            playbackJob?.cancel()
            audioPlayer?.stop()
            audioPlayer = null
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
    
    private fun getDefaultRecordingURL(): NSURL {
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, 
            NSUserDomainMask, 
            true
        ).firstOrNull() as? String ?: ""
        
        val fileName = "recording_${Clock.System.now().toEpochMilliseconds()}.m4a"
        val filePath = "$documentsPath/$fileName"
        
        return NSURL.fileURLWithPath(filePath)
    }
    
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (millis % 1000) / 10 // 0.01초 단위 (centiseconds)
        
        val minutesStr = if (minutes < 10) "0$minutes" else "$minutes"
        val secondsStr = if (seconds < 10) "0$seconds" else "$seconds"
        val centisecondsStr = if (centiseconds < 10) "0$centiseconds" else "$centiseconds"
        
        return "$minutesStr:$secondsStr:$centisecondsStr"
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> {
                val mbValue = bytes / (1024.0 * 1024.0)
                NSString.stringWithFormat("%.1f MB", mbValue) as String
            }
            bytes >= 1024 -> {
                val kbValue = bytes / 1024.0
                NSString.stringWithFormat("%.1f KB", kbValue) as String
            }
            else -> "$bytes B"
        }
    }
    
    private fun startMeteringUpdates() {
        meteringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isRecording && properties.meteringEnabled) {
                delay(properties.updateIntervalMs)
                try {
                    audioRecorder?.updateMeters()
                    val peakPower = audioRecorder?.peakPowerForChannel(0u) ?: -160.0f
                    val averagePower = audioRecorder?.averagePowerForChannel(0u) ?: -160.0f
                    
                    audioMeteringListener?.invoke(
                        AudioMeteringInfo(
                            currentMetering = averagePower.toFloat(),
                            peakPower = peakPower.toFloat(),
                            averagePower = averagePower.toFloat()
                        )
                    )
                } catch (e: Exception) {
                    // Recorder might be released or in invalid state
                    break
                }
            }
        }
    }
    
    override suspend fun startPlaying(source: AudioSource): Result<Unit> {
        return try {
            when (source) {
                is AudioSource.File -> startPlaying(source.path)
                is AudioSource.Url -> {
                    if (isPlaying) {
                        return Result.failure(Exception("Already playing"))
                    }
                    
                    val url = NSURL.URLWithString(source.url)
                        ?: return Result.failure(Exception("Invalid URL: ${source.url}"))
                    
                    memScoped {
                        val error = alloc<ObjCObjectVar<NSError?>>()
                        error.value = null
                        
                        // Create AVPlayer for streaming
                        val playerItem = if (source.headers != null) {
                            // Create AVURLAsset with headers
                            val options = mapOf<Any?, Any?>(
                                "AVURLAssetHTTPHeaderFieldsKey" to source.headers
                            )
                            val asset = AVURLAsset(url, options)
                            AVPlayerItem(asset)
                        } else {
                            AVPlayerItem(url)
                        }
                        
                        // For URL playback, we need to use AVPlayer instead of AVAudioPlayer
                        // This is a limitation - we'll need to refactor to support both
                        // For now, return an error
                        Result.failure(Exception("URL playback requires AVPlayer implementation. Currently only local files are supported."))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> {
        return try {
            if (speed < 0.5f || speed > 2.0f) {
                return Result.failure(IllegalArgumentException("Speed must be between 0.5 and 2.0"))
            }
            
            audioPlayer?.let { player ->
                player.rate = speed
                player.enableRate = true
                Result.success(Unit)
            } ?: Result.failure(IllegalStateException("Audio player not initialized"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Actual implementation for iOS
 */
actual fun createAudioRecorderPlayer(): AudioRecorderPlayer {
    return IOSAudioRecorderPlayer()
}
