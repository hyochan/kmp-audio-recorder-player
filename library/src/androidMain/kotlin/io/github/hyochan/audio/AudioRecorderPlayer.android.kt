package io.github.hyochan.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

/**
 * Android implementation of AudioRecorderPlayer using MediaRecorder and MediaPlayer
 */
class AndroidAudioRecorderPlayer(
    private val context: Context
) : AudioRecorderPlayer {
    
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    
    private var recordingListener: ((RecordingProgress) -> Unit)? = null
    private var playbackListener: ((PlaybackProgress) -> Unit)? = null
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    
    private var isRecording = false
    private var isPlaying = false
    private var isPaused = false
    private var recordingStartTime = 0L
    private var recordingPauseTime = 0L
    private var lastRecordedFile: String? = null
    
    // Configuration properties
    private var properties = AudioRecorderPlayerProperties()
    private var recorderAudioSet = RecorderAudioSet()
    
    override suspend fun startRecording(filePath: String?): Result<String> {
        return try {
            if (isRecording) {
                return Result.failure(Exception("Already recording"))
            }
            
            val outputFile = filePath ?: getDefaultRecordingPath()
            
            // Ensure directory exists
            val file = File(outputFile)
            file.parentFile?.mkdirs()
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                try {
                    // Apply audio source setting
                    val audioSource = when (recorderAudioSet.audioSourceAndroid) {
                        AudioSourceAndroidType.MIC -> MediaRecorder.AudioSource.MIC
                        AudioSourceAndroidType.CAMCORDER -> MediaRecorder.AudioSource.CAMCORDER
                        AudioSourceAndroidType.VOICE_RECOGNITION -> MediaRecorder.AudioSource.VOICE_RECOGNITION
                        AudioSourceAndroidType.VOICE_COMMUNICATION -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
                        else -> MediaRecorder.AudioSource.MIC
                    }
                    setAudioSource(audioSource)
                    
                    // Apply output format setting
                    val outputFormat = when (recorderAudioSet.outputFormatAndroid) {
                        OutputFormatAndroidType.THREE_GPP -> MediaRecorder.OutputFormat.THREE_GPP
                        OutputFormatAndroidType.MPEG_4 -> MediaRecorder.OutputFormat.MPEG_4
                        OutputFormatAndroidType.AMR_NB -> MediaRecorder.OutputFormat.AMR_NB
                        OutputFormatAndroidType.AMR_WB -> MediaRecorder.OutputFormat.AMR_WB
                        OutputFormatAndroidType.AAC_ADTS -> MediaRecorder.OutputFormat.AAC_ADTS
                        OutputFormatAndroidType.WEBM -> MediaRecorder.OutputFormat.WEBM
                        else -> MediaRecorder.OutputFormat.MPEG_4
                    }
                    setOutputFormat(outputFormat)
                    
                    // Apply audio encoder setting
                    val audioEncoder = when (recorderAudioSet.audioEncoderAndroid) {
                        AudioEncoderAndroidType.AMR_NB -> MediaRecorder.AudioEncoder.AMR_NB
                        AudioEncoderAndroidType.AMR_WB -> MediaRecorder.AudioEncoder.AMR_WB
                        AudioEncoderAndroidType.AAC -> MediaRecorder.AudioEncoder.AAC
                        AudioEncoderAndroidType.HE_AAC -> MediaRecorder.AudioEncoder.HE_AAC
                        AudioEncoderAndroidType.AAC_ELD -> MediaRecorder.AudioEncoder.AAC_ELD
                        AudioEncoderAndroidType.VORBIS -> MediaRecorder.AudioEncoder.VORBIS
                        else -> MediaRecorder.AudioEncoder.AAC
                    }
                    setAudioEncoder(audioEncoder)
                    
                    // Apply additional settings if specified
                    recorderAudioSet.audioEncodingBitRateAndroid?.let { bitRate ->
                        setAudioEncodingBitRate(bitRate)
                    }
                    
                    recorderAudioSet.audioSamplingRateAndroid?.let { sampleRate ->
                        setAudioSamplingRate(sampleRate)
                    }
                    
                    recorderAudioSet.audioChannelsAndroid?.let { channels ->
                        setAudioChannels(channels)
                    }
                    
                    setOutputFile(outputFile)
                    
                    prepare()
                    start()
                } catch (e: SecurityException) {
                    throw Exception("Permission denied. Please grant RECORD_AUDIO permission.", e)
                } catch (e: IOException) {
                    throw Exception("Failed to start recording. Check if microphone is available.", e)
                } catch (e: IllegalStateException) {
                    throw Exception("MediaRecorder is in invalid state.", e)
                }
            }
            
            isRecording = true
            isPaused = false
            recordingStartTime = System.currentTimeMillis()
            lastRecordedFile = outputFile
            
            startRecordingProgressUpdates()
            
            Result.success(outputFile)
        } catch (e: Exception) {
            stopRecordingInternal()
            Result.failure(e)
        }
    }
    
    override suspend fun pauseRecording(): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                isPaused = true
                recordingPauseTime = System.currentTimeMillis()
                recordingJob?.cancel()
            } else {
                return Result.failure(Exception("Pause recording not supported on this Android version"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resumeRecording(): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                isPaused = false
                // Adjust start time to account for pause duration
                recordingStartTime += (System.currentTimeMillis() - recordingPauseTime)
                startRecordingProgressUpdates()
            } else {
                return Result.failure(Exception("Resume recording not supported on this Android version"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopRecording(): Result<String> {
        return try {
            val filePath = lastRecordedFile ?: ""
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
            
            val fileToPlay = filePath ?: lastRecordedFile 
                ?: return Result.failure(Exception("No file to play"))
            
            if (!File(fileToPlay).exists()) {
                return Result.failure(Exception("File does not exist: $fileToPlay"))
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(fileToPlay)
                prepare()
                
                setOnCompletionListener { _ ->
                    println("🎵 Playback completed - auto stopping")
                    this@AndroidAudioRecorderPlayer.isPlaying = false
                    playbackJob?.cancel()
                    
                    // Notify listener about completion
                    playbackListener?.invoke(
                        PlaybackProgress(
                            currentPosition = duration.toLong(),
                            duration = duration.toLong(),
                            formattedCurrentTime = formatTime(duration.toLong()),
                            formattedDuration = formatTime(duration.toLong())
                        )
                    )
                }
                
                start()
            }
            
            isPlaying = true
            startPlaybackProgressUpdates()
            
            Result.success(Unit)
        } catch (e: Exception) {
            stopPlayingInternal()
            Result.failure(e)
        }
    }
    
    override suspend fun pausePlaying(): Result<Unit> {
        return try {
            mediaPlayer?.pause()
            playbackJob?.cancel()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resumePlaying(): Result<Unit> {
        return try {
            mediaPlayer?.start()
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
            mediaPlayer?.seekTo(position.toInt())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        return try {
            mediaPlayer?.setVolume(volume, volume)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecordingInfo(): Result<RecordingInfo?> {
        return try {
            val filePath = lastRecordedFile
            if (filePath != null && File(filePath).exists()) {
                val file = File(filePath)
                val fileSize = file.length()
                
                // Get duration using MediaPlayer
                val tempPlayer = MediaPlayer()
                var duration = 0L
                try {
                    tempPlayer.setDataSource(filePath)
                    tempPlayer.prepare()
                    duration = tempPlayer.duration.toLong()
                } catch (e: Exception) {
                    // If we can't get duration, estimate based on file size
                    duration = 0L
                } finally {
                    tempPlayer.release()
                }
                
                Result.success(
                    RecordingInfo(
                        filePath = filePath,
                        duration = duration,
                        fileSize = fileSize,
                        formattedDuration = formatTime(duration),
                        formattedFileSize = formatFileSize(fileSize)
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    override fun addRecordingListener(listener: (RecordingProgress) -> Unit) {
        recordingListener = listener
    }
    
    override fun addPlaybackListener(listener: (PlaybackProgress) -> Unit) {
        playbackListener = listener
    }
    
    override fun removeListeners() {
        recordingListener = null
        playbackListener = null
        recordingJob?.cancel()
        playbackJob?.cancel()
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
            while (isRecording && !isPaused) {
                delay(properties.updateIntervalMs) // Use configurable update interval
                val elapsed = System.currentTimeMillis() - recordingStartTime
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
                    val currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val duration = mediaPlayer?.duration?.toLong() ?: 0L
                    
                    playbackListener?.invoke(
                        PlaybackProgress(
                            currentPosition = currentPosition,
                            duration = duration,
                            formattedCurrentTime = formatTime(currentPosition),
                            formattedDuration = formatTime(duration)
                        )
                    )
                    
                    if (currentPosition >= duration && duration > 0) {
                        isPlaying = false
                        break
                    }
                } catch (e: Exception) {
                    // Player might be released
                    break
                }
            }
        }
    }
    
    private fun stopRecordingInternal() {
        try {
            isRecording = false
            isPaused = false
            recordingJob?.cancel()
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
    
    private fun stopPlayingInternal() {
        try {
            isPlaying = false
            playbackJob?.cancel()
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
    
    private fun getDefaultRecordingPath(): String {
        // Use app-specific external storage directory (doesn't require WRITE_EXTERNAL_STORAGE permission)
        val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        val fileName = "recording_${System.currentTimeMillis()}.m4a"
        return File(recordingsDir, fileName).absolutePath
    }
    
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (millis % 1000) / 10 // 0.01초 단위 (centiseconds)
        return String.format("%02d:%02d:%02d", minutes, seconds, centiseconds)
    }
}

/**
 * Platform-specific factory function for Android
 */
private var applicationContext: Context? = null

/**
 * Initialize the library with Android context
 * Must be called before using createAudioRecorderPlayer()
 */
fun initializeAudioRecorderPlayer(context: Context) {
    applicationContext = context.applicationContext
}

/**
 * Actual implementation for Android
 */
actual fun createAudioRecorderPlayer(): AudioRecorderPlayer {
    val context = applicationContext 
        ?: throw IllegalStateException("AudioRecorderPlayer not initialized. Call initializeAudioRecorderPlayer(context) first.")
    return AndroidAudioRecorderPlayer(context)
}
