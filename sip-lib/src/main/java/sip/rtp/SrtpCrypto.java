// Peers SIP Softphone - GPL v3 License

package sip.rtp;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

public class SrtpCrypto {
    
    private static final int SRTP_AUTH_TAG_LENGTH = 10;
    private static final int SRTP_SALT_LENGTH = 14;
    
    private byte[] masterKey;
    private byte[] masterSalt;
    private boolean enabled;
    
    private BlockCipher cipher;
    private Mac hmac;
    
    public SrtpCrypto(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            try {
                this.masterKey = generateRandomKey(16);
                this.masterSalt = generateRandomKey(SRTP_SALT_LENGTH);
                
                cipher = new SICBlockCipher(new AESEngine());
                hmac = Mac.getInstance("HmacSHA1");
                hmac.init(new SecretKeySpec(masterKey, "HmacSHA1"));
            } catch (Exception e) {
                this.enabled = false;
            }
        }
    }
    
    public SrtpCrypto(byte[] masterKey, byte[] masterSalt) {
        this.masterKey = masterKey;
        this.masterSalt = masterSalt;
        this.enabled = (masterKey != null && masterSalt != null);
        
        if (enabled) {
            try {
                cipher = new SICBlockCipher(new AESEngine());
                hmac = Mac.getInstance("HmacSHA1");
                hmac.init(new SecretKeySpec(masterKey, "HmacSHA1"));
            } catch (Exception e) {
                this.enabled = false;
            }
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public byte[] getMasterKey() {
        return masterKey;
    }
    
    public byte[] getMasterSalt() {
        return masterSalt;
    }
    
    public byte[] encrypt(byte[] rtpPacket) {
        if (!enabled || rtpPacket == null || rtpPacket.length < 12) {
            return rtpPacket;
        }
        
        try {
            byte[] header = Arrays.copyOfRange(rtpPacket, 0, 12);
            byte[] payload = Arrays.copyOfRange(rtpPacket, 12, rtpPacket.length);
            
            byte[] iv = deriveIV(header);
            
            KeyParameter keyParam = new KeyParameter(masterKey);
            ParametersWithIV params = new ParametersWithIV(keyParam, iv);
            cipher.init(true, params);
            
            byte[] encryptedPayload = new byte[payload.length];
            int offset = 0;
            while (offset < payload.length) {
                int blockSize = Math.min(16, payload.length - offset);
                cipher.processBlock(payload, offset, encryptedPayload, offset);
                offset += blockSize;
            }
            
            byte[] authTag = computeAuthTag(header, encryptedPayload);
            
            byte[] srtpPacket = new byte[header.length + encryptedPayload.length + SRTP_AUTH_TAG_LENGTH];
            System.arraycopy(header, 0, srtpPacket, 0, header.length);
            System.arraycopy(encryptedPayload, 0, srtpPacket, header.length, encryptedPayload.length);
            System.arraycopy(authTag, 0, srtpPacket, header.length + encryptedPayload.length, SRTP_AUTH_TAG_LENGTH);
            
            return srtpPacket;
        } catch (Exception e) {
            return rtpPacket;
        }
    }
    
    public byte[] decrypt(byte[] srtpPacket) {
        if (!enabled || srtpPacket == null || srtpPacket.length < 12 + SRTP_AUTH_TAG_LENGTH) {
            return srtpPacket;
        }
        
        try {
            int payloadLength = srtpPacket.length - 12 - SRTP_AUTH_TAG_LENGTH;
            
            byte[] header = Arrays.copyOfRange(srtpPacket, 0, 12);
            byte[] encryptedPayload = Arrays.copyOfRange(srtpPacket, 12, 12 + payloadLength);
            byte[] receivedAuthTag = Arrays.copyOfRange(srtpPacket, 12 + payloadLength, srtpPacket.length);
            
            byte[] computedAuthTag = computeAuthTag(header, encryptedPayload);
            if (!Arrays.equals(receivedAuthTag, Arrays.copyOfRange(computedAuthTag, 0, SRTP_AUTH_TAG_LENGTH))) {
                return null;
            }
            
            byte[] iv = deriveIV(header);
            
            KeyParameter keyParam = new KeyParameter(masterKey);
            ParametersWithIV params = new ParametersWithIV(keyParam, iv);
            cipher.init(false, params);
            
            byte[] payload = new byte[encryptedPayload.length];
            int offset = 0;
            while (offset < encryptedPayload.length) {
                int blockSize = Math.min(16, encryptedPayload.length - offset);
                cipher.processBlock(encryptedPayload, offset, payload, offset);
                offset += blockSize;
            }
            
            byte[] rtpPacket = new byte[header.length + payload.length];
            System.arraycopy(header, 0, rtpPacket, 0, header.length);
            System.arraycopy(payload, 0, rtpPacket, header.length, payload.length);
            
            return rtpPacket;
        } catch (Exception e) {
            return null;
        }
    }
    
    private byte[] deriveIV(byte[] rtpHeader) {
        byte[] iv = new byte[16];
        System.arraycopy(masterSalt, 0, iv, 0, Math.min(masterSalt.length, 14));
        
        long ssrc = ((rtpHeader[8] & 0xFFL) << 24) |
                    ((rtpHeader[9] & 0xFFL) << 16) |
                    ((rtpHeader[10] & 0xFFL) << 8) |
                    (rtpHeader[11] & 0xFFL);
        
        int seqNum = ((rtpHeader[2] & 0xFF) << 8) | (rtpHeader[3] & 0xFF);
        
        iv[4] ^= (byte)((ssrc >> 24) & 0xFF);
        iv[5] ^= (byte)((ssrc >> 16) & 0xFF);
        iv[6] ^= (byte)((ssrc >> 8) & 0xFF);
        iv[7] ^= (byte)(ssrc & 0xFF);
        
        iv[14] ^= (byte)((seqNum >> 8) & 0xFF);
        iv[15] ^= (byte)(seqNum & 0xFF);
        
        return iv;
    }
    
    private byte[] computeAuthTag(byte[] header, byte[] payload) {
        try {
            hmac.reset();
            hmac.update(header);
            hmac.update(payload);
            return hmac.doFinal();
        } catch (Exception e) {
            return new byte[SRTP_AUTH_TAG_LENGTH];
        }
    }
    
    private byte[] generateRandomKey(int length) {
        byte[] key = new byte[length];
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(String.valueOf(System.currentTimeMillis()).getBytes());
            System.arraycopy(hash, 0, key, 0, Math.min(length, hash.length));
        } catch (Exception e) {
            for (int i = 0; i < length; i++) {
                key[i] = (byte) (Math.random() * 256);
            }
        }
        return key;
    }
}

