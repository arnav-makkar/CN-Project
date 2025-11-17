// Peers SIP Softphone - GPL v3 License

package sip;

public class JavaUtils {
    
    public static String getShortClassName(Class<?> c) {
        String name = c.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }
    
}
