// Peers SIP Softphone - GPL v3 License

package sip.syntaxencoding;

public class SipHeaderParamName {

    private String name;
    
    public SipHeaderParamName(String name) {
        this.name = name;
    }
    
    
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        String objName = ((SipHeaderParamName)obj).getName();
        if (name.equalsIgnoreCase(objName)) {
            return true;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }
    
    @Override
    public String toString() {
        return name;
    }
}
