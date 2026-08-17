package com.vone.simrs.mr;

import java.util.List;

/**
 * Hasil pengajuan peminjaman berkas rekam medis (screen SC0175).
 * Sama persis dengan legacy {@code MRController.requestMR()}: berkas dengan
 * status selain TERSEDIA tidak bisa diajukan dan dilaporkan pada
 * {@code failedCodes}.
 */
public class MrBorrowRequestResultResponse {

    private final int requestedCount;
    private final List<String> failedCodes;

    public MrBorrowRequestResultResponse(int requestedCount, List<String> failedCodes) {
        this.requestedCount = requestedCount;
        this.failedCodes = failedCodes;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public List<String> getFailedCodes() {
        return failedCodes;
    }
}
