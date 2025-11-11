# AfterPackets - Device Testing Guide

## 📱 Quick Start

### Prerequisites
- Android device running Android 8.0+ (API 26+)
- USB cable for ADB connection
- ADB installed on your computer ([Download here](https://developer.android.com/studio/releases/platform-tools))
- USB Debugging enabled on your device (Settings → Developer Options → USB Debugging)

### Installation

1. **Install via ADB:**
```bash
adb install ~/Desktop/AfterPackets-FullFix-20251112-045404.apk
```

2. **Or copy to device and install manually:**
```bash
adb push ~/Desktop/AfterPackets-FullFix-20251112-045404.apk /sdcard/Download/
# Then open Files app on device and tap the APK to install
```

3. **Grant permissions when prompted:**
   - ✅ VPN permission (REQUIRED for packet capture)
   - ✅ Notification permission (for alerts)
   - ✅ Storage permission on Android 6-12 (for exports)

---

## 🧪 Testing Plan

### Test 1: Basic VPN Capture (Bidirectional Traffic)

**Goal:** Verify app captures both OUTBOUND and INBOUND packets without blocking normal traffic.

**Steps:**
1. Launch AfterPackets app
2. Tap **"Start Capture"** button
3. Accept VPN permission dialog if prompted
4. Wait for "Packet capture started" toast
5. Open **YouTube** app
6. Play a video for 30 seconds
7. Return to AfterPackets

**Expected Results:**
- ✅ YouTube video plays smoothly (no buffering/errors)
- ✅ Packet list shows BOTH:
  - **Outbound** packets (10.0.0.2 → youtube.com IPs)
  - **Inbound** packets (youtube.com IPs → 10.0.0.2)
- ✅ Stats show total packets increasing
- ✅ Direction column shows mix of "outbound" and "inbound"

**Logcat Command:**
```bash
adb logcat -s PacketCaptureService:* PacketProcessor:* PacketForwarder:* VpnSocketProtector:* native-lib:*
```

**Look for:**
- `✅ Protected socket FD=` (socket protection working)
- `⬇️ Wrote X bytes to TUN (inbound packet)` (inbound forwarding)
- `PacketProcessor: Processing packet` (packets being parsed)
- NO ERRORS about "Failed to write to TUN"

---

### Test 2: App Connectivity (YouTube + WhatsApp)

**Goal:** Verify normal apps stay online and functional during capture.

**Steps:**
1. Start capture in AfterPackets
2. Open **WhatsApp**
3. Send a text message to a contact
4. Wait for delivery (double checkmark)
5. Open **YouTube**
6. Search for a video and play it
7. Return to AfterPackets
8. Stop capture

**Expected Results:**
- ✅ WhatsApp message sends successfully
- ✅ YouTube video loads and plays without issues
- ✅ AfterPackets shows packets from both apps
- ✅ No crashes or "No internet connection" errors

**Logcat Filter:**
```bash
adb logcat -s PacketCaptureService:* VpnSocketProtector:*
```

---

### Test 3: PCAP Export to Downloads

**Goal:** Verify exported PCAP files appear in Downloads and are downloadable.

**Steps:**
1. Capture some traffic (at least 100 packets)
2. Tap **"Export"** tab
3. Tap **"Export as PCAP"**
4. Wait for success toast: "✅ Exported X packets to PCAP"
5. Open **Files** app on device
6. Navigate to **Downloads → PacketHunter** folder
7. Verify `.pcap` file is present

**Expected Results:**
- ✅ Toast shows success with file path
- ✅ File appears in Downloads/PacketHunter/
- ✅ File size > 0 bytes
- ✅ Can share file via Files app (e.g., email to yourself)
- ✅ Download on computer and open in Wireshark

**Logcat Command:**
```bash
adb logcat -s ExportManager:* MediaScanner:*
```

**Look for:**
- `✅ Exported X packets to PCAP: /storage/emulated/0/Download/PacketHunter/capture_XXX.pcap`
- `✅ File copied to Downloads via MediaStore:` (Android 10+)
- `✅ File copied to Downloads (legacy):` (Android 9-)
- `✅ File indexed by MediaScanner:` (file made visible)

**Verify on computer:**
```bash
adb pull /sdcard/Download/PacketHunter/capture_*.pcap .
wireshark capture_*.pcap
```

---

### Test 4: JSON Export to Downloads

**Goal:** Verify exported JSON files appear in Downloads with full metadata.

**Steps:**
1. Capture traffic with at least one alert (visit http://example.com to trigger HTTP alert)
2. Tap **"Export"** tab
3. Tap **"Export as JSON"**
4. Wait for success toast
5. Check Downloads/PacketHunter/ folder
6. Pull file to computer and inspect

**Expected Results:**
- ✅ JSON file appears in Downloads/PacketHunter/
- ✅ File contains valid JSON with:
  - `version`, `exportTime`, `stats`, `packets`, `alerts`, `metadata`
- ✅ Packet objects include IP, port, protocol, payload, etc.
- ✅ Alert objects included if any triggered

**Verify:**
```bash
adb pull /sdcard/Download/PacketHunter/capture_*.json .
cat capture_*.json | jq '.metadata'
cat capture_*.json | jq '.packets | length'
cat capture_*.json | jq '.alerts'
```

---

### Test 5: Interception Safety Guards

**Goal:** Verify interception features require explicit authorization.

**Steps:**
1. Fresh install of app (or clear app data)
2. Launch app
3. Tap **"Interception"** tab
4. Try to enable interception toggle
5. Observe behavior

**Expected Results:**
- ✅ Interception toggle does NOT enable without consent
- ✅ Warning dialog appears with legal notice
- ✅ User must check "I understand and consent" checkbox
- ✅ Only after consent does interception enable
- ✅ Logcat shows: `✅ Authorized Testing Mode ENABLED`

**Logcat Command:**
```bash
adb logcat -s AuthorizedTestingMode:* InterceptionManager:* AuditLogger:*
```

**Look for:**
- `❌ Cannot enable interception - Authorized Testing Mode is disabled`
- After consent: `✅ Authorized Testing Mode ENABLED - User consent recorded`
- `📝 Audit log: CONSENT_GRANTED`

**Verify audit log:**
```bash
adb shell "run-as com.packethunter.mobile cat /data/data/com.packethunter.mobile/files/interception_audit.log"
```

---

### Test 6: Direction Detection

**Goal:** Verify packets show correct direction (inbound vs outbound).

**Steps:**
1. Start capture
2. Open browser and visit http://example.com
3. Let page fully load
4. Return to AfterPackets
5. Filter packets by "example.com" IP

**Expected Results:**
- ✅ DNS query shows **"outbound"** (10.0.0.2 → 8.8.8.8)
- ✅ DNS response shows **"inbound"** (8.8.8.8 → 10.0.0.2)
- ✅ HTTP request shows **"outbound"** (10.0.0.2 → example.com)
- ✅ HTTP response shows **"inbound"** (example.com → 10.0.0.2)

**Logcat Filter:**
```bash
adb logcat -s PacketProcessor:* | grep -E "direction|Processing packet"
```

---

### Test 7: Alert Rules

**Goal:** Verify alert rules trigger correctly.

**Steps:**
1. Start capture
2. Visit http://example.com (triggers HTTP alert)
3. Visit https://suspicious-domain.ru (triggers foreign server alert if enabled)
4. Check **"Alerts"** tab in app

**Expected Results:**
- ✅ Alert appears for HTTP traffic
- ✅ Alert includes timestamp, severity, description
- ✅ Tap alert to see full details

**Logcat Command:**
```bash
adb logcat -s PacketProcessor:* AlertManager:*
```

---

## 🔧 Troubleshooting

### Issue: VPN permission denied
**Fix:** Go to Settings → Apps → AfterPackets → Permissions → Grant VPN permission manually

### Issue: No packets captured
**Check:**
```bash
adb logcat -s PacketCaptureService:* | grep -E "Started|Stopped|Error"
```
- Ensure VPN is active (check notification bar)
- Restart capture
- Reboot device if needed

### Issue: Export file not visible
**Check:**
```bash
adb shell "ls -la /sdcard/Download/PacketHunter/"
```
- If file exists but not visible, trigger media scan:
```bash
adb shell "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/PacketHunter/"
```

### Issue: Apps can't connect during capture
**Check logcat for:**
```bash
adb logcat -s VpnSocketProtector:* | grep -E "Protected|Failed"
```
- If seeing "❌ Failed to protect socket", restart app
- Check for "✅ Protected socket FD=" messages

### Issue: Inbound packets not appearing
**Check:**
```bash
adb logcat -s native-lib:* | grep -E "Wrote.*TUN|inbound"
```
- Should see: `⬇️ Wrote X bytes to TUN (inbound packet)`
- If not, native forwarder may have issue

---

## 📊 Performance Benchmarks

### Expected Performance:
- **Capture rate:** 100-500 packets/second
- **CPU usage:** 5-15% while capturing
- **Memory usage:** 50-150 MB
- **Battery drain:** ~10-15% per hour of active capture

### Monitor Performance:
```bash
# CPU usage
adb shell "top -n 1 | grep packethunter"

# Memory usage
adb shell "dumpsys meminfo com.packethunter.mobile"

# Battery stats
adb shell "dumpsys batterystats com.packethunter.mobile"
```

---

## 🐛 Full Debug Logs

### Capture ALL app logs:
```bash
adb logcat -s PacketCaptureService:* PacketProcessor:* PacketForwarder:* \
  VpnSocketProtector:* InterceptionManager:* ExportManager:* \
  AuthorizedTestingMode:* AuditLogger:* native-lib:* \
  > afterpackets_debug.log
```

### Native layer only:
```bash
adb logcat -s native-lib:* packet_forwarder:* native_interface:*
```

### Export + file operations only:
```bash
adb logcat -s ExportManager:* MediaScanner:*
```

### Interception + authorization only:
```bash
adb logcat -s InterceptionManager:* AuthorizedTestingMode:* AuditLogger:*
```

---

## 📦 Build Info

- **APK:** `AfterPackets-FullFix-20251112-045404.apk`
- **Size:** 23 MB
- **Min Android:** 8.0 (API 26)
- **Target Android:** 14 (API 34)
- **Branch:** `fix/vpn-inbound-forwarding`

### Commits:
1. **a0a7e97e** - Socket protection with VpnSocketProtector.protectFd()
2. **79d936b3** - Export fix with MediaStore for Android 10+
3. **550ef541** - Authorized testing mode for interception safety

---

## ⚠️ Known Limitations

1. **Root Required:** No (works on non-rooted devices)
2. **VPN Conflicts:** Cannot run alongside other VPN apps
3. **TLS Decryption:** Not supported (encrypted payloads shown as hex)
4. **IPv6:** Limited support (primarily IPv4 focus)

---

## 📝 Testing Checklist

- [ ] Basic VPN capture with bidirectional traffic
- [ ] YouTube plays smoothly during capture
- [ ] WhatsApp sends messages during capture
- [ ] PCAP export appears in Downloads
- [ ] JSON export appears in Downloads
- [ ] Interception requires authorization consent
- [ ] Direction detection (inbound/outbound) correct
- [ ] Alert rules trigger on HTTP traffic
- [ ] No crashes during 10-minute capture session
- [ ] Files downloadable via ADB pull

---

## 🚀 Next Steps After Testing

1. **Report Issues:** Create GitHub issue with logcat output
2. **Feature Requests:** Use GitHub Discussions
3. **Performance:** Share logcat + battery stats if sluggish
4. **Crash Reports:** `adb logcat > crash.log` immediately after crash

---

**Happy Testing! 🎉**
