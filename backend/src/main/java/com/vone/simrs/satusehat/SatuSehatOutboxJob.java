package com.vone.simrs.satusehat;

/**
 * Job pada outbox SATUSEHAT.
 */
public class SatuSehatOutboxJob {

    private final long id;
    private final String jobType;
    private final String localRef;
    private final String status;
    private final int attempts;
    private final String lastError;
    private final String nextAttempt;
    private final String updatedAt;

    public SatuSehatOutboxJob(long id, String jobType, String localRef, String status, int attempts,
            String lastError, String nextAttempt, String updatedAt) {
        this.id = id;
        this.jobType = jobType;
        this.localRef = localRef;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.nextAttempt = nextAttempt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public String getJobType() {
        return jobType;
    }

    public String getLocalRef() {
        return localRef;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public String getNextAttempt() {
        return nextAttempt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
