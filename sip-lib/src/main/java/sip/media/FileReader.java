// Peers SIP Softphone - GPL v3 License
package sip.media;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import sip.Logger;

// To create an audio file for peers, you can use audacity:
//
// Edit > Preferences
//
// - Peripherals
//   - Channels: 1 (Mono)
// - Quality
//   - Sampling frequency of default: 8000 Hz
//   - Default Sample Format: 16 bits
//
// Validate
//
// Record audio
//
// File > Export
//
// - File name: test.raw
//
// Validate

public class FileReader implements SoundSource {

    public final static int BUFFER_SIZE = 256;

    private FileInputStream fileInputStream;
    private Logger logger;

    public FileReader(String fileName, Logger logger) {
        this.logger = logger;
        try {
            fileInputStream = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            logger.error("file not found: " + fileName, e);
        }
    }

    public synchronized void close() {
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                logger.error("io exception", e);
            }
            fileInputStream = null;
        }
    }

    @Override
    public synchronized byte[] readData() {
        if (fileInputStream == null) {
            return null;
        }
        byte buffer[] = new byte[BUFFER_SIZE];
        try {
            if (fileInputStream.read(buffer) >= 0) {
                Thread.sleep(15);
                return buffer;
            } else {
                fileInputStream.close();
                fileInputStream = null;
            }
        } catch (IOException e) {
            logger.error("io exception", e);
        } catch (InterruptedException e) {
            logger.debug("file reader interrupted");
        }
        return null;
    }

}
