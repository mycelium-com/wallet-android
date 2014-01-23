package se.grunka.fortuna.entropy;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;

/**
 * obtains android from the best system-provided source of randomness. usually /dev/urandom
 */
public class PlatformEntropySource implements EntropySource {

    public static final File URANDOM = new File("/dev/urandom");
    private final byte[] bytes = new byte[32];

    private static class SecureRandomHolder {
        //avoid init on linux platforms.
        //note that this is broken on unpatched Android systems, so we don't use it there.
        static final SecureRandom secureRandom = new SecureRandom();
    }

    @Override
    public void event(EventScheduler scheduler, EventAdder adder) {
        try {
            InputStream inputStream = device2Stream();
            try {
                int size = inputStream.read(bytes);
                Preconditions.checkState(size == bytes.length);
                adder.add(bytes);
                scheduler.schedule(100, TimeUnit.MILLISECONDS);
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("unexpected error while obtaining entropy: ", e);
        }
    }

    private InputStream device2Stream() throws FileNotFoundException {
        InputStream inputStream;
        if (URANDOM.exists()) {
            inputStream = new FileInputStream(URANDOM);
        } else {
            //on windows, fall back to secureRandom, of unknown quality. we are using it the right way, though
            inputStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    return SecureRandomHolder.secureRandom.nextInt();
                }
            };
        }
        return inputStream;
    }
}
