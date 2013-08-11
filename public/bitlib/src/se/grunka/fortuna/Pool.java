package se.grunka.fortuna;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Pool {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final MessageDigest poolDigest = createDigest();
    private long size = 0;

    private MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Could not initialize digest", e);
        }
    }

    public long size() {
        readLock.lock();
        try {
            return size;
        } finally {
            readLock.unlock();
        }
    }

    public void add(int source, byte[] event) {
        writeLock.lock();
        try {
            if (source < 0 || source > 255) {
                throw new IllegalArgumentException("Source needs to be in the range 0 to 255, it was " + source);
            }
            if (event.length < 1 || event.length > 32) {
                throw new IllegalArgumentException("The length of the event need to be in the range 1 to 32, it was " + event.length);
            }
            size += event.length + 2;
            poolDigest.update(new byte[]{(byte) source, (byte) event.length});
            poolDigest.update(event);
        } finally {
            writeLock.unlock();
        }
    }

    public byte[] getAndClear() {
        writeLock.lock();
        try {
            size = 0;
            return poolDigest.digest();
        } finally {
            writeLock.unlock();
        }
    }
}
