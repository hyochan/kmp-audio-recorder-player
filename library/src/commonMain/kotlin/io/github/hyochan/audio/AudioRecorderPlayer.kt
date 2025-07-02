package io.github.hyochan.audio

/**
 * Progress information during audio recording
 */
data class RecordingProgress(
    val currentPosition: Long,
    val formattedTime: String
)

/**
 * Progress information during audio playback
 */
data class PlaybackProgress(
    val currentPosition: Long,
    val duration: Long,
    val formattedCurrentTime: String,
    val formattedDuration: String
)

/**
 * Information about a recorded audio file
 */
data class RecordingInfo(
    val filePath: String,
    val duration: Long,
    val fileSize: Long,
    val formattedDuration: String,
    val formattedFileSize: String
)

/**
 * Audio encoding types for iOS
 */
enum class AVEncodingType {
    LPCM,
    IMA4,
    AAC,
    MAC3,
    MAC6,
    ULAW,
    ALAW,
    MP1,
    MP2,
    MP4,
    ALAC,
    AMR,
    FLAC,
    OPUS,
    WAV
}

/**
 * Audio source types for Android
 */
enum class AudioSourceAndroidType {
    DEFAULT,
    MIC,
    VOICE_UPLINK,
    VOICE_DOWNLINK,
    VOICE_CALL,
    CAMCORDER,
    VOICE_RECOGNITION,
    VOICE_COMMUNICATION,
    REMOTE_SUBMIX,
    UNPROCESSED,
    RADIO_TUNER,
    HOTWORD
}

/**
 * Output format types for Android
 */
enum class OutputFormatAndroidType {
    DEFAULT,
    THREE_GPP,
    MPEG_4,
    AMR_NB,
    AMR_WB,
    AAC_ADIF,
    AAC_ADTS,
    OUTPUT_FORMAT_RTP_AVP,
    MPEG_2_TS,
    WEBM,
    UNUSED,
    OGG
}

/**
 * Audio encoder types for Android
 */
enum class AudioEncoderAndroidType {
    DEFAULT,
    AMR_NB,
    AMR_WB,
    AAC,
    HE_AAC,
    AAC_ELD,
    VORBIS,
    OPUS
}

/**
 * Audio quality types for iOS
 */
enum class AVEncoderAudioQualityIOSType {
    MIN,
    LOW,
    MEDIUM,
    HIGH,
    MAX
}

/**
 * Audio recorder configuration settings
 */
data class RecorderAudioSet(
    // iOS settings
    val avSampleRateKeyIOS: Int? = null,
    val avFormatIDKeyIOS: AVEncodingType? = null,
    val avNumberOfChannelsKeyIOS: Int? = null,
    val avEncoderAudioQualityKeyIOS: AVEncoderAudioQualityIOSType? = null,
    val avLinearPCMBitDepthKeyIOS: Int? = null,
    val avLinearPCMIsBigEndianKeyIOS: Boolean? = null,
    val avLinearPCMIsFloatKeyIOS: Boolean? = null,
    val avLinearPCMIsNonInterleavedIOS: Boolean? = null,
    val avEncoderBitRateKeyIOS: Int? = null,
    
    // Android settings
    val audioSourceAndroid: AudioSourceAndroidType? = null,
    val outputFormatAndroid: OutputFormatAndroidType? = null,
    val audioEncoderAndroid: AudioEncoderAndroidType? = null,
    val audioEncodingBitRateAndroid: Int? = null,
    val audioSamplingRateAndroid: Int? = null,
    val audioChannelsAndroid: Int? = null
)

/**
 * Configuration properties for AudioRecorderPlayer
 */
data class AudioRecorderPlayerProperties(
    val updateIntervalMs: Long = 25L // Update interval in milliseconds (default: 0.025 seconds = 25ms)
)

/**
 * Audio recording and playback interface for multiplatform support
 */
interface AudioRecorderPlayer {
    
    /**
     * Start recording audio
     * @param filePath Optional file path to save the recording. If null, uses default path.
     * @return Result with the file path where recording is saved
     */
    suspend fun startRecording(filePath: String? = null): Result<String>
    
    /**
     * Pause the current recording
     */
    suspend fun pauseRecording(): Result<Unit>
    
    /**
     * Resume the paused recording
     */
    suspend fun resumeRecording(): Result<Unit>
    
    /**
     * Stop the current recording
     * @return Result with the file path where recording was saved
     */
    suspend fun stopRecording(): Result<String>
    
    /**
     * Start playing audio from file
     * @param filePath Optional file path to play. If null, plays the last recorded file.
     */
    suspend fun startPlaying(filePath: String? = null): Result<Unit>
    
    /**
     * Pause the current playback
     */
    suspend fun pausePlaying(): Result<Unit>
    
    /**
     * Resume the paused playback
     */
    suspend fun resumePlaying(): Result<Unit>
    
    /**
     * Stop the current playback
     */
    suspend fun stopPlaying(): Result<Unit>
    
    /**
     * Seek to a specific position during playback
     * @param position Position in milliseconds
     */
    suspend fun seekTo(position: Long): Result<Unit>
    
    /**
     * Set the volume for playback
     * @param volume Volume level (0.0 to 1.0)
     */
    suspend fun setVolume(volume: Float): Result<Unit>
    
    /**
     * Add listener for recording progress updates
     */
    fun addRecordingListener(listener: (RecordingProgress) -> Unit)
    
    /**
     * Add listener for playback progress updates
     */
    fun addPlaybackListener(listener: (PlaybackProgress) -> Unit)
    
    /**
     * Remove all listeners and cleanup resources
     */
    fun removeListeners()
    
    /**
     * Get information about the last recorded file
     * @return Result with RecordingInfo if a file exists, null otherwise
     */
    suspend fun getRecordingInfo(): Result<RecordingInfo?>
    
    /**
     * Set configuration properties for the audio player (update intervals, etc.)
     * @param properties Configuration properties including update intervals
     */
    fun setPlayerProperties(properties: AudioRecorderPlayerProperties)
    
    /**
     * Set audio configuration for the recorder (quality, format, etc.)
     * @param audioSet Audio configuration settings for recording
     */
    fun setRecorderProperties(audioSet: RecorderAudioSet)
}

/**
 * Platform-specific factory function to create AudioRecorderPlayer instance
 */
expect fun createAudioRecorderPlayer(): AudioRecorderPlayer
