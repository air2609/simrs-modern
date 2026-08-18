package com.vone.simrs.cashier;

/**
 * Bank untuk pembayaran kartu kredit/debit (SC0021 tab CARA PEMBAYARAN).
 */
public class CashierBankResponse {

    private final Integer bankId;
    private final String name;

    public CashierBankResponse(Integer bankId, String name) {
        this.bankId = bankId;
        this.name = name;
    }

    public Integer getBankId() {
        return bankId;
    }

    public String getName() {
        return name;
    }
}
