package com.vone.simrs.cashier;

/**
 * Request setoran DEPOSIT / RETUR-DEPOSIT pasien rawat inap.
 */
public class CashierDepositRequest {

    private Integer registrationId;
    private Integer unitId;
    private String nameOnBill;
    private String addrOnBill;
    private Double amount;

    public Integer getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Integer registrationId) {
        this.registrationId = registrationId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public String getNameOnBill() {
        return nameOnBill;
    }

    public void setNameOnBill(String nameOnBill) {
        this.nameOnBill = nameOnBill;
    }

    public String getAddrOnBill() {
        return addrOnBill;
    }

    public void setAddrOnBill(String addrOnBill) {
        this.addrOnBill = addrOnBill;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
