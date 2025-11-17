// Peers SIP Softphone - GPL v3 License

package sip.sdp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import sip.Config;
import sip.Logger;
import sip.rtp.RFC3551;
import sip.rtp.RFC4733;
import sip.core.useragent.UserAgent;

public class SDPManager {
    
    private SdpParser sdpParser;
    private UserAgent userAgent;
    private List<Codec> supportedCodecs;
    private Random random;

    private Logger logger;
    
    public SDPManager(UserAgent userAgent, Logger logger) {
        this.userAgent = userAgent;
        this.logger = logger;
        sdpParser = new SdpParser();
        supportedCodecs = new ArrayList<Codec>();
        random = new Random();
        
        Codec codec = new Codec();
        codec.setPayloadType(RFC3551.PAYLOAD_TYPE_PCMU);
        codec.setName(RFC3551.PCMU);
        supportedCodecs.add(codec);
        codec = new Codec();
        codec.setPayloadType(RFC3551.PAYLOAD_TYPE_PCMA);
        codec.setName(RFC3551.PCMA);
        supportedCodecs.add(codec);
        codec = new Codec();
        codec.setPayloadType(RFC4733.PAYLOAD_TYPE_TELEPHONE_EVENT);
        codec.setName(RFC4733.TELEPHONE_EVENT);
        
        supportedCodecs.add(codec);
    }
    
    public SessionDescription parse(byte[] sdp) {
        try {
            return sdpParser.parse(sdp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public MediaDestination getMediaDestination(
            SessionDescription sessionDescription) throws NoCodecException {
        InetAddress destAddress = sessionDescription.getIpAddress();
        List<MediaDescription> mediaDescriptions = sessionDescription.getMediaDescriptions();
        for (MediaDescription mediaDescription: mediaDescriptions) {
            if (RFC4566.MEDIA_AUDIO.equals(mediaDescription.getType())) {
                for (Codec offerCodec: mediaDescription.getCodecs()) {
                    if (supportedCodecs.contains(offerCodec)) {
                        String offerCodecName = offerCodec.getName();
                        if (offerCodecName.equalsIgnoreCase(RFC3551.PCMU) ||
                                offerCodecName.equalsIgnoreCase(RFC3551.PCMA)) {
                            int destPort = mediaDescription.getPort();
                            if (mediaDescription.getIpAddress() != null) {
                                destAddress = mediaDescription.getIpAddress();
                            }
                            MediaDestination mediaDestination =
                                new MediaDestination();
                            mediaDestination.setDestination(
                                    destAddress.getHostAddress());
                            mediaDestination.setPort(destPort);
                            mediaDestination.setCodec(offerCodec);
                            return mediaDestination;
                        }
                    }
                }
            }
        }
        throw new NoCodecException();
    }

    public SessionDescription createSessionDescription(SessionDescription offer,
            int localRtpPort)
            throws IOException {
        SessionDescription sessionDescription = new SessionDescription();
        sessionDescription.setUsername("user1");
        sessionDescription.setId(random.nextInt(Integer.MAX_VALUE));
        sessionDescription.setVersion(random.nextInt(Integer.MAX_VALUE));
        Config config = userAgent.getConfig();
        InetAddress inetAddress = config.getPublicInetAddress();
        if (inetAddress == null) {
            inetAddress = config.getLocalInetAddress();
        }
        sessionDescription.setIpAddress(inetAddress);
        sessionDescription.setName("-");
        sessionDescription.setAttributes(new Hashtable<String, String>());
        List<Codec> codecs;
        if (offer == null) {
            codecs = supportedCodecs;
        } else {
            codecs = new ArrayList<Codec>();
            for (MediaDescription mediaDescription:
                    offer.getMediaDescriptions()) {
                if (RFC4566.MEDIA_AUDIO.equals(mediaDescription.getType())) {
                    for (Codec codec: mediaDescription.getCodecs()) {
                        if (supportedCodecs.contains(codec)) {
                            codecs.add(codec);
                        }
                    }
                }
            }
        }
        MediaDescription mediaDescription = new MediaDescription();
        Hashtable<String, String> attributes = new Hashtable<String, String>();
        attributes.put(RFC4566.ATTR_SENDRECV, "");
        mediaDescription.setAttributes(attributes);
        mediaDescription.setType(RFC4566.MEDIA_AUDIO);
        mediaDescription.setPort(localRtpPort);
        mediaDescription.setCodecs(codecs);
        List<MediaDescription> mediaDescriptions =
            new ArrayList<MediaDescription>();
        mediaDescriptions.add(mediaDescription);
        sessionDescription.setMediaDescriptions(mediaDescriptions);
        return sessionDescription;
    }

}
