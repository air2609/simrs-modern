package com.vone.simrs.master.cardtype;

/**
 * Request simpan/ubah tipe kartu bank (SCM0048 - MASTER CARD TYPE).
 * Mengikuti field {@code MsCreditCardType}.
 */
public class CardTypeSaveRequest {

    private Integer id;
    private Short paymentType;
    private Integer bankId;
    private Integer coaId;
    private String cardName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Short getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(Short paymentType) {
        this.paymentType = paymentType;
    }

    public Integer getBankId() {
        return bankId;
    }

    public void setBankId(Integer bankId) {
        this.bankId = bankId;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }
}
