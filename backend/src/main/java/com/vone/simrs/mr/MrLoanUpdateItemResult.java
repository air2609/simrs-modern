package com.vone.simrs.mr;

/**
 * Hasil pemrosesan satu berkas rekam medis pada aksi MR KELUAR / MR KEMBALI /
 * BATAL PINJAM.
 * Sama persis dengan legacy
 * {@code Messagebox.show(mr.getVMrCode()+" "+message)} per item.
 */
public class MrLoanUpdateItemResult {

    private final String mrCode;
    private final boolean success;
    private final String message;

    public MrLoanUpdateItemResult(String mrCode, boolean success, String message) {
        this.mrCode = mrCode;
        this.success = success;
        this.message = message;
    }

    public String getMrCode() {
        return mrCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
