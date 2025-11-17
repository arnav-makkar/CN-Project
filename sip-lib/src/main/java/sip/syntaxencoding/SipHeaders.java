// Peers SIP Softphone - GPL v3 License

package sip.syntaxencoding;

import java.util.ArrayList;

import sip.RFC3261;



public class SipHeaders {
    
    private ArrayList<SipHeader> headers;
    
    public SipHeaders() {
        headers = new ArrayList<SipHeader>();
    }
    
    /**
     * 
     * @param name
     * @param value
     * @param index -1 to add at the end
     */
    public void add(SipHeaderFieldName name, SipHeaderFieldValue value, int index) {
        SipHeader header = new SipHeader(name, value);
        if (headers.contains(header)) {
            header =  headers.get(headers.indexOf(header));
            SipHeaderFieldValue oldValue = header.getValue();
            
            if (oldValue instanceof SipHeaderFieldMultiValue) {
                SipHeaderFieldMultiValue oldMultiVal = (SipHeaderFieldMultiValue) oldValue;
                oldMultiVal.getValues().add(value);
            } else {
                ArrayList<SipHeaderFieldValue> arr = new ArrayList<SipHeaderFieldValue>();
                arr.add(oldValue);
                arr.add(value);
                header.setValue(new SipHeaderFieldMultiValue(arr));
            }
        } else {
            if (index == -1) {
                headers.add(header);
            } else {
                headers.add(index, header);
            }
        }
    }
    
    public void add(SipHeaderFieldName name, SipHeaderFieldValue value) {
        add(name, value, -1);
    }
    
    public void remove(SipHeaderFieldName name) {
        headers.remove(new SipHeader(name, null));
    }
    
    public boolean contains(SipHeaderFieldName name) {
        return headers.contains(new SipHeader(name, null));
    }
    
    public SipHeaderFieldValue get(SipHeaderFieldName name) {
        int index = headers.indexOf(new SipHeader(name, null));
        if (index < 0) {
            return null;
        }
        return headers.get(index).getValue();
    }
    
    public int getCount() {
        return headers.size();
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (SipHeader header : headers) {
            buf.append(header.getName().toString());
            buf.append(": ");
            buf.append(header.getValue());
            buf.append(RFC3261.CRLF);
        }
        return buf.toString();
    }
}
