// Peers SIP Softphone - GPL v3 License

package sip.syntaxencoding;

import sip.RFC3261;

public class NameAddress {

    public static String nameAddressToUri(String nameAddress) {
        int leftPos = nameAddress.indexOf(RFC3261.LEFT_ANGLE_BRACKET);
        int rightPos = nameAddress.indexOf(RFC3261.RIGHT_ANGLE_BRACKET);
        if (leftPos < 0 || rightPos < 0) {
            return nameAddress;
        }
        return nameAddress.substring(leftPos + 1, rightPos);
    }
    
    protected String addrSpec;
    protected String displayName;

    public NameAddress(String addrSpec) {
        super();
        this.addrSpec = addrSpec;
    }

    public NameAddress(String addrSpec, String displayName) {
        super();
        this.addrSpec = addrSpec;
        this.displayName = displayName;
    }
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (displayName != null) {
            buf.append(displayName);
            buf.append(' ');
        }
        buf.append(RFC3261.LEFT_ANGLE_BRACKET);
        buf.append(addrSpec);
        buf.append(RFC3261.RIGHT_ANGLE_BRACKET);
        return buf.toString();
    }

    public String getAddrSpec() {
        return addrSpec;
    }
    
}
