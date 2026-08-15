package com.vone.simrs.accounting.coa;

/**
 * Request simpan/edit COA (SCM0046 - CHART OF ACCOUNT).
 * Mengikuti field yang diisi pada form legacy {@code CoaController}
 * (account type, account no, account name, status, balance, subaccount of).
 */
public class CoaSaveRequest {

    private Integer coaId;
    private Integer typeId;
    private String acctNo;
    private String acctName;
    private Boolean active;
    private Double balance;
    private Integer supCoaId;

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    public String getAcctName() {
        return acctName;
    }

    public void setAcctName(String acctName) {
        this.acctName = acctName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Integer getSupCoaId() {
        return supCoaId;
    }

    public void setSupCoaId(Integer supCoaId) {
        this.supCoaId = supCoaId;
    }
}
