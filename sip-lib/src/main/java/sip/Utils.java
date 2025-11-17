// SIP Softphone - GPL v3 License

package sip;


import java.net.InetAddress;

import sip.core.useragent.UAS;
import sip.syntaxencoding.SipHeaderFieldMultiValue;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaders;
import sip.transport.SipMessage;


public class Utils {

    public static final String SIPHOME_SYSTEM_PROPERTY = "sip.home";
    public static final String DEFAULT_SIP_HOME = ".";

    public final static SipHeaderFieldValue getTopVia(SipMessage sipMessage) {
        SipHeaders sipHeaders = sipMessage.getSipHeaders();
        SipHeaderFieldName viaName = new SipHeaderFieldName(RFC3261.HDR_VIA);
        SipHeaderFieldValue via = sipHeaders.get(viaName);
        if (via instanceof SipHeaderFieldMultiValue) {
            via = ((SipHeaderFieldMultiValue)via).getValues().get(0);
        }
        return via;
    }
    
    public final static String generateTag() {
        return randomString(8);
    }
    
    public final static String generateCallID(InetAddress inetAddress) {
        
        StringBuffer buf = new StringBuffer();
        buf.append(randomString(8));
        buf.append('-');
        buf.append(String.valueOf(System.currentTimeMillis()));
        buf.append('@');
        buf.append(inetAddress.getHostName());
        return buf.toString();
    }
    
    public final static String generateBranchId() {
        StringBuffer buf = new StringBuffer();
        buf.append(RFC3261.BRANCHID_MAGIC_COOKIE);
        
        buf.append(randomString(9));
        return buf.toString();
    }
    
    public final static String getMessageCallId(SipMessage sipMessage) {
        SipHeaderFieldValue callId = sipMessage.getSipHeaders().get(
                new SipHeaderFieldName(RFC3261.HDR_CALLID));
        return callId.getValue();
    }
    
    public final static String randomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz" +
                       "ABCDEFGHIFKLMNOPRSTUVWXYZ" +
                       "0123456789";
        StringBuffer buf = new StringBuffer(length);
        for (int i = 0; i < length; ++i) {
            int pos = (int)Math.round(Math.random() * (chars.length() - 1));
            buf.append(chars.charAt(pos));
        }
        return buf.toString();
    }
    
    public final static void copyHeader(SipMessage src, SipMessage dst, String name) {
        SipHeaderFieldName sipHeaderFieldName = new SipHeaderFieldName(name);
        SipHeaderFieldValue sipHeaderFieldValue = src.getSipHeaders().get(sipHeaderFieldName);
        if (sipHeaderFieldValue != null) {
            dst.getSipHeaders().add(sipHeaderFieldName, sipHeaderFieldValue);
        }
    }
    
    public final static String getUserPart(String sipUri) {
        int start = sipUri.indexOf(RFC3261.SCHEME_SEPARATOR);
        int end = sipUri.indexOf(RFC3261.AT);
        return sipUri.substring(start + 1, end);
    }

    /**
     * adds Max-Forwards Supported and Require headers
     * @param headers
     */
    public final static void addCommonHeaders(SipHeaders headers) {
        //Max-Forwards
        
        headers.add(new SipHeaderFieldName(RFC3261.HDR_MAX_FORWARDS),
                new SipHeaderFieldValue(
                        String.valueOf(RFC3261.DEFAULT_MAXFORWARDS)));
        
        
    }

    public final static String generateAllowHeader() {
        StringBuffer buf = new StringBuffer();
        for (String supportedMethod: UAS.SUPPORTED_METHODS) {
            buf.append(supportedMethod);
            buf.append(", ");
        }
        int bufLength = buf.length();
        buf.delete(bufLength - 2, bufLength);
        return buf.toString();
    }

}
