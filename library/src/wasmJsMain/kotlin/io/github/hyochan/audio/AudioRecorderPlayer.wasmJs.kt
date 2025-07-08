package io.github.hyochan.audio

import kotlinx.coroutines.*

/**
 * WasmJS implementation of AudioRecorderPlayer (stub implementation)
 */
class WasmJSAudioRecorderPlayer : AudioRecorderPlayer {
    
    private var recordingListener: ((RecordingProgress) -> Unit)? = null
    private var playbackListener: ((PlaybackProgress) -> Unit)? = null
    private var audioMeteringListener: ((AudioMeteringInfo) -> Unit)? = null
    
    override suspend fun startRecording(filePath: String?): Result<String> {
        return Result.failure(Exception("Audio recording is not supported on WasmJS"))
    }
    
    override suspend fun pauseRecording(): Result<Unit> {
        return Result.failure(Exception("Audio recording is not supported on WasmJS"))
    }
    
    override suspend fun resumeRecording(): Result<Unit> {
        return Result.failure(Exception("Audio recording is not supported on WasmJS"))
    }
    
    override suspend fun stopRecording(): Result<String> {
        return Result.failure(Exception("Audio recording is not supported on WasmJS"))
    }
    
    override suspend fun startPlaying(filePath: String?): Result<Unit> {
        return Result.failure(Exception("Audio playback is not supported on WasmJS"))
    }
    
    override suspend fun pausePlaying(): Result<Unit> {
        return Result.failure(Exception("Audio playback is not supported on WasmJS"))
    }
    
    override suspend fun resumePlaying(): Result<Unit> {
        return Result.failure(Exception("Audio playback is not supported on WasmJS"))
    }
    
    override suspend fun stopPlaying(): Result<Unit> {
        return Result.failure(Exception("Audio playback is not supported on WasmJS"))
    }
    
    override suspend fun seekTo(position: Long): Result<Unit> {
        return Result.failure(Exception("Audio seek is not supported on WasmJS"))
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        return Result.failure(Exception("Audio volume control is not supported on WasmJS"))
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
    
    override fun addAudioMeteringListener(listener: (AudioMeteringInfo) -> Unit) {
        audioMeteringListener = listener
    }
    
    override fun removeAudioMeteringListener() {
        audioMeteringListener = null
    }
    
    override fun removeListeners() {
        recordingListener = null
        playbackListener = null
        audioMeteringListener = null
    }
    
    override fun setPlayerProperties(properties: AudioRecorderPlayerProperties) {
        // WasmJS implementation stub - no action needed
    }
    
    override fun setRecorderProperties(audioSet: RecorderAudioSet) {
        // WasmJS implementation stub - no action needed
    }
    
    override suspend fun startPlaying(source: AudioSource): Result<Unit> {
        return Result.failure(Exception("Audio playback is not supported on WasmJS"))
    }
    
    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> {
        return Result.failure(Exception("Playback speed control not supported on WASM/JS platform"))
    }
}

/**
 * Actual implementation for WasmJS
 */
actual fun createAudioRecorderPlayer(): AudioRecorderPlayer {
    return WasmJSAudioRecorderPlayer()
}
