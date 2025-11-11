# 🎯 Packet Hunter - Feature Guide

## 📱 Navigation

The app now uses a **hamburger menu** (three lines icon) in the top-left corner. Tap it to open the navigation drawer and access all features.

## 🔍 Features Overview

### 1. 🎯 Dashboard
**Your main control center**
- **Real-time metrics**: See packets/second, bandwidth usage
- **Total statistics**: Total packets captured and data transferred
- **Quick filters**: Filter packets by type (HTTP/HTTPS, DNS, ICMP, etc.)
- **Protocol distribution**: Visual breakdown of protocol usage
- **Top talkers**: See the IP addresses with most traffic

### 2. 📦 Packet List
**Detailed packet inspection**
- View all captured packets in real-time
- Click any packet to see full details:
  - Source/destination IPs and ports
  - Protocol information
  - Payload preview
  - TLS/SSL details
  - HTTP requests/responses
- Packets are color-coded by protocol

### 3. 📱 App Usage
**NEW FEATURE: App-level tracking**
- See which apps are using your network
- Shows for each app:
  - Sent packets and bytes
  - Received packets and bytes
  - Remote hosts contacted
  - Protocols used
- Sorted by total data usage

### 4. 🗺️ Geo Map
**Geographic visualization**
- **Now shows YOUR actual device location** (requires location permission)
- Plots remote servers you're connecting to on a map
- Lines show connections from your device to remote hosts
- See country and location details for each connection
- Useful for detecting suspicious connections to unexpected locations

### 5. ⚠️ Alerts
**Security notifications**
- Real-time security alerts
- Different severity levels: Low, Medium, High, Critical
- Alert types include:
  - Man-in-the-Middle (MITM) attacks
  - DNS spoofing attempts
  - Data exfiltration
  - ARP spoofing
  - Suspicious patterns
- Acknowledge alerts to clear them

### 6. 🔐 Detection Rules
**Custom network monitoring rules**

#### What are Rules?
Rules are **automatic triggers** that watch your network traffic and alert you when specific conditions are met.

#### How to Use Rules:

1. **Tap "Add Rule" to create a new rule**

2. **Choose what to monitor (Metric):**
   - `outbound_bytes`: Bytes sent from your device
   - `inbound_bytes`: Bytes received to your device
   - `packet_rate`: Number of packets per second
   - `connection_count`: Number of active connections
   - `failed_connections`: Failed connection attempts

3. **Set a condition:**
   - `>` (greater than)
   - `<` (less than)
   - `==` (equals)
   - `!=` (not equals)

4. **Set a threshold:** The value to trigger the alert

5. **Choose time window:** How long to monitor (in seconds)

6. **Select action:**
   - `alert`: Show an alert notification
   - `log`: Just log it silently
   - `block`: Block the connection (requires root)

7. **Set severity:** low, medium, high, or critical

#### Example Rules:

**Rule 1: High upload detection**
- Metric: `outbound_bytes`
- Condition: `>`
- Threshold: `100000000` (100 MB)
- Time window: `60` seconds
- Action: `alert`
- Severity: `high`
- **Triggers when**: Your device sends more than 100MB in 60 seconds (possible data exfiltration)

**Rule 2: Port scan detection**
- Metric: `connection_count`
- Condition: `>`
- Threshold: `50`
- Time window: `10` seconds
- Action: `alert`
- Severity: `critical`
- **Triggers when**: More than 50 connections in 10 seconds (possible port scan)

**Rule 3: Connection flood**
- Metric: `packet_rate`
- Condition: `>`
- Threshold: `1000`
- Time window: `5` seconds
- Action: `alert`
- Severity: `medium`
- **Triggers when**: More than 1000 packets per second

### 7. 💾 Export Data
**Save your capture data**

Three export formats:

1. **PCAP Format**
   - Industry-standard packet capture format
   - Open in Wireshark for deep analysis
   - Contains raw packet data

2. **JSON Format**
   - Human-readable text format
   - Includes packets, alerts, and statistics
   - Easy to parse with scripts

3. **Evidence Bundle**
   - Complete forensic package
   - Includes PCAP, JSON, and detailed report
   - Perfect for incident response
   - Contains timeline and threat assessment

## 🎮 Usage Tips

### Starting Capture
1. Tap the **Play button (▶️)** in the top-right corner
2. Grant VPN permission when prompted
3. Capture starts automatically
4. You'll see "Packet capture started" notification

### Stopping Capture
1. Tap the **Stop button (⏹️)** in the top-right corner
2. Capture stops immediately
3. Data remains in the app for analysis

### Best Practices
- **Start capture** before using other apps to monitor their traffic
- **Check App Usage** screen to see which apps use most data
- **Set up rules** for specific threats you want to monitor
- **Export data** regularly for long-term analysis
- **Review alerts** frequently to catch security issues

## 🔒 Privacy & Security

- All data stays on your device
- No cloud uploads unless you choose to export
- VPN is local only - doesn't route through external servers
- GPS location used only for map display
- App requires explicit permission for location access

## 🐛 Troubleshooting

### App crashes when clicking Play
- **Fixed**: Foreground service type now properly declared for Android 14+

### Map shows wrong location
- **Fixed**: Now uses your device's actual GPS location
- Make sure location permission is granted
- Enable GPS in device settings

### Bottom menu missing
- **Not a bug**: Now uses hamburger menu (☰) instead
- Tap the three lines in top-left corner

## 📊 Understanding the Data

### Protocols
- **TCP**: Reliable connection-based protocol (web, email, etc.)
- **UDP**: Fast connectionless protocol (video streaming, gaming)
- **ICMP**: Network diagnostics (ping)
- **HTTP**: Unencrypted web traffic
- **HTTPS**: Encrypted web traffic (secure)
- **DNS**: Domain name lookups (converts names to IPs)

### Directions
- **Outbound**: Data leaving your device
- **Inbound**: Data coming to your device

### Colors
Each protocol has a unique color for easy identification in the UI.

## 🎯 Hackathon Tips

1. **Demo the map feature** - visually impressive
2. **Show app usage tracking** - unique feature
3. **Create custom rules** - shows flexibility
4. **Export evidence bundle** - professional feature
5. **Explain security use cases** - MITM detection, etc.

## 📱 Supported Android Versions

- Minimum: Android 8.0 (API 26)
- Tested on: Android 14 (API 34)
- Optimized for: Pixel 7 and similar devices

---

**Remember**: This is a powerful tool. Always ensure you have permission to monitor network traffic on your device and network.
