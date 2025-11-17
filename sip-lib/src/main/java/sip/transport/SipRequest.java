// Peers SIP Softphone - GPL v3 License

package sip.transport;

import sip.RFC3261;
import sip.syntaxencoding.SipURI;

public class SipRequest extends SipMessage {
    protected String method;
    protected SipURI requestUri;
    //protected String requestUri;
    
    public SipRequest(String method, SipURI requestUri) {
        super();
        this.method = method;
        this.requestUri = requestUri;
    }
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(method).append(' ').append(requestUri).append(
                ' ').append(RFC3261.DEFAULT_SIP_VERSION).append(RFC3261.CRLF);
        buf.append(super.toString());
        return buf.toString();
    }

    public String getMethod() {
        return method;
    }

    public SipURI getRequestUri() {
        return requestUri;
    }
    
}
