// Peers SIP Softphone - GPL v3 License

package sip.core.useragent;

import sip.transport.SipMessage;

public interface MessageInterceptor {

    public void postProcess(SipMessage sipMessage);
}
