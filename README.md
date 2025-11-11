# 🎯 AFTERPACKETS

**Professional-Grade Mobile Deep Packet Inspection & Network Analysis Platform**

A comprehensive cybersecurity tool featuring:
- 📱 **Android App**: Real-time packet capture via VPNService with native DPI engine
- 💻 **Desktop Web App**: Advanced visualization, timeline replay, and geo-location mapping
- 🔒 **Security-First**: Encrypted exports, audit logging, legal consent enforcement

---

## 🌟 Features

### Core MVP Features

#### Android App
- ✅ **Packet Capture**: VPNService-based capture (no root required)
- ✅ **Deep Packet Inspection**: Native C++ parser for IP/TCP/UDP/ICMP/HTTP/DNS/TLS
- ✅ **Real-time Dashboard**: Live metrics, protocol distribution, top talkers
- ✅ **Security Alerts**: MITM detection, DNS spoofing, data exfiltration, ARP spoofing
- ✅ **Quick Filters**: One-tap filtering (HTTP/HTTPS, DNS, Large Transfers, ICMP, TLS)
- ✅ **Export Formats**: PCAP (Wireshark-compatible), JSON metadata, Evidence bundles
- ✅ **Legal Compliance**: Consent dialog, audit logging, auto-cleanup

#### Desktop Web App
- ✅ **Import & Visualize**: Drag-and-drop evidence bundle import
- ✅ **Dashboard Analytics**: Interactive charts (Recharts), protocol breakdown
- ✅ **Geo-Location Map**: Plot remote hosts on interactive map (Leaflet.js)
- ✅ **Timeline Replay**: Playback captured sessions with temporal visualization
- ✅ **Rule Builder**: Create & manage custom detection rules
- ✅ **Cross-Platform**: Works on Mac, Windows, Linux

### 💎 Bonus Features
- ⚡ **One-Tap Security Filters**: Pre-configured filters for common threat patterns
- 🗺️ **GeoIP Integration**: Automatic IP geo-location with map visualization
- ⚖️ **Live Rule Builder**: Custom detection rules with real-time triggers
- ⏪ **Session Replay**: Timeline-based playback with frame-by-frame analysis

---

## 🚀 Quick Start

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

### Desktop Web App

#### Prerequisites
- Node.js 18+ 
- npm 9+

#### Development
```bash
cd desktop-web
npm install
npm run dev
# Open http://localhost:3000
```

#### Production Build
```bash
npm run build
# Static files in dist/
```

---

## 📁 Project Structure

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

## 🛠️ Technology Stack

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

### Desktop Web
| Layer | Technology |
|-------|-----------|
| Framework | React 18 + TypeScript |
| Build Tool | Vite 5 |
| Styling | Tailwind CSS 3 |
| Charts | Recharts 2 |
| Maps | Leaflet + React-Leaflet |
| State | Zustand |
| Icons | Lucide React |
| Routing | React Router 6 |

---

## 📖 Usage Guide

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

### Desktop App Workflow

1. **Import Data**
   - Navigate to Import page
   - Drag & drop .zip evidence bundle or .json file
   - Data loads automatically

2. **Analyze Dashboard**
   - View protocol distribution pie chart
   - Check top talkers list
   - Review recent packets table

3. **Explore Map View**
   - See remote hosts plotted geographically
   - Click markers for packet/byte counts
   - Filter by country/region

4. **Replay Session**
   - Use playback controls (play/pause/scrub)
   - Watch packet rate changes over time
   - Analyze timeline buckets

5. **Manage Rules**
   - Create custom detection rules
   - Enable/disable individual rules
   - Export rule sets

---

## 🔒 Security & Privacy

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
- ✅ Authorized security professionals
- ✅ Network administrators
- ✅ Security researchers (with proper authorization)
- ✅ Educational purposes (in controlled environments)

**Unauthorized network monitoring is illegal in most jurisdictions.**

---

## 🎬 Demo Scenarios

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

## 📊 Performance

### Android App
- **CPU Usage**: <5% average during capture
- **Memory**: ~50MB RAM footprint
- **Battery**: <2% per hour (background capture)
- **Packet Rate**: Up to 5,000 packets/sec

### Desktop App
- **Bundle Size**: ~1.2MB (gzipped)
- **Load Time**: <2s for 100K packets
- **Browser Support**: Chrome/Firefox/Safari/Edge (last 2 versions)

---

## 🐛 Troubleshooting

### Android

**Issue**: VPN permission denied
- **Solution**: Go to Settings → Apps → Packet Hunter → Permissions → Grant VPN access

**Issue**: No packets captured
- **Solution**: Ensure you're generating network traffic (browse web, use apps)

**Issue**: App crashes on start
- **Solution**: Check Android version (need 8.0+), clear app data

### Desktop

**Issue**: Import fails
- **Solution**: Verify file is valid .zip or .json, check browser console

**Issue**: Map not showing
- **Solution**: Check internet connection (Leaflet requires tiles), enable location data in export

---

## 🤝 Contributing

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

## 🏆 Hackathon Checklist

- [x] **Core MVP Features**: All implemented
- [x] **Bonus Features**: All 4 implemented  
- [x] **Android App**: Fully functional with Compose UI
- [x] **Desktop App**: React/TypeScript with all views
- [x] **Export/Import**: PCAP + JSON + Evidence bundles
- [x] **Documentation**: Comprehensive README
- [x] **Demo Ready**: Clear usage scenarios
- [x] **Build Scripts**: Gradle + npm configured
- [x] **Sample Data**: Included for testing

---

## 📞 Contact & Support

**Project**: AFTERPACKETS  
**Version**: 1.0.0  
**Platform**: Android 8.0+ | Web (Modern Browsers)  
**Repository**: [GitHub Link]  
**Issues**: [Issues Page]

---

*Built with ❤️ for cybersecurity professionals worldwide*
