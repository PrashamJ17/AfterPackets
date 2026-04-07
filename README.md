# AFTERPACKETS

**Professional-Grade Mobile Deep Packet Inspection & Network Analysis Platform**

A comprehensive cybersecurity tool featuring:
-  **Android App**: Real-time packet capture via VPNService with native DPI engine
-  **Desktop Web App**: Advanced visualization, timeline replay, and geo-location mapping
-  **Security-First**: Encrypted exports, audit logging, legal consent enforcement

---

##  Features

### Core MVP Features

#### Android App
-  **Packet Capture**: VPNService-based capture (no root required)
-  **Deep Packet Inspection**: Native C++ parser for IP/TCP/UDP/ICMP/HTTP/DNS/TLS
-  **Real-time Dashboard**: Live metrics, protocol distribution, top talkers
-  **Security Alerts**: MITM detection, DNS spoofing, data exfiltration, ARP spoofing
-  **Quick Filters**: One-tap filtering (HTTP/HTTPS, DNS, Large Transfers, ICMP, TLS)
-  **Export Formats**: PCAP (Wireshark-compatible), JSON metadata, Evidence bundles
-  **Legal Compliance**: Consent dialog, audit logging, auto-cleanup

###  Bonus Features
-  **One-Tap Security Filters**: Pre-configured filters for common threat patterns
-  **Live Rule Builder**: Custom detection rules with real-time triggers

---

##  Quick Start

### Android App

#### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or later
- Android SDK 26-34
- NDK 25.2.9519653+
- Kotlin 1.9.20+

#### Build & Run
```bash
cd android-app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Release Build
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

---

##  Project Structure

```
mobile-packet-hunter/
├── android-app/              # Android application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/packethunter/mobile/
│   │   │   │   ├── capture/          # VPNService & packet capture
│   │   │   │   ├── data/             # Database & models
│   │   │   │   ├── export/           # PCAP/JSON export
│   │   │   │   └── ui/               # Jetpack Compose UI
│   │   │   ├── cpp/                  # Native C++ packet parser
│   │   │   └── res/                  # Android resources
│   │   └── build.gradle.kts
│   └── settings.gradle.kts
│
├── desktop-web/              # React/TypeScript web app
│   ├── src/
│   │   ├── pages/            # Dashboard, Import, Map, Replay, Rules
│   │   ├── store/            # Zustand state management
│   │   ├── types/            # TypeScript interfaces
│   │   └── App.tsx
│   ├── package.json
│   └── vite.config.ts
│
├── docs/                     # Documentation & presentations
├── sample-data/              # Demo .pcap and .json files
└── README.md
```

---

##  Technology Stack

### Android
| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9.20 |
| UI Framework | Jetpack Compose + Material 3 |
| Native Code | C++17 (NDK) |
| Database | Room + SQLite |
| Networking | VPNService API |
| Packet Parser | Custom C++ engine |
| Charts | Vico Compose |
| Security | Android Keystore (AES-GCM) |

---

##  Usage Guide

### Android App Workflow

1. **Accept Legal Consent**
   - First launch shows legal notice
   - Confirm you have permission to monitor traffic

2. **Start Capture**
   - Tap the Play button (top-right)
   - Grant VPN permission when prompted
   - Capture starts immediately

3. **Apply Filters**
   - Use quick filter chips to focus on specific traffic
   - View real-time dashboard metrics

4. **Review Alerts**
   - Check notification badge for security alerts
   - Tap to view details and evidence

5. **Export Data**
   - Navigate to Export screen
   - Choose PCAP, JSON, or Evidence Bundle
   - Share with desktop app

---

##  Security & Privacy

### Legal Compliance
- **Consent Enforcement**: Mandatory legal notice on first launch
- **Audit Logging**: All capture sessions logged with timestamps
- **Auto-Cleanup**: Configurable data retention (default: 24 hours)

### Data Protection
- **Encrypted Exports**: AES-256-GCM using Android Keystore
- **Ephemeral Mode**: Optional no-disk-write mode
- **Local Processing**: All analysis happens on-device

### Ethical Use
This tool is intended for:
-  Authorized security professionals
-  Network administrators
-  Security researchers (with proper authorization)
-  Educational purposes (in controlled environments)

**Unauthorized network monitoring is illegal in most jurisdictions.**

---

##  Demo Scenarios

### Scenario 1: HTTP Traffic Analysis
```
1. Start capture
2. Apply "HTTP/HTTPS" filter
3. Visit http://example.com
4. Review GET requests in packet list
5. Export as PCAP for Wireshark analysis
```

### Scenario 2: DNS Query Inspection
```
1. Start capture
2. Apply "DNS" filter  
3. Perform web browsing
4. Check DNS queries to various domains
5. Detect any suspicious DNS patterns
```

### Scenario 3: Data Exfiltration Detection
```
1. Start capture
2. Simulate large file upload (>1MB in 10s)
3. Alert triggers automatically
4. Export evidence bundle
5. Import to desktop for timeline replay
```

---

##  Performance

### Android App
- **CPU Usage**: <5% average during capture
- **Memory**: ~50MB RAM footprint
- **Battery**: <2% per hour (background capture)
- **Packet Rate**: Up to 5,000 packets/sec

---

##  Troubleshooting

### Android

**Issue**: VPN permission denied
- **Solution**: Go to Settings → Apps → Packet Hunter → Permissions → Grant VPN access

**Issue**: No packets captured
- **Solution**: Ensure you're generating network traffic (browse web, use apps)

**Issue**: App crashes on start
- **Solution**: Check Android version (need 8.0+), clear app data

---

##  Contributing

We welcome contributions! Please:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see LICENSE file for details.

**Disclaimer**: This tool is for authorized security analysis only. Users are solely responsible for ensuring compliance with applicable laws and regulations.

---

## 📞 Contact & Support

**Project**: AFTERPACKETS  
**Version**: 1.0.0  
**Platform**: Android 8.0+ | Web (Modern Browsers)  
**Repository**: https://github.com/PrashamJ17/AfterPackets  
**Issues**: ----

---
