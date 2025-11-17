// Peers SIP Softphone - GPL v3 License

package sip.media;

public interface SoundSource {

    /**
     * read raw data linear PCM 8kHz, 16 bits signed, mono-channel, little endian
     * @return
     */
    public byte[] readData();

}
