// Peers SIP Softphone - GPL v3 License

package sip.rtp;

public interface RtpListener {

    public void receivedRtpPacket(RtpPacket rtpPacket);

}
