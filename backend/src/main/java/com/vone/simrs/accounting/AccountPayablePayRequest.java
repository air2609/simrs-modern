package com.vone.simrs.accounting;

/**
 * Request pembayaran hutang (SC0196). Migrasi dari legacy
 * {@code AccountPayableController.pembayaranClick()} + modal
 * {@code journalPayment.zul}.
 */
public class AccountPayablePayRequest {

    private Integer apId;
    private Integer viaCoaId;
    private Double total;
    private String memo;

    public Integer getApId() {
        return apId;
    }

    public void setApId(Integer apId) {
        this.apId = apId;
    }

    public Integer getViaCoaId() {
        return viaCoaId;
    }

    public void setViaCoaId(Integer viaCoaId) {
        this.viaCoaId = viaCoaId;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
