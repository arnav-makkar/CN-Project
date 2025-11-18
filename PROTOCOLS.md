# Network Protocols Implementation Documentation

This document provides a comprehensive overview of all network protocols implemented or used in this SIP softphone project, including their implementation details, usage patterns, and identification of redundant or unused components.

---

## Table of Contents

1. [Protocol Overview](#protocol-overview)
2. [SIP (Session Initiation Protocol)](#sip-session-initiation-protocol)
3. [SDP (Session Description Protocol)](#sdp-session-description-protocol)
4. [RTP (Real-time Transport Protocol)](#rtp-real-time-transport-protocol)
5. [DTMF (RFC 4733)](#dtmf-rfc-4733)
6. [HTTP Digest Authentication (RFC 2617)](#http-digest-authentication-rfc-2617)
7. [Transport Layer Protocols](#transport-layer-protocols)
8. [SRTP (Secure RTP)](#srtp-secure-rtp)
9. [Protocol Usage Flow](#protocol-usage-flow)
10. [Redundant/Unused Components](#redundantunused-components)

---

## Protocol Overview

This SIP softphone implements the following protocols:

| Protocol | RFC | Status | Purpose |
|----------|-----|--------|---------|
| SIP | RFC 3261 | ✅ Fully Implemented | Call signaling |
| SDP | RFC 4566 | ✅ Implemented | Media negotiation |
| RTP | RFC 3550 | ✅ Implemented | Media transport |
| DTMF | RFC 4733 | ✅ Implemented | DTMF events |
| HTTP Digest | RFC 2617 | ✅ Implemented | SIP authentication |
| RFC 3581 | RFC 3581 | ✅ Partial | NAT traversal (rport) |
| UDP | - | ✅ Primary Transport | SIP & RTP transport |
| TCP | - | ❌ Not Implemented | Commented out |
| SRTP | RFC 3711 | ✅ Optional | Encrypted RTP |

---

## SIP (Session Initiation Protocol)

### RFC 3261 Implementation

**Status:** ✅ Fully implemented core subset

### Implemented SIP Methods

| Method | Implementation Status | Location |
|--------|----------------------|----------|
| **INVITE** | ✅ Full | `sip.core.useragent.handlers.InviteHandler` |
| **ACK** | ✅ Full | `sip.transaction.InviteClientTransaction.createAndSendAck()` |
| **BYE** | ✅ Full | `sip.core.useragent.handlers.ByeHandler` |
| **REGISTER** | ✅ Full | `sip.core.useragent.handlers.RegisterHandler` |
| **CANCEL** | ✅ Full | `sip.core.useragent.handlers.CancelHandler` |
| **OPTIONS** | ✅ Basic | `sip.core.useragent.handlers.OptionsHandler` |
| **MESSAGE** | ✅ Basic | `sip.core.useragent.handlers.MessageHandler` |

### SIP Transaction Management

**Location:** `sip.transaction.*`

- **InviteClientTransaction** - Client-side INVITE transactions
  - States: INIT → CALLING → PROCEEDING → COMPLETED → TERMINATED
  - File: `sip-lib/src/main/java/sip/transaction/InviteClientTransaction.java`
  
- **InviteServerTransaction** - Server-side INVITE transactions
  - States: INIT → PROCEEDING → COMPLETED → CONFIRMED → TERMINATED
  - File: `sip-lib/src/main/java/sip/transaction/InviteServerTransaction.java`

- **NonInviteClientTransaction** - Client-side non-INVITE (BYE, REGISTER, etc.)
  - File: `sip-lib/src/main/java/sip/transaction/NonInviteClientTransaction.java`

- **NonInviteServerTransaction** - Server-side non-INVITE
  - File: `sip-lib/src/main/java/sip/transaction/NonInviteServerTransaction.java`

### SIP Dialog Management

**Location:** `sip.transactionuser.Dialog`

- Dialog state machine with 4 states: INIT, EARLY, CONFIRMED, TERMINATED
- File: `sip-lib/src/main/java/sip/transactionuser/Dialog.java`

### SIP Message Parsing/Encoding

**Location:** `sip.syntaxencoding.*`

- **SipParser** - Parses raw SIP messages
  - File: `sip-lib/src/main/java/sip/syntaxencoding/SipParser.java`
  
- **SipHeaders** - Header management
  - File: `sip-lib/src/main/java/sip/syntaxencoding/SipHeaders.java`

- **SipURI** - URI parsing
  - File: `sip-lib/src/main/java/sip/syntaxencoding/SipURI.java`

### SIP Response Codes Implemented

- **1xx Provisional:** 100 Trying, 180 Ringing
- **2xx Success:** 200 OK
- **3xx Redirection:** Not implemented
- **4xx Client Error:** 401 Unauthorized, 405 Method Not Allowed, 407 Proxy Authentication Required, 486 Busy Here, 487 Request Terminated
- **5xx Server Error:** 500 Server Internal Error
- **6xx Global Failure:** 600 Busy Everywhere

### SIP Usage During Call Flow

#### Call Initiation (UAC)
1. **INVITE Request** created in `UAC.invite()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/UAC.java:114-119`
   - Creates SDP offer via `SDPManager.createSessionDescription()`
   - Sends via `InviteClientTransaction.start()`

2. **Provisional Responses (1xx)** handled in `InviteHandler.provResponseReceived()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/handlers/InviteHandler.java:423-472`
   - Updates dialog state to EARLY

3. **200 OK Response** handled in `InviteHandler.successResponseReceived()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/handlers/InviteHandler.java:474-520`
   - Parses SDP answer
   - Sends ACK via `InviteClientTransaction.createAndSendAck()`
   - Starts RTP session

#### Call Acceptance (UAS)
1. **INVITE Received** in `InviteHandler.handleInitialInvite()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/handlers/InviteHandler.java:74-99`
   - Sends 180 Ringing immediately
   - Creates dialog

2. **Accept Call** via `InviteHandler.acceptCall()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/handlers/InviteHandler.java:276-285`
   - Sends 200 OK with SDP answer
   - Starts RTP session

#### Call Teardown
1. **BYE Request** handled in `ByeHandler.handleBye()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/handlers/ByeHandler.java:55-98`
   - Sends 200 OK response
   - Stops media session
   - Removes dialog

---

## SDP (Session Description Protocol)

### RFC 4566 Implementation

**Status:** ✅ Basic implementation (offer/answer model)

### Implemented SDP Fields

| Field | Type | Status | Location |
|-------|------|--------|----------|
| v (version) | Required | ✅ | `SdpParser.parse()` |
| o (origin) | Required | ✅ | Parsed in `SdpParser` |
| s (session name) | Required | ✅ | Parsed in `SdpParser` |
| c (connection) | Optional | ✅ | Parsed per media description |
| m (media) | Required | ✅ | Parsed in `SdpParser` |
| a (attribute) | Optional | ✅ | Parsed (rtpmap, sendrecv) |

### SDP Implementation Details

**Main Classes:**
- **SDPManager** - High-level SDP management
  - File: `sip-lib/src/main/java/sip/sdp/SDPManager.java`
  - Methods:
    - `parse(byte[] sdp)` - Parse incoming SDP
    - `createSessionDescription()` - Create SDP offer/answer
    - `getMediaDestination()` - Extract media destination from SDP

- **SdpParser** - Low-level SDP parsing
  - File: `sip-lib/src/main/java/sip/sdp/SdpParser.java`
  - Parses SDP text format into `SessionDescription` object

- **SessionDescription** - SDP data structure
  - File: `sip-lib/src/main/java/sip/sdp/SessionDescription.java`

### Supported Codecs

- **PCMU (G.711 μ-law)** - Payload type 0
  - Location: `sip-lib/src/main/java/sip/rtp/RFC3551.java`
  
- **PCMA (G.711 A-law)** - Payload type 8
  - Location: `sip-lib/src/main/java/sip/rtp/RFC3551.java`

- **DTMF (telephone-event)** - Payload type 101
  - Location: `sip-lib/src/main/java/sip/rtp/RFC4733.java`

### SDP Usage During Call Flow

#### Call Initiation
1. **SDP Offer Creation** in `SDPManager.createSessionDescription()`
   - Location: `sip-lib/src/main/java/sip/sdp/SDPManager.java:88-131`
   - Includes: origin, session name, connection info, media description
   - Attached to INVITE body

2. **SDP Answer Parsing** in `InviteHandler.successResponseReceived()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/handlers/InviteHandler.java:474-520`
   - Extracts remote RTP address/port and codec

#### Call Acceptance
1. **SDP Offer Parsing** in `InviteHandler.sendSuccessfulResponse()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/handlers/InviteHandler.java:176-274`
   - Parses offer from INVITE body

2. **SDP Answer Creation** in `SDPManager.createSessionDescription(offer, localRtpPort)`
   - Location: `sip-lib/src/main/java/sip/sdp/SDPManager.java:88-131`
   - Codec negotiation (selects first matching codec)
   - Attached to 200 OK body

---

## RTP (Real-time Transport Protocol)

### RFC 3550 Implementation

**Status:** ✅ Core RTP implementation (no RTCP)

### RTP Header Implementation

**RTP Packet Structure:**
- **Version** (2 bits) - Always 2
- **Padding** (1 bit) - Not used
- **Extension** (1 bit) - Not used
- **CSRC Count** (4 bits) - Supported
- **Marker** (1 bit) - Used for DTMF
- **Payload Type** (7 bits) - PCMU (0), PCMA (8), DTMF (101)
- **Sequence Number** (16 bits) - For packet loss detection
- **Timestamp** (32 bits) - For jitter calculation
- **SSRC** (32 bits) - Synchronization source identifier
- **CSRC List** (variable) - Contributing sources

**Location:** `sip-lib/src/main/java/sip/rtp/RtpPacket.java`

### RTP Implementation Details

**Main Classes:**
- **RtpSession** - RTP session management
  - File: `sip-lib/src/main/java/sip/rtp/RtpSession.java`
  - Methods:
    - `start()` - Starts RTP receiver thread
    - `send(RtpPacket)` - Sends RTP packet
    - `addRtpListener()` - Registers packet receiver

- **RtpParser** - RTP packet encoding/decoding
  - File: `sip-lib/src/main/java/sip/rtp/RtpParser.java`
  - Methods:
    - `decode(byte[])` - Parses raw RTP packet
    - `encode(RtpPacket)` - Creates raw RTP packet

- **RtpStatistics** - RTP quality metrics
  - File: `sip-lib/src/main/java/sip/rtp/RtpStatistics.java`
  - Tracks: packet loss, latency, jitter, call duration

### RTP Usage During Media Transport

#### Outgoing Media (Capture → RTP)
1. **Audio Capture** in `Capture.readData()`
   - Location: `sip-lib/src/main/java/sip/media/Capture.java:34-63`
   - Reads from `SoundSource` (microphone)

2. **Audio Encoding** in `PcmuEncoder` or `PcmaEncoder`
   - Location: `sip-lib/src/main/java/sip/media/PcmuEncoder.java` / `PcmaEncoder.java`
   - Converts PCM to G.711

3. **RTP Packet Creation** in `RtpSender.run()`
   - Location: `sip-lib/src/main/java/sip/media/RtpSender.java`
   - Creates RTP packet with sequence number, timestamp, SSRC

4. **RTP Transmission** in `RtpSession.send()`
   - Location: `sip-lib/src/main/java/sip/rtp/RtpSession.java:94-110`
   - Sends via UDP `DatagramSocket`

#### Incoming Media (RTP → Playback)
1. **RTP Reception** in `RtpSession.Receiver.run()`
   - Location: `sip-lib/src/main/java/sip/rtp/RtpSession.java:112-160`
   - Receives UDP packets
   - Parses via `RtpParser.decode()`

2. **RTP Statistics** in `RtpStatistics.recordReceivedPacket()`
   - Location: `sip-lib/src/main/java/sip/rtp/RtpStatistics.java:50-89`
   - Calculates packet loss, jitter

3. **Audio Decoding** in `PcmuDecoder` or `PcmaDecoder`
   - Location: `sip-lib/src/main/java/sip/media/PcmuDecoder.java` / `PcmaDecoder.java`
   - Converts G.711 to PCM

4. **Audio Playback** in `AbstractSoundManager.writeData()`
   - Location: `sip-lib/src/main/java/sip/media/AbstractSoundManager.java`
   - Writes to speakers

### RTCP Status

**❌ RTCP is NOT implemented**

- No RTCP sender/receiver
- No RTCP packet parsing
- Statistics are calculated from RTP packets only

---

## DTMF (RFC 4733)

### Implementation Status

**Status:** ✅ Implemented for DTMF events over RTP

### DTMF Implementation

**Location:** `sip-lib/src/main/java/sip/rtp/RFC4733.java`

- **Payload Type:** 101 (telephone-event)
- **Event Encoding:** Standard RFC 4733 format

**DTMF Packet Structure:**
- Event (8 bits) - DTMF digit code (0-15)
- Volume (8 bits) - Volume level (bit 7 = end event flag)
- Duration (16 bits) - Event duration

### DTMF Usage

**Sending DTMF:**
- Location: `sip-lib/src/main/java/sip/media/DtmfFactory.java`
- Creates RTP packets with payload type 101
- Marker bit set for start of event

**Receiving DTMF:**
- Location: `sip-lib/src/main/java/sip/media/IncomingRtpReader.java:54-104`
- Detects payload type 101
- Parses event code and end flag
- Notifies `MediaManager.receivedDtmf()`

---

## HTTP Digest Authentication (RFC 2617)

### Implementation Status

**Status:** ✅ Implemented for SIP authentication

### Implementation Details

**Location:** `sip-lib/src/main/java/sip/core/useragent/ChallengeManager.java`

**Supported Features:**
- Digest authentication scheme
- Nonce handling
- Response calculation (MD5)
- Username/password authentication
- Realm support

**RFC 2617 Constants:**
- Location: `sip-lib/src/main/java/sip/RFC2617.java`

### Authentication Flow

1. **401/407 Challenge** received
   - Location: `sip.core.useragent.handlers.*.errResponseReceived()`

2. **Challenge Handling** in `ChallengeManager.handleChallenge()`
   - Location: `sip-lib/src/main/java/sip/core/useragent/ChallengeManager.java`
   - Extracts nonce, realm, algorithm
   - Calculates response hash

3. **Re-request with Authorization** header
   - Adds `Authorization` or `Proxy-Authorization` header
   - Retransmits original request

---

## Transport Layer Protocols

### UDP (User Datagram Protocol)

**Status:** ✅ Primary transport for SIP and RTP

**SIP over UDP:**
- **Location:** `sip-lib/src/main/java/sip/transport/UdpMessageSender.java`
- **Location:** `sip-lib/src/main/java/sip/transport/UdpMessageReceiver.java`
- Uses `DatagramSocket` for SIP signaling
- Default port: 5060

**RTP over UDP:**
- Uses separate `DatagramSocket` for media
- Default port: 8000 (configurable)
- Location: `sip-lib/src/main/java/sip/rtp/RtpSession.java`

### TCP (Transmission Control Protocol)

**Status:** ❌ Not implemented (commented out)

**Evidence:**
- TCP code exists but is commented out in `TransportManager.java`
- Location: `sip-lib/src/main/java/sip/transport/TransportManager.java:233-247`
- Auto-fallback to TCP for large messages (>1300 bytes) exists but not functional
- Location: `sip-lib/src/main/java/sip/transport/TransportManager.java:91-93`

**Redundant Code:**
- TCP transport detection in Via header parsing
- Location: `sip-lib/src/main/java/sip/transport/TransportManager.java:198-200`

### SCTP (Stream Control Transmission Protocol)

**Status:** ❌ Not implemented

**Evidence:**
- Constants defined in `RFC3261.java`
- No implementation code found

---

## SRTP (Secure RTP)

### Implementation Status

**Status:** ✅ Implemented but optional (disabled by default)

### SRTP Implementation

**Location:** `sip-lib/src/main/java/sip/rtp/SrtpCrypto.java`

**Features:**
- AES-128 encryption in counter mode (SIC)
- HMAC-SHA1 authentication (10-byte tag)
- IV derivation from RTP header (SSRC + sequence number)
- Master key and salt support

**Configuration:**
- Enabled via `config.xml`: `<enableSRTP>true</enableSRTP>`
- Location: `sip-lib/src/main/java/sip/Config.java`

**Usage:**
- Encrypts RTP payload before transmission
- Decrypts RTP payload after reception
- Authentication tag appended to encrypted packet

**Limitations:**
- No key exchange mechanism (keys must be pre-shared)
- No SRTP key derivation (RFC 3711 key derivation not implemented)
- Master key generation uses simple random, not proper key exchange

---

## Protocol Usage Flow

### Complete Call Flow

#### 1. Call Initiation (Alice calls Bob)

```
1. SIP INVITE (UDP)
   ├─ SDP Offer (PCMU/PCMA codecs, local RTP port)
   └─ Via: UDP, From, To, Call-ID, CSeq, Contact

2. SIP 180 Ringing (UDP)
   └─ Provisional response

3. SIP 200 OK (UDP)
   ├─ SDP Answer (selected codec, remote RTP port)
   └─ To tag added

4. SIP ACK (UDP)
   └─ Confirms 200 OK

5. RTP Media Stream (UDP)
   ├─ Audio packets (payload type 0 or 8)
   ├─ Sequence numbers increment
   ├─ Timestamps for jitter calculation
   └─ Optional: SRTP encryption
```

#### 2. Call Acceptance (Bob answers)

```
1. Receive SIP INVITE
   └─ Parse SDP offer

2. Send SIP 180 Ringing
   └─ Immediate response

3. User clicks "Pickup"
   ├─ Create SDP answer
   ├─ Send SIP 200 OK
   └─ Start RTP session

4. Receive SIP ACK
   └─ Call established

5. RTP Media Stream
   └─ Bidirectional audio
```

#### 3. Call Teardown

```
1. SIP BYE (UDP)
   └─ From either party

2. SIP 200 OK (UDP)
   └─ Acknowledges BYE

3. Stop RTP Session
   ├─ Close DatagramSocket
   └─ Stop capture/playback threads
```

### Registration Flow

```
1. SIP REGISTER (UDP)
   ├─ Contact header
   ├─ Expires header
   └─ Authorization (if challenged)

2. SIP 401 Unauthorized (UDP)
   └─ WWW-Authenticate header with nonce

3. SIP REGISTER (UDP)
   └─ Authorization header with response

4. SIP 200 OK (UDP)
   └─ Registration successful
```

---

## Redundant/Unused Components

### 1. Unused Classes

| Class | Location | Reason |
|-------|----------|--------|
| **SDPMessage** | `sip.sdp.SDPMessage.java` | Empty class, never used |
| **SipEvent** | `sip.core.useragent.SipEvent.java` | Defined but never instantiated |
| **SipListeningPoint** | `sip.transaction.SipListeningPoint.java` | Defined but never used |
| **RunnableSipParser** | `sip.syntaxencoding.RunnableSipParser.java` | Empty stub with TODO |
| **JavaConfig** | `sip.JavaConfig.java` | Alternative config, only XmlConfig used |

### 2. Incomplete Implementations

| Component | Location | Issue |
|-----------|----------|-------|
| **TCP Transport** | `TransportManager.java:233-247` | Code commented out, not functional |
| **SRTP Key Exchange** | `SrtpCrypto.java` | No proper key derivation (RFC 3711) |
| **RTCP** | N/A | Not implemented at all |
| **JavaConfig.save()** | `JavaConfig.java:save()` | Throws RuntimeException("not implemented") |

### 3. Redundant Protocol Paths

#### TCP Detection Without Implementation
- **Location:** `TransportManager.java:91-93, 198-200`
- **Issue:** Code detects TCP in Via headers and attempts to use it, but TCP implementation is commented out
- **Recommendation:** Remove TCP detection or implement TCP transport

#### SCTP Constants Without Implementation
- **Location:** `RFC3261.java` (TRANSPORT_SCTP constant)
- **Issue:** Constant defined but never used
- **Recommendation:** Remove if SCTP not planned

### 4. Overly Complex/Unused Features

#### Multicast Support
- **Location:** `TransportManager.java:95-101`
- **Issue:** Code handles multicast addresses but feature likely unused
- **Recommendation:** Remove if not needed

#### TLS Support
- **Location:** `Config.java`, `XmlConfig.java`
- **Issue:** Configuration option exists but no TLS implementation
- **Recommendation:** Remove config option or implement TLS

### 5. Legacy/Commented Code

- **TCP Socket Code:** `TransportManager.java:233-247` (commented)
- **TcpMessageReceiver:** Referenced but not implemented
- **Various TODOs:** Multiple TODO comments indicating incomplete features

---

## Summary

### Essential Protocols (Core Functionality)
- ✅ **SIP (RFC 3261)** - Full implementation
- ✅ **SDP (RFC 4566)** - Basic offer/answer
- ✅ **RTP (RFC 3550)** - Core RTP (no RTCP)
- ✅ **UDP** - Primary transport
- ✅ **RFC 4733** - DTMF support
- ✅ **RFC 2617** - SIP authentication

### Optional/Incomplete Protocols
- ⚠️ **SRTP** - Implemented but requires manual key configuration
- ❌ **TCP** - Not implemented (commented out)
- ❌ **RTCP** - Not implemented
- ❌ **TLS** - Not implemented (config exists but no code)
- ❌ **SCTP** - Not implemented

### Recommended Cleanup
1. Remove unused classes: `SDPMessage`, `SipEvent`, `SipListeningPoint`, `RunnableSipParser`
2. Remove or implement TCP transport code
3. Remove TLS/SCTP configuration if not planned
4. Complete or remove `JavaConfig.save()` method
5. Remove commented TCP code or implement it properly

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-18  
**Project:** SIP Softphone (CN-Project)

