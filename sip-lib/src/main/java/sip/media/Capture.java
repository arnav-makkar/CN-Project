// Peers SIP Softphone - GPL v3 License

package sip.media;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import sip.Logger;


public class Capture implements Runnable {
    
    public static final int SAMPLE_SIZE = 16;
    public static final int BUFFER_SIZE = SAMPLE_SIZE * 20;
    
    private PipedOutputStream rawData;
    private boolean isStopped;
    private boolean isMuted;
    private SoundSource soundSource;
    private Logger logger;
    private CountDownLatch latch;
    
    public Capture(PipedOutputStream rawData, SoundSource soundSource,
            Logger logger, CountDownLatch latch) {
        this.rawData = rawData;
        this.soundSource = soundSource;
        this.logger = logger;
        this.latch = latch;
        isStopped = false;
        isMuted = false;
    }

    public void run() {
        byte[] buffer;
        
        while (!isStopped) {
            buffer = soundSource.readData();
            try {
                if (buffer == null) {
                    break;
                }
                if (isMuted) {
                    byte[] silence = new byte[buffer.length];
                    rawData.write(silence);
                } else {
                    rawData.write(buffer);
                }
                rawData.flush();
            } catch (IOException e) {
                logger.error("input/output error", e);
                return;
            }
        }
        latch.countDown();
        if (latch.getCount() != 0) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                logger.error("interrupt exception", e);
            }
        }
    }

    public synchronized void setStopped(boolean isStopped) {
        this.isStopped = isStopped;
    }

    public synchronized void setMuted(boolean isMuted) {
        this.isMuted = isMuted;
    }

    public synchronized boolean isMuted() {
        return isMuted;
    }

}
