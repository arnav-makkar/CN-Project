// Peers SIP Softphone - GPL v3 License

package sip.transport;


public interface SipClientTransportUser {

    public void requestTransportError(SipRequest sipRequest, Exception e);
    public void responseTransportError(Exception e);
}
