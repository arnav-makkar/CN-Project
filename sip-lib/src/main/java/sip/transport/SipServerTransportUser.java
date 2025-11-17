// Peers SIP Softphone - GPL v3 License

package sip.transport;

public interface SipServerTransportUser {

    public void messageReceived(SipMessage sipMessage);
}
