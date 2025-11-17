// Peers SIP Softphone - GPL v3 License

package sip.rtp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RtpStatistics {
    
    private final AtomicInteger packetsSent = new AtomicInteger(0);
    private final AtomicInteger packetsReceived = new AtomicInteger(0);
    private final AtomicInteger packetsLost = new AtomicInteger(0);
    private final AtomicInteger expectedPackets = new AtomicInteger(0);
    
    private volatile int lastSequenceNumber = -1;
    private volatile boolean firstPacket = true;
    
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicInteger latencySamples = new AtomicInteger(0);
    
    private final AtomicLong totalJitter = new AtomicLong(0);
    private volatile long lastArrivalTime = 0;
    private volatile long lastTimestamp = 0;
    
    private volatile long callStartTime = 0;
    
    public void startCall() {
        callStartTime = System.currentTimeMillis();
        reset();
    }
    
    public void reset() {
        packetsSent.set(0);
        packetsReceived.set(0);
        packetsLost.set(0);
        expectedPackets.set(0);
        lastSequenceNumber = -1;
        firstPacket = true;
        totalLatency.set(0);
        latencySamples.set(0);
        totalJitter.set(0);
        lastArrivalTime = 0;
        lastTimestamp = 0;
    }
    
    public void recordSentPacket() {
        packetsSent.incrementAndGet();
    }
    
    public void recordReceivedPacket(int sequenceNumber, long timestamp) {
        packetsReceived.incrementAndGet();
        
        if (firstPacket) {
            lastSequenceNumber = sequenceNumber;
            firstPacket = false;
            expectedPackets.set(1);
            lastTimestamp = timestamp;
            lastArrivalTime = System.nanoTime();
            return;
        }
        
        int expectedSeq = (lastSequenceNumber + 1) & 0xFFFF;
        int receivedSeq = sequenceNumber & 0xFFFF;
        
        if (receivedSeq != expectedSeq) {
            int gap;
            if (receivedSeq > expectedSeq) {
                gap = receivedSeq - expectedSeq;
            } else {
                gap = (0xFFFF - expectedSeq) + receivedSeq + 1;
            }
            packetsLost.addAndGet(gap);
            expectedPackets.addAndGet(gap + 1);
        } else {
            expectedPackets.incrementAndGet();
        }
        
        lastSequenceNumber = receivedSeq;
        
        long currentTime = System.nanoTime();
        if (lastArrivalTime > 0) {
            long arrivalDelta = currentTime - lastArrivalTime;
            long timestampDelta = (timestamp - lastTimestamp) * 125000;
            long jitter = Math.abs(arrivalDelta - timestampDelta);
            totalJitter.addAndGet(jitter);
        }
        lastArrivalTime = currentTime;
        lastTimestamp = timestamp;
    }
    
    public void recordLatency(long latencyMs) {
        if (latencyMs > 0 && latencyMs < 10000) {
            totalLatency.addAndGet(latencyMs);
            latencySamples.incrementAndGet();
        }
    }
    
    public double getPacketLossPercentage() {
        int expected = expectedPackets.get();
        if (expected == 0) {
            return 0.0;
        }
        int lost = packetsLost.get();
        return (lost * 100.0) / expected;
    }
    
    public int getPacketsLost() {
        return packetsLost.get();
    }
    
    public int getPacketsReceived() {
        return packetsReceived.get();
    }
    
    public int getPacketsSent() {
        return packetsSent.get();
    }
    
    public int getExpectedPackets() {
        return expectedPackets.get();
    }
    
    public long getAverageLatencyMs() {
        int samples = latencySamples.get();
        if (samples == 0) {
            return 0;
        }
        return totalLatency.get() / samples;
    }
    
    public long getAverageJitterMs() {
        int received = packetsReceived.get();
        if (received < 2) {
            return 0;
        }
        return totalJitter.get() / (received - 1) / 1000000;
    }
    
    public long getCallDurationSeconds() {
        if (callStartTime == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - callStartTime) / 1000;
    }
    
    public String getFormattedCallDuration() {
        long seconds = getCallDurationSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "RTP Stats - Sent: %d, Received: %d, Lost: %d (%.2f%%), Latency: %dms, Jitter: %dms, Duration: %s",
            getPacketsSent(),
            getPacketsReceived(),
            getPacketsLost(),
            getPacketLossPercentage(),
            getAverageLatencyMs(),
            getAverageJitterMs(),
            getFormattedCallDuration()
        );
    }
}

