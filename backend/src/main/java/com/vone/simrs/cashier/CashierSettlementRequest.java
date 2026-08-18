package com.vone.simrs.cashier;

/**
 * Satu cara pembayaran (bank/insurance) pada tab CARA PEMBAYARAN.
 */
public class CashierSettlementRequest {

    private Integer type;          // 1=BANK, 2=INSURANCE
    private Double amount;
    private Integer bankId;
    private Integer insuranceId;
    private String accountNo;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getBankId() {
        return bankId;
    }

    public void setBankId(Integer bankId) {
        this.bankId = bankId;
    }

    public Integer getInsuranceId() {
        return insuranceId;
    }

    public void setInsuranceId(Integer insuranceId) {
        this.insuranceId = insuranceId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
}
