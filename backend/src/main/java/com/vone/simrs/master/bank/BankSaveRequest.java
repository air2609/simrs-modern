package com.vone.simrs.master.bank;

/**
 * Request simpan/edit bank (SCM0033). Mengikuti field yang diisi pada form
 * legacy {@code BankController} (nama, alamat, no. acct, no. coa, telp,
 * telp alternatif).
 */
public class BankSaveRequest {

    private Integer id;
    private String bankName;
    private String bankAddr;
    private String bankAccNo;
    private Integer coaId;
    private String bankContactNo;
    private String bank2ndCtcNo;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAddr() {
        return bankAddr;
    }

    public void setBankAddr(String bankAddr) {
        this.bankAddr = bankAddr;
    }

    public String getBankAccNo() {
        return bankAccNo;
    }

    public void setBankAccNo(String bankAccNo) {
        this.bankAccNo = bankAccNo;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }

    public String getBankContactNo() {
        return bankContactNo;
    }

    public void setBankContactNo(String bankContactNo) {
        this.bankContactNo = bankContactNo;
    }

    public String getBank2ndCtcNo() {
        return bank2ndCtcNo;
    }

    public void setBank2ndCtcNo(String bank2ndCtcNo) {
        this.bank2ndCtcNo = bank2ndCtcNo;
    }
}
