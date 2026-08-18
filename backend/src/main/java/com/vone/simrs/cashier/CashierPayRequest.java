package com.vone.simrs.cashier;

import java.util.List;

/**
 * Request pembayaran pelunasan nota (transaksi kasir).
 */
public class CashierPayRequest {

    private Integer registrationId;
    private Integer unitId;
    private String nameOnBill;
    private String addrOnBill;
    private Double ppn;
    private Double discount;
    private String discountType;
    private Double cash;
    private Double deposit;
    private List<Integer> noteIds;
    private List<CashierSettlementRequest> settlements;

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

    public Double getPpn() {
        return ppn;
    }

    public void setPpn(Double ppn) {
        this.ppn = ppn;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public Double getCash() {
        return cash;
    }

    public void setCash(Double cash) {
        this.cash = cash;
    }

    public Double getDeposit() {
        return deposit;
    }

    public void setDeposit(Double deposit) {
        this.deposit = deposit;
    }

    public List<Integer> getNoteIds() {
        return noteIds;
    }

    public void setNoteIds(List<Integer> noteIds) {
        this.noteIds = noteIds;
    }

    public List<CashierSettlementRequest> getSettlements() {
        return settlements;
    }

    public void setSettlements(List<CashierSettlementRequest> settlements) {
        this.settlements = settlements;
    }
}
