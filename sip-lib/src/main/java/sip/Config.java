// Peers SIP Softphone - GPL v3 License

package sip;

import java.net.InetAddress;

import sip.media.MediaMode;
import sip.syntaxencoding.SipURI;

public interface Config {

    public void save();
    public InetAddress getLocalInetAddress();
    public InetAddress getPublicInetAddress();
    public String getUserPart();
    public String getDomain();
    public String getPassword();
    public SipURI getOutboundProxy();
    public int getSipPort();
    public MediaMode getMediaMode();
    public boolean isMediaDebug();
    public String getMediaFile();
    public int getRtpPort();
    public String getAuthorizationUsername();
    public boolean isEnableSRTP();
    public boolean isEnableTLS();
    public void setLocalInetAddress(InetAddress inetAddress);
    public void setPublicInetAddress(InetAddress inetAddress);
    public void setUserPart(String userPart);
    public void setDomain(String domain);
    public void setPassword(String password);
    public void setOutboundProxy(SipURI outboundProxy);
    public void setSipPort(int sipPort);
    public void setMediaMode(MediaMode mediaMode);
    public void setMediaDebug(boolean mediaDebug);
    public void setMediaFile(String mediaFile);
    public void setRtpPort(int rtpPort);
    public void setAuthorizationUsername(String authorizationUsername);
    public void setEnableSRTP(boolean enableSRTP);
    public void setEnableTLS(boolean enableTLS);

}
