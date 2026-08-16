package com.vone.simrs.master.cardtype;

/**
 * Opsi bank untuk dropdown NAMA BANK (SCM0048).
 * Mengikuti {@code BankController.getBanks()} pada tabel ms_bank.
 */
public class BankOptionResponse {

    private final Integer id;
    private final String bankName;

    public BankOptionResponse(Integer id, String bankName) {
        this.id = id;
        this.bankName = bankName;
    }

    public Integer getId() {
        return id;
    }

    public String getBankName() {
        return bankName;
    }
}
