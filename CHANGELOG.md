# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-alpha04] - 2025-01-08

### Fixed
- Android recording meter not updating during recording
  - Improved amplitude reading mechanism
  - Added proper normalization for UI display
  - Fixed threading issues for UI updates
- iOS playback state now correctly remains paused at completion instead of stopping
  - Allows resuming or seeking from end position
  - Maintains player instance for better state management

### Changed
- Improved cross-platform consistency for audio metering behavior
- Enhanced debug logging for metering issues
- Updated ViewModel to handle both normalized values (Android) and dB values (iOS)

### Added
- Visual feedback for recording meter even when amplitude is 0
- Better error handling for MediaRecorder state

## [1.0.0-alpha03] - 2025-01-07

### Added
- Initial KMP library implementation
- Support for Android, iOS, Desktop (JVM), and Web (WASM) platforms
- Audio recording functionality
- Audio playback functionality
- Real-time audio metering
- Playback speed control
- Maven Central publishing setup

### Features
- Cross-platform audio recording and playback
- Configurable audio properties
- Progress callbacks for recording and playback
- Audio metering for visualization
- Pause/resume support
- Seek functionality
- Volume control