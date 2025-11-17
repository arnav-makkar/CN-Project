// Peers SIP Softphone - GPL v3 License

package sip.core.useragent;

import sip.transport.SipMessage;

public class SipEvent {

    public enum EventType {
        ERROR, RINGING, INCOMING_CALL, CALLEE_PICKUP;
    }

    private EventType eventType;
    private SipMessage sipMessage;

    public SipEvent(EventType type, SipMessage sipMessage) {
        this.eventType = type;
        this.sipMessage = sipMessage;
    }

    public SipMessage getSipMessage() {
        return sipMessage;
    }

    public EventType getEventType() {
        return eventType;
    }

}
