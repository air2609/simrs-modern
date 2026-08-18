package com.vone.simrs.cashier;

/**
 * Perusahaan asuransi (di tanggung perusahaan/asuransi).
 */
public class CashierInsuranceResponse {

    private final Integer insuranceId;
    private final String name;

    public CashierInsuranceResponse(Integer insuranceId, String name) {
        this.insuranceId = insuranceId;
        this.name = name;
    }

    public Integer getInsuranceId() {
        return insuranceId;
    }

    public String getName() {
        return name;
    }
}
