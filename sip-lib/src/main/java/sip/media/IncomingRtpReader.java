// Peers SIP Softphone - GPL v3 License

package sip.media;

import java.io.IOException;

import sip.Logger;
import sip.rtp.RFC3551;
import sip.rtp.RFC4733;
import sip.rtp.RtpListener;
import sip.rtp.RtpPacket;
import sip.rtp.RtpSession;
import sip.sdp.Codec;

public class IncomingRtpReader implements RtpListener {

    private RtpSession rtpSession;
    private AbstractSoundManager soundManager;
    private Decoder decoder;
    private MediaManager mediaManager;
    private Logger logger;

    public IncomingRtpReader(RtpSession rtpSession,
            AbstractSoundManager soundManager, Codec codec, Logger logger)
            throws IOException {
        this(rtpSession, soundManager, codec, null, logger);
    }

    public IncomingRtpReader(RtpSession rtpSession,
            AbstractSoundManager soundManager, Codec codec, MediaManager mediaManager, Logger logger)
            throws IOException {
        logger.debug("playback codec:" + codec.toString().trim());
        this.rtpSession = rtpSession;
        this.soundManager = soundManager;
        this.mediaManager = mediaManager;
        this.logger = logger;
        switch (codec.getPayloadType()) {
        case RFC3551.PAYLOAD_TYPE_PCMU:
            decoder = new PcmuDecoder();
            break;
        case RFC3551.PAYLOAD_TYPE_PCMA:
            decoder = new PcmaDecoder();
            break;
        default:
            throw new RuntimeException("unsupported payload type");
        }
        rtpSession.addRtpListener(this);
    }
    
    public void start() {
        rtpSession.start();
    }

    @Override
    public void receivedRtpPacket(RtpPacket rtpPacket) {
        // Check if this is a DTMF (RFC4733) packet
        if (rtpPacket.getPayloadType() == RFC4733.PAYLOAD_TYPE_TELEPHONE_EVENT) {
            handleDtmfPacket(rtpPacket);
            return;
        }
        
        // Handle audio packets
        byte[] rawBuf = decoder.process(rtpPacket.getData());
        if (soundManager != null) {
            soundManager.writeData(rawBuf, 0, rawBuf.length);
        }
    }

    private void handleDtmfPacket(RtpPacket rtpPacket) {
        byte[] data = rtpPacket.getData();
        if (data == null || data.length < 4) {
            logger.debug("Invalid DTMF packet: data too short");
            return;
        }

        byte event = data[0];
        byte volume = data[1];
        boolean endEvent = (volume & 0x80) != 0; // End event flag is bit 7 (0x80 = 128)

        // Process end event packets to notify about completed DTMF digit
        if (endEvent) {
            char digit = convertEventToDigit(event);
            if (digit != 0 && mediaManager != null) {
                mediaManager.receivedDtmf(digit);
                logger.debug("Received DTMF digit: " + digit);
            }
        } else if (rtpPacket.isMarker()) {
            // Start of new DTMF event - we'll process it when end event arrives
            logger.debug("DTMF event started: " + convertEventToDigit(event));
        }
    }

    private char convertEventToDigit(byte event) {
        if (event >= 0 && event <= 9) {
            return (char) ('0' + event);
        } else if (event == 10) {
            return '*';
        } else if (event == 11) {
            return '#';
        } else if (event >= 12 && event <= 15) {
            return (char) ('A' + (event - 12));
        }
        return 0; // Unknown event
    }

}
