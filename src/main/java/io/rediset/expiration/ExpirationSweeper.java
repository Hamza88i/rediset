package io.rediset.expiration;

import io.rediset.storage.StorageEngine;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single background thread that periodically reclaims expired keys that are
 * never accessed (the active half of the lazy + active expiration strategy in
 * ADR-004). Each tick asks the {@link StorageEngine} to sample and expire a
 * bounded number of TTL-bearing keys, keeping per-tick cost predictable.
 *
 * <p>The sweeper uses one scheduled thread — never one thread per key — and is
 * started and stopped as part of the server lifecycle.
 */
public final class ExpirationSweeper {

    private static final Logger log = LoggerFactory.getLogger(ExpirationSweeper.class);

    private final StorageEngine storage;
    private final ExpirationConfig config;
    private final ScheduledExecutorService scheduler;

    private volatile boolean running;

    public ExpirationSweeper(StorageEngine storage, ExpirationConfig config) {
        this.storage = storage;
        this.config = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory());
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "rediset-expiration-sweeper");
            thread.setDaemon(true);
            return thread;
        };
    }

    /** Starts periodic sweeping. Idempotent. */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        long interval = config.sweepIntervalMillis();
        scheduler.scheduleAtFixedRate(
                this::sweepQuietly, interval, interval, TimeUnit.MILLISECONDS);
        log.debug("Expiration sweeper started (interval={}ms, sample={})",
                interval, config.sampleSize());
    }

    private void sweepQuietly() {
        try {
            int removed = storage.sweepExpired(config.sampleSize());
            if (removed > 0) {
                log.trace("Expiration sweep removed {} key(s)", removed);
            }
        } catch (RuntimeException e) {
            // A sweep failure must never kill the scheduler thread.
            log.warn("Expiration sweep failed", e);
        }
    }

    /** Stops sweeping and releases the scheduler thread. Idempotent. */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        scheduler.shutdownNow();
        log.debug("Expiration sweeper stopped");
    }

    public boolean isRunning() {
        return running;
    }
}
