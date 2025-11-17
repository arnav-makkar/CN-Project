// Peers SIP Softphone - GPL v3 License

package sip.sdp;

public class MediaDestination {

    private String destination;
    private int port;
    private Codec codec;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Codec getCodec() {
        return codec;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

}
