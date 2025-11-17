// Peers SIP Softphone - GPL v3 License

package sip.sdp;

public class SdpLine {
    private char type;
    private String value;
    public char getType() {
        return type;
    }
    public void setType(char type) {
        this.type = type;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    
}
