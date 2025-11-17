// Peers SIP Softphone - GPL v3 License

package sip.transaction;


public class SipListeningPoint {

    private int localPort;
    private String localTransport;
    
    public SipListeningPoint(int localPort, String localTransport) {
        super();
        this.localPort = localPort;
        this.localTransport = localTransport;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != SipListeningPoint.class) {
            return false;
        }
        SipListeningPoint other = (SipListeningPoint)obj;
        return localPort == other.localPort &&
                localTransport.equals(other.localTransport);
    }
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(':').append(localPort).append('/').append(localTransport);
        return buf.toString();
    }
    
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public int getlocalPort() {
        return localPort;
    }

    public String getlocalTransport() {
        return localTransport;
    }

}
