# Android App - AFTERPACKETS

## Build Instructions

### Requirements
- Android Studio Hedgehog+ 
- JDK 17
- Android SDK 26-34
- NDK 25.2+

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing)
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

### Project Structure
- `app/src/main/java` - Kotlin source code
- `app/src/main/cpp` - Native C++ packet parser
- `app/src/main/res` - Android resources
- `app/build.gradle.kts` - Build configuration

### Key Components
- **PacketCaptureService**: VPNService implementation
- **NativePacketParser**: JNI wrapper for C++ parser
- **PacketProcessor**: Packet analysis & storage
- **MainViewModel**: UI state management
- **ExportManager**: PCAP/JSON export

##Permissions
App requires:
- `BIND_VPN_SERVICE` - For packet capture
- `INTERNET` - For network access
- `FOREGROUND_SERVICE` - For background capture
- `POST_NOTIFICATIONS` - For alerts (Android 13+)
