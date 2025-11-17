// Peers SIP Softphone - GPL v3 License

package sip.sdp;

import sip.media.MediaManager;

public class Codec {

    private int payloadType;
    private String name;

    public int getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(int payloadType) {
        this.payloadType = payloadType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Codec)) {
            return false;
        }
        Codec codec = (Codec)obj;
        if (codec.getName() == null) {
            return name == null;
        }
        return codec.getName().equalsIgnoreCase(name);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(RFC4566.TYPE_ATTRIBUTE).append(RFC4566.SEPARATOR);
        buf.append(RFC4566.ATTR_RTPMAP).append(RFC4566.ATTR_SEPARATOR);
        buf.append(payloadType).append(" ").append(name).append("/");
        buf.append(MediaManager.DEFAULT_CLOCK).append("\r\n");
        return buf.toString();
    }

}
