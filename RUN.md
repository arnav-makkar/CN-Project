# Build and Run

## Build
```bash
mvn clean package
```

## Run Alice (Test Directory)
```bash
java -Dsip.home=test-alice -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
```

## Run Bob (Test Directory)
```bash
java -Dsip.home=test-bob -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
```

## Test Directories
Test directories with pre-configured settings are available:
- `test-alice/conf/config.xml` - Alice configuration (SIP port 5060, RTP port 40000)
- `test-bob/conf/config.xml` - Bob configuration (SIP port 5061, RTP port 40002)

## Making Calls

### Call Bob from Alice

When both Alice and Bob are running on the same machine/network:

1. **Start Alice:**
   ```bash
   java -Dsip.home=test-alice -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
   ```

2. **Start Bob:**
   ```bash
   java -Dsip.home=test-bob -jar sip-gui/target/sip-gui-1.0.0-jar-with-dependencies.jar
   ```

3. **In Alice's window, enter one of these SIP URIs:**
   - `sip:bob@192.168.1.197:5061` (direct IP call - recommended for local testing)
   - `sip:bob@local:5061` (if domain resolution works)
   - `sip:bob@local` (may require SIP proxy)

4. **Click the "Call" button** or press Enter

5. **Bob will receive an incoming call** - click "Pickup" to answer

### SIP URI Format

For direct calls (no SIP server):
- `sip:username@ipaddress:port` - Direct call to specific IP and port
- Example: `sip:bob@192.168.1.197:5061`

For calls through a SIP server:
- `sip:username@domain` - Uses domain resolution
- Example: `sip:bob@local`

**Note:** Update the IP address (`192.168.1.197`) to match your actual network IP address.

## Custom Configs
To create your own config directories:
1. Create a directory (e.g., `alice/conf/`)
2. Copy `conf/config.xml` as a template
3. Update the following in your config:
   - `ipAddress`: Your local IP address
   - `userPart`: SIP username
   - `domain`: SIP domain
   - `sipPort`: Unique SIP port (different for each instance)
   - `rtpPort`: Unique RTP port (different for each instance, must be even)

