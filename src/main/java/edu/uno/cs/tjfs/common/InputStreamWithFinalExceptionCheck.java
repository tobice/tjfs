package edu.uno.cs.tjfs.common;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class InputStreamWithFinalExceptionCheck extends FilterInputStream {
    private final AtomicReference<IOException> exception = new AtomicReference<>(null);
    private final CountDownLatch complete = new CountDownLatch(1);

    public InputStreamWithFinalExceptionCheck(final InputStream stream) {
        super(stream);
    }

    @Override
    public void close() throws IOException {
        try {
            // Here I wait until somebody calls countDown(). The CountDownLatch works here as a
            // gate and we simply wait until it's opened.
            complete.await();
            final IOException e = exception.get();
            if (e != null) {
                throw e;
            }
        } catch (final InterruptedException e) {
            throw new IOException("Interrupted while waiting for synchronised closure");
        } finally {
            super.close();
        }
    }

    public void fail(final Exception e) {
        if (e == null) {
            exception.set(new IOException("Failed with unknown (null) reason"));
        } else {
            exception.set(new IOException(e.getMessage(), e));
        }
    }

    public void countDown() {
        complete.countDown();
    }
}

