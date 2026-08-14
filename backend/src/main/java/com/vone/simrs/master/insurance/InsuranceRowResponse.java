package com.vone.simrs.master.insurance;

import java.time.LocalDate;

/**
 * Baris data asuransi (SCM0034). Mengikuti entity legacy {@code MsInsurance}
 * (tabel ms_insurance).
 */
public class InsuranceRowResponse {

    private final Integer id;
    private final String insuranceName;
    private final String insuranceAddr;
    private final String insurancePhNo;
    private final String insuranceDesc;
    private final boolean active;
    private final LocalDate endOfContract;
    private final Integer coaId;
    private final String coaNo;
    private final String coaName;

    public InsuranceRowResponse(Integer id, String insuranceName, String insuranceAddr,
            String insurancePhNo, String insuranceDesc, boolean active,
            LocalDate endOfContract, Integer coaId, String coaNo, String coaName) {
        this.id = id;
        this.insuranceName = insuranceName;
        this.insuranceAddr = insuranceAddr;
        this.insurancePhNo = insurancePhNo;
        this.insuranceDesc = insuranceDesc;
        this.active = active;
        this.endOfContract = endOfContract;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.coaName = coaName;
    }

    public Integer getId() {
        return id;
    }

    public String getInsuranceName() {
        return insuranceName;
    }

    public String getInsuranceAddr() {
        return insuranceAddr;
    }

    public String getInsurancePhNo() {
        return insurancePhNo;
    }

    public String getInsuranceDesc() {
        return insuranceDesc;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getEndOfContract() {
        return endOfContract;
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
