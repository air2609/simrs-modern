package com.vone.simrs.master.cardtype;

import java.util.List;

/**
 * Data master untuk form tipe kartu bank (SCM0048 - MASTER CARD TYPE).
 * Berisi opsi bank untuk dropdown NAMA BANK.
 */
public class CardTypeMastersResponse {

    private final List<BankOptionResponse> bankOptions;

    public CardTypeMastersResponse(List<BankOptionResponse> bankOptions) {
        this.bankOptions = bankOptions;
    }

    public List<BankOptionResponse> getBankOptions() {
        return bankOptions;
    }
}
