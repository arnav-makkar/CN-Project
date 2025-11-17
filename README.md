# SIP Softphone Application

A modern, feature-rich SIP softphone built in Java with dark mode UI, real-time call quality monitoring, and encryption support.

## Features

### 🎨 Modern Dark Mode UI
- Beautiful dark theme with smooth animations
- Rounded buttons and modern components
- Intuitive user interface
- Real-time visual feedback

### 📊 Real-Time Call Quality Monitoring
- **Packet Loss Tracking**: Live percentage display with color-coded indicators
- **Latency Measurement**: Real-time round-trip time in milliseconds
- **Jitter Monitoring**: Network stability metrics
- **Call Duration**: Formatted timer showing call length
- **Packet Statistics**: Sent/received packet counts

### 🔒 Security Features
- **SRTP Support**: Encrypt RTP media streams using AES encryption
- **Authentication**: SIP digest authentication (RFC 2617)
- **Configuration**: Easy encryption enable/disable via config file

### 🎯 Core SIP Features
- Full SIP User Agent (UAC/UAS) implementation
- REGISTER, INVITE, ACK, BYE, CANCEL, OPTIONS methods
- SDP offer/answer negotiation
- DTMF support (RFC 4733)
- NAT traversal (RFC 3581 rport)
- G.711 codecs (PCMU/PCMA)

## Requirements

- Java 8 or higher
- Maven 3.x
- Microphone and speakers for audio calls

## Building

```bash
# Build all modules
mvn clean install

# Build GUI module only
mvn -pl sip-gui -am install
```

The executable JAR will be created at:
`sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar`

## Running

### Basic Usage

```bash
java -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
```

### With Custom Configuration

```bash
java -Dsip.home=/path/to/config -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
```

## Configuration

Edit `conf/config.xml` to configure your SIP account:

```xml
<config>
  <!-- SIP Account Settings -->
  <userPart>your-username</userPart>
  <domain>sip.provider.com</domain>
  <password>your-password</password>
  
  <!-- Network Settings -->
  <sipPort>5060</sipPort>
  <rtpPort>8000</rtpPort>
  
  <!-- Media Settings -->
  <mediaMode>captureAndPlayback</mediaMode>
  <mediaDebug>false</mediaDebug>
  
  <!-- Security Settings -->
  <enableSRTP>false</enableSRTP>
  <enableTLS>false</enableTLS>
</config>
```

### Configuration Options

- **userPart**: SIP username
- **domain**: SIP domain/server
- **password**: SIP account password
- **sipPort**: Local SIP listening port (0 for random)
- **rtpPort**: Local RTP port (0 for random)
- **mediaMode**: `captureAndPlayback`, `echo`, `none`, or `file`
- **enableSRTP**: Enable SRTP encryption for media (true/false)
- **enableTLS**: Enable TLS for signaling (true/false)

## Usage Guide

### Making a Call

1. Enter the SIP URI in the text field (e.g., `sip:alice@example.com`)
2. Click the **Call** button
3. Wait for the remote party to answer
4. View real-time call quality statistics during the call
5. Click **Hangup** to end the call

### Call Quality Indicators

- 🟢 **Green**: Excellent quality
- 🟡 **Yellow**: Acceptable quality
- 🔴 **Red**: Poor quality

**Packet Loss:**
- Green: < 1%
- Yellow: 1-5%
- Red: > 5%

**Latency:**
- Green: < 100ms
- Yellow: 100-200ms
- Red: > 200ms

## Project Structure

```
sip-softphone/
├── pom.xml                      # Parent Maven configuration
├── README.md                    # This file
├── conf/                        # Configuration files
│   ├── config.xml              # Main configuration
│   └── config.xsd              # XML schema
├── sip-lib/                     # Core SIP/RTP library
│   └── src/main/java/
│       └── net/sourceforge/sip/
│           ├── rtp/            # RTP implementation
│           │   ├── RtpSession.java
│           │   ├── RtpStatistics.java
│           │   └── SrtpCrypto.java
│           ├── sip/            # SIP stack
│           └── media/          # Audio encoding/decoding
├── sip-gui/                     # Graphical user interface
│   └── src/main/java/
│       └── net/sourceforge/sip/gui/
│           ├── MainFrame.java
│           ├── CallFrame.java
│           ├── DarkTheme.java
│           └── CallStatisticsPanel.java
└── sip-javaxsound/             # Java Sound API integration
```

## Technical Details

### RTP Statistics Collection

The system tracks:
- Sequence numbers to detect packet loss
- Timestamps for latency calculation
- Arrival times for jitter measurement
- Packet counts (sent/received/lost)

### SRTP Encryption

When enabled:
- Uses AES-128 in counter mode
- HMAC-SHA1 authentication tags
- Automatic IV derivation from RTP header
- Configurable master key and salt

### Network Quality Metrics

- **Packet Loss**: `(lost_packets / expected_packets) * 100`
- **Latency**: Round-trip time measurement
- **Jitter**: Variance in packet arrival times

## Troubleshooting

### No Audio
- Check microphone/speaker permissions
- Verify codec compatibility (G.711)
- Ensure RTP ports are not blocked

### Connection Failed
- Verify SIP credentials
- Check network connectivity
- Confirm SIP server address
- Try different ports if blocked

### High Packet Loss
- Check network quality
- Try wired connection instead of WiFi
- Reduce network congestion
- Check firewall settings

## Dependencies

- **BouncyCastle** (v1.70): Cryptography library for SRTP
- **Java Sound API**: Audio capture and playback
- **Java Swing**: GUI framework

## License

This project is licensed under the GNU General Public License v3.0.

---

**Version**: 1.0.0  
**Last Updated**: 2025  
**Java Version**: 8+  
**Build Tool**: Maven 3.x
