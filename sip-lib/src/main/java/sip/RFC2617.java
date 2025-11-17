// Peers SIP Softphone - GPL v3 License

package sip;

public class RFC2617 {

    // SCHEMES
    
    public static final String SCHEME_DIGEST = "Digest";
    
    // PARAMETERS

    public static final String PARAM_NONCE    = "nonce";
    public static final String PARAM_OPAQUE   = "opaque";
    public static final String PARAM_REALM    = "realm";
    public static final String PARAM_RESPONSE = "response";
    public static final String PARAM_URI      = "uri";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_QOP      = "qop";
    public static final String PARAM_CNONCE   = "cnonce";
    public static final String PARAM_NC       = "nc";
    public static final String PARAM_ALGORITHM= "algorithm";
    
    // MISCELLANEOUS
    
    public static final char PARAM_SEPARATOR       = ',';
    public static final char PARAM_VALUE_SEPARATOR = '=';
    public static final char PARAM_VALUE_DELIMITER = '"';
    public static final char DIGEST_SEPARATOR      = ':';
}
