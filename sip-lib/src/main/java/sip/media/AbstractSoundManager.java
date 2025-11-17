// Peers SIP Softphone - GPL v3 License
package sip.media;

public abstract class AbstractSoundManager implements SoundSource {

    public final static String MEDIA_DIR = "media";

    public abstract void init();
    public abstract void close();
    public abstract int writeData(byte[] buffer, int offset, int length);
}
