package com.vone.simrs.purchasing;

/**
 * Hasil simpan BPP (SC0195). Migrasi dari legacy
 * {@code DOManagerImpl.doSaveAdd(...)} yang menampilkan pesan
 * "PEMBUATAN BPP BERHASIL! NO BPP : ...".
 */
public class DeliveryOrderResultResponse {

    private final String doCode;
    private final String status;

    public DeliveryOrderResultResponse(String doCode, String status) {
        this.doCode = doCode;
        this.status = status;
    }

    public String getDoCode() {
        return doCode;
    }

    public String getStatus() {
        return status;
    }
}
