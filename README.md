# KMP Audio Recorder Player

<div align="center">
  <img src="https://github.com/user-attachments/assets/df643740-6c9e-418e-a3a9-0da974ba7c5f" alt="KMP Audio Recorder Player" width="128" height="128" />
  
  [![Maven Central](https://img.shields.io/maven-central/v/io.github.hyochan/kmp-audio-recorder-player.svg)](https://central.sonatype.com/artifact/io.github.hyochan/kmp-audio-recorder-player)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  
  *A Kotlin Multiplatform library for audio recording and playback that works seamlessly across Android, iOS, Desktop, and Web platforms.*
</div>

## ✨ Features

- 🎤 **Audio Recording**: Record audio with high quality across all platforms
- 🔊 **Audio Playback**: Play recorded audio files with full control
- 🌐 **Multiplatform**: Supports Android, iOS, Desktop (JVM), and Web (WASM)
- 📱 **Modern API**: Kotlin-first design with coroutines support
- 🛡️ **Type Safety**: Fully typed API with comprehensive error handling

## 🚀 Installation

Add the library to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.hyochan:kmp-audio-recorder-player:1.0.0-alpha04")
}
```

## 📱 Platform Support

| Platform | Status | Notes   |
| -------- | ------ | ------- |
| Android  | ✅     | API 24+ |
| iOS      | ✅     | iOS 13+ |
| Desktop  | ✅     | JVM     |
| Web      | ✅     | WASM    |

## 🏗️ Development Setup

This project uses VS Code with predefined tasks for development:

### Building and Testing

- **Build Library for All Platforms**: `Cmd+Shift+P` → `Tasks: Run Task` → `Build Library for All Platforms`
- **Clean and Build Library**: Clean and rebuild the entire library

### Running Examples

- **Run Android Debug**: Build and run Android example app
- **Run iOS Simulator**: Build and run iOS example app
- **Run Desktop Debug**: Run desktop example application

### Publishing Tasks

- **Check Publishing Tasks**: View all available publishing-related Gradle tasks

## Configuration

No additional configuration required! The library works out of the box on all supported platforms.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 🐛 Issues

Found a bug or have a feature request? Please create an issue on [GitHub](https://github.com/hyochan/kmp-audio-recorder-player/issues).

---

## Development Guide

### Prerequisites

- Kotlin Multiplatform development environment
- Android Studio or VS Code with Kotlin support
- For iOS development: Xcode on macOS

### Local Development

1. **Clone the repository**
2. **Open in VS Code** (recommended for predefined tasks)
3. **Build**: Use VS Code tasks or run `./gradlew :library:build`
4. **Test**: Run `./gradlew :library:test`

For detailed setup instructions, see the [Kotlin Multiplatform Library Template](https://github.com/Kotlin/multiplatform-library-template) documentation.
