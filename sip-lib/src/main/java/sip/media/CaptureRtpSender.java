// Peers SIP Softphone - GPL v3 License

package sip.media;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import sip.Logger;
import sip.rtp.RFC3551;
import sip.rtp.RtpSession;
import sip.sdp.Codec;



public class CaptureRtpSender {

    public static final int PIPE_SIZE = 4096;

    private RtpSession rtpSession;
    private Capture capture;
    private Encoder encoder;
    private RtpSender rtpSender;

    public CaptureRtpSender(RtpSession rtpSession, SoundSource soundSource,
            boolean mediaDebug, Codec codec, Logger logger, String peersHome)
            throws IOException {
        super();
        this.rtpSession = rtpSession;
        // the use of PipedInputStream and PipedOutputStream in Capture,
        // Encoder and RtpSender imposes a synchronization point at the
        // end of life of those threads to a void read end dead exceptions
        CountDownLatch latch = new CountDownLatch(3);
        PipedOutputStream rawDataOutput = new PipedOutputStream();
        PipedInputStream rawDataInput;
        try {
            rawDataInput = new PipedInputStream(rawDataOutput, PIPE_SIZE);
        } catch (IOException e) {
            logger.error("input/output error", e);
            return;
        }
        
        PipedOutputStream encodedDataOutput = new PipedOutputStream();
        PipedInputStream encodedDataInput;
        try {
            encodedDataInput = new PipedInputStream(encodedDataOutput,
                    PIPE_SIZE);
        } catch (IOException e) {
            logger.error("input/output error");
            rawDataInput.close();
            return;
        }
        capture = new Capture(rawDataOutput, soundSource, logger, latch);
        switch (codec.getPayloadType()) {
        case RFC3551.PAYLOAD_TYPE_PCMU:
            encoder = new PcmuEncoder(rawDataInput, encodedDataOutput,
                    mediaDebug, logger, peersHome, latch);
            break;
        case RFC3551.PAYLOAD_TYPE_PCMA:
            encoder = new PcmaEncoder(rawDataInput, encodedDataOutput,
                    mediaDebug, logger, peersHome, latch);
            break;
        default:
            encodedDataInput.close();
            rawDataInput.close();
            throw new RuntimeException("unknown payload type");
        }
        rtpSender = new RtpSender(encodedDataInput, rtpSession, mediaDebug,
                codec, logger, peersHome, latch);
    }

    public void start() throws IOException {
        
        capture.setStopped(false);
        encoder.setStopped(false);
        rtpSender.setStopped(false);
        
        Thread captureThread = new Thread(capture,
                Capture.class.getSimpleName());
        Thread encoderThread = new Thread(encoder,
                Encoder.class.getSimpleName());
        Thread rtpSenderThread = new Thread(rtpSender,
                RtpSender.class.getSimpleName());
        
        captureThread.start();
        encoderThread.start();
        rtpSenderThread.start();
        
    }

    public void stop() {
        if (capture != null) {
            capture.setStopped(true);
        }
        if (encoder != null) {
            encoder.setStopped(true);
        }
        if (rtpSender != null) {
            rtpSender.setStopped(true);
        }
    }

    public synchronized RtpSession getRtpSession() {
        return rtpSession;
    }

    public RtpSender getRtpSender() {
        return rtpSender;
    }

    public synchronized void setMuted(boolean muted) {
        if (capture != null) {
            capture.setMuted(muted);
        }
    }

    public synchronized boolean isMuted() {
        if (capture != null) {
            return capture.isMuted();
        }
        return false;
    }

}
