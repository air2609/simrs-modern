package com.vone.simrs.mr;

import java.util.List;

/**
 * Request body untuk mengubah status berkas rekam medis pada screen SC0082
 * (MR KELUAR / MR KEMBALI / BATAL PINJAM).
 */
public class MrLoanUpdateRequestBody {

    private List<String> mrCodes;
    private String action;

    public List<String> getMrCodes() {
        return mrCodes;
    }

    public void setMrCodes(List<String> mrCodes) {
        this.mrCodes = mrCodes;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
