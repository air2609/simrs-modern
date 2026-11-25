package com.vone.simrs.satusehat;

/**
 * Ringkasan hasil pemrosesan outbox.
 */
public class SatuSehatOutboxProcessResult {

    private final int processed;
    private final int sent;
    private final int failed;

    public SatuSehatOutboxProcessResult(int processed, int sent, int failed) {
        this.processed = processed;
        this.sent = sent;
        this.failed = failed;
    }

    public int getProcessed() {
        return processed;
    }

    public int getSent() {
        return sent;
    }

    public int getFailed() {
        return failed;
    }
}
