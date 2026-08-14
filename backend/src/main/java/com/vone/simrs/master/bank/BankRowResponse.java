package com.vone.simrs.master.bank;

/**
 * Baris data bank (SCM0033). Mengikuti entity legacy {@code MsBank}
 * (tabel ms_bank).
 */
public class BankRowResponse {

    private final Integer id;
    private final String bankName;
    private final String bankAccNo;
    private final String bankAddr;
    private final String bankContactNo;
    private final String bank2ndCtcNo;
    private final Integer coaId;
    private final String coaNo;
    private final String coaName;

    public BankRowResponse(Integer id, String bankName, String bankAccNo, String bankAddr,
            String bankContactNo, String bank2ndCtcNo, Integer coaId,
            String coaNo, String coaName) {
        this.id = id;
        this.bankName = bankName;
        this.bankAccNo = bankAccNo;
        this.bankAddr = bankAddr;
        this.bankContactNo = bankContactNo;
        this.bank2ndCtcNo = bank2ndCtcNo;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.coaName = coaName;
    }

    public Integer getId() {
        return id;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankAccNo() {
        return bankAccNo;
    }

    public String getBankAddr() {
        return bankAddr;
    }

    public String getBankContactNo() {
        return bankContactNo;
    }

    public String getBank2ndCtcNo() {
        return bank2ndCtcNo;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public String getCoaNo() {
        return coaNo;
    }

    public String getCoaName() {
        return coaName;
    }
}
