package com.vone.simrs.mr;

import java.util.List;

/**
 * Response untuk aksi ubah status berkas rekam medis pada screen SC0082.
 */
public class MrLoanUpdateResultResponse {

    private final List<MrLoanUpdateItemResult> results;

    public MrLoanUpdateResultResponse(List<MrLoanUpdateItemResult> results) {
        this.results = results;
    }

    public List<MrLoanUpdateItemResult> getResults() {
        return results;
    }
}
