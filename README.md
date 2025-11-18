# SIP Softphone Application

A modern SIP softphone that layers a dark-mode GUI, live RTP analytics, basic media encryption and SIP SYN-cookie defenses on top of the upstream peers-sip stack. This document captures the idea, design, implementation details, protocol references, and the exact runbook so that the project can be evaluated or extended quickly.

---

## Idea

- **Unified collaboration surface:** Blend voice, chat, attachments, and call analytics into one window so operators can escalate conversations without swapping tools.
- **Operational insight by default:** Surface packet loss, jitter, latency and duration directly in the UI so network degradations are obvious to end users and NOC teams.
- **Secure-by-default posture:** Add SRTP helpers, GUI toggles for encryption, and a SIP SYN-cookie handshake to make it harder for spoofed INVITEs to exhaust sockets.
- **Leverage proven foundations:** Reuse peers-sip transports, transactions, SDP, RTP and protocol compliance while focusing effort on GUI, analytics and security improvements.

---

## Design

| Layer | Key Components | Responsibilities |
| --- | --- | --- |
| GUI (`sip-gui`) | `MainFrame`, `ChatPanel`, `CallStatisticsPanel`, `DarkTheme` | Modern dialer, mute/hangup controls, chat with attachments, live metric cards, split-pane layout. |
| Media/Analytics (`sip-lib`) | `MediaManager`, `RtpSession`, `RtpStatistics`, `SrtpCrypto` | Capture/playback, RTP session lifecycle, SRTP hooks, packet/jitter/loss tracking. |
| Security | `InviteHandler`, `SrtpCrypto`, `Config` flags | SIP SYN-cookie handshake, optional SRTP payload encryption, simple toggles exposed in config. |
| Core SIP Stack | peers-sip (transactions, dialogs, transport, SDP) | Standards-compliant REGISTER/INVITE/BYE/etc., offer/answer, DTMF, NAT helpers. |

Design priorities drawn from `@README_IMPROVEMENTS copy.md`:
- **Network analytics pipeline** polls shared `RtpStatistics` every second and paints color-coded indicators.
- **Encryption controls** rely on `SrtpCrypto` (AES-CTR + HMAC-SHA1) and can flip per session once key material is shared.
- **SIP SYN-cookie defense** issues `X-SIP-Set-Cookie` challenges before allocating dialogs, mirroring TCP SYN cookies.

---

## Implementation

### GUI & User Experience
- DarkTheme-based Swing components deliver the dialer, call controls, registration state, chat canvas, and statistics panel in a single split pane.
- `MainFrameState` transitions ensure appropriate controls (Call, Pickup, Hangup, Mute) are exposed per phase.
- Chat attachments up to 1 MB are base64-encoded into SIP `MESSAGE` payloads and shown inline.

### Network Analytics
- `sip.rtp.RtpStatistics` (atomic counters) records packets sent/received, loss, jitter, latency, and call duration.
- `CallStatisticsPanel` maps those metrics to 🟢/🟡/🔴 thresholds (packet loss & latency) so operators can visually grade quality.
- Statistics are pulled once per second while a call is active and cached per `MediaManager`.

### Encryption (Basic SRTP)
- `sip.rtp.SrtpCrypto` wraps AES-128 in counter mode with HMAC-SHA1 (80-bit tag) and IV derivation from SSRC + sequence number.
- The config flag `<enableSRTP>true</enableSRTP>` turns on payload protection and auth tagging once both peers share the master key/salt (currently pre-shared; no signaling exchange).

### SIP SYN-Cookie Handshake
- `InviteHandler` inspects incoming INVITEs. If `X-SIP-Cookie` is missing, it replies with `100 Trying` + `X-SIP-Set-Cookie` (HMAC-backed, 60s lifetime).
- Valid callers echo `X-SIP-Cookie`, at which point dialog creation resumes. Spoofed INVITEs fail to obtain RTP resources.

### Reuse of peers-sip
- Core SIP transactions, transport manager, SDP parsing, codecs, and RTP pipeline originate from peers-sip. We extend it via GUI hooks, analytics, and security toggles without rewriting protocol plumbing.

---

## How to Build and Run

### Prerequisites
- Java 8+
- Maven 3.x
- Microphone & speakers (or loopback audio device)

### Build
```bash
# Build everything
mvn clean install

# Or just the GUI with dependencies
mvn -pl sip-gui -am install

# Output: sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
```

### Run (default configuration)
```bash
java -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
```

### Run with custom config directory
```bash
java -Dsip.home=/path/to/config \
     -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
```

### Local Alice/Bob test (see `RUN.md`)
```bash
# Terminal 1
java -Dsip.home=test-alice -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar

# Terminal 2
java -Dsip.home=test-bob   -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
```
Enter `sip:bob@<your-ip>:5061` in Alice’s dialer, wait for ringing, then click **Pickup** in Bob’s window.

---

## Configuration

`conf/config.xml` (or a custom directory referenced via `-Dsip.home`) controls SIP credentials, ports, media settings, and security toggles:

```xml
<config>
  <!-- SIP account -->
  <userPart>your-username</userPart>
  <domain>sip.provider.com</domain>
  <password>your-password</password>
  
  <!-- Network -->
  <ipAddress>192.168.1.10</ipAddress>
  <sipPort>5060</sipPort>
  <rtpPort>8000</rtpPort>
  
  <!-- Media -->
  <mediaMode>captureAndPlayback</mediaMode>
  <mediaDebug>false</mediaDebug>
  
  <!-- Security -->
  <enableSRTP>false</enableSRTP>
  <enableTLS>false</enableTLS> <!-- placeholder; TLS transport not yet implemented -->
</config>
```

Key options:
- `mediaMode`: `captureAndPlayback`, `echo`, `file`, or `none`.
- `enableSRTP`: toggles SRTP payload protection (requires shared keys).
- `enableTLS`: reserved for future TLS transport work.

---

## Usage Guide

1. Launch the GUI (Alice/Bob as needed).
2. Ensure registration succeeds (status label shows success).
3. Enter a SIP URI (`sip:bob@192.168.1.50:5061`) and press **Call**.
4. Watch ringing/connected states; mute/hangup controls appear automatically.
5. Observe live metrics in `CallStatisticsPanel` and send chat messages or attachments from the right pane.
6. Click **Hangup** to terminate the dialog.

**Quality indicators**
- Packet loss: <1 % 🟢, 1‑5 % 🟡, >5 % 🔴.
- Latency: <100 ms 🟢, 100‑200 ms 🟡, >200 ms 🔴.

---

## Project Structure

```
CN-Project/
├── conf/                # Shared config & schema
├── sip-lib/             # peers-sip core + media, RTP, security extensions
│   ├── media/           # Capture, encoders, MediaManager
│   ├── rtp/             # RtpSession, RtpStatistics, SrtpCrypto
│   └── core/...         # Transactions, dialogs, transport
├── sip-gui/             # Swing application (MainFrame, ChatPanel, analytics)
└── sip-javaxsound/      # Java Sound bridge (capture/playback)
```

Each module is a Maven subproject; the root `pom.xml` orchestrates builds and dependency wiring.


---

## Tools & Technologies

- **Java Swing** – dark-mode GUI, chat pane, statistics dashboard.
- **Java Sound API (`sip-javaxsound`)** – microphone capture, speaker playback.
- **BouncyCastle 1.70** – cryptographic primitives for SRTP (AES-CTR, HMAC-SHA1).
- **Maven** – multi-module build, dependency management.
- **peers-sip** – upstream SIP/RTP/SDP implementation that this project extends.

---

## Protocol Reference (from `PROTOCOLS.md`)

| Protocol | RFC | Status in peers-sip | Notes |
| --- | --- | --- | --- |
| SIP over UDP | RFC 3261 | ✅ | Primary signaling transport. |
| RTP | RFC 3550 | ✅ | Used for audio media streams. |
| G.711 (PCMU/PCMA) | RFC 3551 | ✅ | Default audio codecs. |
| DTMF Events | RFC 4733 | ✅ | Telephony event payload type. |
| SDP | RFC 4566 | ✅ | Offer/answer negotiation. |
| SRTP | RFC 3711 | ✅ (optional) | Implemented via `SrtpCrypto`; key exchange must be pre-arranged. |
| SIP Digest Auth | RFC 2617 | ✅ | Supported by peers-sip auth stack. |
| SIP SYN Cookie | Custom | ✅ | `X-SIP-Set-Cookie`/`X-SIP-Cookie` flow guards RTP allocation. |
| TLS | RFC 3261 §26 | ❌ | Config placeholder exists; transport not yet implemented. |

> The project references the same protocol coverage that ships with peers-sip, adding SRTP helpers and the SYN-cookie handshake as incremental improvements.



