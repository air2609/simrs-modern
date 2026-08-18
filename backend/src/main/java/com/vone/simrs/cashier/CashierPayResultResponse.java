package com.vone.simrs.cashier;

/**
 * Hasil pembayaran kasir: nomor kwitansi + saldo deposit baru.
 */
public class CashierPayResultResponse {

    private final boolean success;
    private final String message;
    private final String kwitansiCode;
    private final Double depositBalance;

    public CashierPayResultResponse(boolean success, String message, String kwitansiCode,
            Double depositBalance) {
        this.success = success;
        this.message = message;
        this.kwitansiCode = kwitansiCode;
        this.depositBalance = depositBalance;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getKwitansiCode() {
        return kwitansiCode;
    }

    public Double getDepositBalance() {
        return depositBalance;
    }
}
