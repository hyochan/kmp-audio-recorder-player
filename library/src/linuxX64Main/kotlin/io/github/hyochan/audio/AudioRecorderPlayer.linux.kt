package io.github.hyochan.audio

/**
 * Linux implementation of AudioRecorderPlayer
 * This is a placeholder implementation for Linux platforms
 */
class LinuxAudioRecorderPlayer : AudioRecorderPlayer {
    
    private var recordingListener: ((RecordingProgress) -> Unit)? = null
    private var playbackListener: ((PlaybackProgress) -> Unit)? = null
    
    override suspend fun startRecording(filePath: String?): Result<String> {
        return Result.failure(Exception("Audio recording not implemented for Linux platform yet"))
    }
    
    override suspend fun pauseRecording(): Result<Unit> {
        return Result.failure(Exception("Audio recording not implemented for Linux platform yet"))
    }
    
    override suspend fun resumeRecording(): Result<Unit> {
        return Result.failure(Exception("Audio recording not implemented for Linux platform yet"))
    }
    
    override suspend fun stopRecording(): Result<String> {
        return Result.failure(Exception("Audio recording not implemented for Linux platform yet"))
    }
    
    override suspend fun startPlaying(filePath: String?): Result<Unit> {
        return Result.failure(Exception("Audio playback not implemented for Linux platform yet"))
    }
    
    override suspend fun pausePlaying(): Result<Unit> {
        return Result.failure(Exception("Audio playback not implemented for Linux platform yet"))
    }
    
    override suspend fun resumePlaying(): Result<Unit> {
        return Result.failure(Exception("Audio playback not implemented for Linux platform yet"))
    }
    
    override suspend fun stopPlaying(): Result<Unit> {
        return Result.failure(Exception("Audio playback not implemented for Linux platform yet"))
    }
    
    override suspend fun seekTo(position: Long): Result<Unit> {
        return Result.failure(Exception("Audio seek not implemented for Linux platform yet"))
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        return Result.failure(Exception("Audio volume control not implemented for Linux platform yet"))
    }
    
    override suspend fun getRecordingInfo(): Result<RecordingInfo?> {
        return Result.success(null)
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
    }
    
    override fun setPlayerProperties(properties: AudioRecorderPlayerProperties) {
        // Linux implementation stub - no action needed
    }
    
    override fun setRecorderProperties(audioSet: RecorderAudioSet) {
        // Linux implementation stub - no action needed
    }
}

/**
 * Actual implementation for Linux
 */
actual fun createAudioRecorderPlayer(): AudioRecorderPlayer {
    return LinuxAudioRecorderPlayer()
}
