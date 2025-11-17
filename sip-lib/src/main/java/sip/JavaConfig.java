// Peers SIP Softphone - GPL v3 License

package sip;

import java.net.InetAddress;

import sip.media.MediaMode;
import sip.syntaxencoding.SipURI;

public class JavaConfig implements Config {

    private InetAddress localInetAddress;
    private InetAddress publicInetAddress;
    private String userPart;
    private String domain;
    private String password;
    private SipURI outboundProxy;
    private int sipPort;
    private MediaMode mediaMode;
    private boolean mediaDebug;
    private String mediaFile;
    private int rtpPort;
    private String authorizationUsername;
    private boolean enableSRTP;
    private boolean enableTLS;

    @Override
    public void save() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public InetAddress getLocalInetAddress() {
        return localInetAddress;
    }

    @Override
    public InetAddress getPublicInetAddress() {
        return publicInetAddress;
    }

    @Override
    public String getUserPart() {
        return userPart;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public SipURI getOutboundProxy() {
        return outboundProxy;
    }

    @Override
    public int getSipPort() {
        return sipPort;
    }

    @Override
    public MediaMode getMediaMode() {
        return mediaMode;
    }

    @Override
    public boolean isMediaDebug() {
        return mediaDebug;
    }

    @Override
    public int getRtpPort() {
        return rtpPort;
    }

    public String getAuthorizationUsername() {
        return authorizationUsername;
    }

    @Override
    public void setLocalInetAddress(InetAddress inetAddress) {
        localInetAddress = inetAddress;
    }

    @Override
    public void setPublicInetAddress(InetAddress inetAddress) {
        publicInetAddress = inetAddress;
    }

    @Override
    public void setUserPart(String userPart) {
        this.userPart = userPart;
    }

    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setOutboundProxy(SipURI outboundProxy) {
        this.outboundProxy = outboundProxy;
    }

    @Override
    public void setSipPort(int sipPort) {
        this.sipPort = sipPort;
    }

    @Override
    public void setMediaMode(MediaMode mediaMode) {
        this.mediaMode = mediaMode;
    }

    @Override
    public void setMediaDebug(boolean mediaDebug) {
        this.mediaDebug = mediaDebug;
    }

    @Override
    public void setRtpPort(int rtpPort) {
        this.rtpPort = rtpPort;
    }

    public void setAuthorizationUsername(String authorizationUsername) {
        this.authorizationUsername = authorizationUsername;
    }

    @Override
    public String getMediaFile() {
        return mediaFile;
    }

    @Override
    public void setMediaFile(String mediaFile) {
        this.mediaFile = mediaFile;
    }

    @Override
    public boolean isEnableSRTP() {
        return enableSRTP;
    }

    @Override
    public boolean isEnableTLS() {
        return enableTLS;
    }

    @Override
    public void setEnableSRTP(boolean enableSRTP) {
        this.enableSRTP = enableSRTP;
    }

    @Override
    public void setEnableTLS(boolean enableTLS) {
        this.enableTLS = enableTLS;
    }

}
