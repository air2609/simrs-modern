package com.vone.simrs.admission;

import java.util.List;

/**
 * Hasil simpan mutasi kamar (SC0002) + daftar history mutasi terbaru.
 */
public class BedMutationSaveResultResponse {

    private final boolean success;
    private final String message;
    private final List<BedMutationHistoryResponse> history;

    public BedMutationSaveResultResponse(boolean success, String message,
            List<BedMutationHistoryResponse> history) {
        this.success = success;
        this.message = message;
        this.history = history;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<BedMutationHistoryResponse> getHistory() {
        return history;
    }
}
