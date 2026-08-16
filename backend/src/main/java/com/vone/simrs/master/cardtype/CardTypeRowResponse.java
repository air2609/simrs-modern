package com.vone.simrs.master.cardtype;

/**
 * Baris data tipe kartu bank (SCM0048 - MASTER CARD TYPE).
 * Mengikuti entity legacy {@code MsCreditCardType} (tabel ms_credit_card_type).
 */
public class CardTypeRowResponse {

    private final Integer id;
    private final Short paymentType;
    private final String paymentTypeName;
    private final Integer bankId;
    private final String bankName;
    private final Integer coaId;
    private final String coaNo;
    private final String coaName;
    private final String cardName;

    public CardTypeRowResponse(Integer id, Short paymentType, String paymentTypeName,
            Integer bankId, String bankName, Integer coaId, String coaNo, String coaName,
            String cardName) {
        this.id = id;
        this.paymentType = paymentType;
        this.paymentTypeName = paymentTypeName;
        this.bankId = bankId;
        this.bankName = bankName;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.coaName = coaName;
        this.cardName = cardName;
    }

    public Integer getId() {
        return id;
    }

    public Short getPaymentType() {
        return paymentType;
    }

    public String getPaymentTypeName() {
        return paymentTypeName;
    }

    public Integer getBankId() {
        return bankId;
    }

    public String getBankName() {
        return bankName;
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

    public String getCardName() {
        return cardName;
    }
}
