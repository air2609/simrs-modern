package com.vone.simrs.master.insurance;

import java.time.LocalDate;

/**
 * Request simpan/edit asuransi (SCM0034). Mengikuti field yang diisi pada form
 * legacy {@code InsuranceController} (nama, alamat, telp, keterangan, no. coa,
 * akhir kontrak, status aktif).
 */
public class InsuranceSaveRequest {

    private Integer id;
    private String insuranceName;
    private String insuranceAddr;
    private String insurancePhNo;
    private String insuranceDesc;
    private Integer coaId;
    private LocalDate endOfContract;
    private boolean active;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getInsuranceName() {
        return insuranceName;
    }

    public void setInsuranceName(String insuranceName) {
        this.insuranceName = insuranceName;
    }

    public String getInsuranceAddr() {
        return insuranceAddr;
    }

    public void setInsuranceAddr(String insuranceAddr) {
        this.insuranceAddr = insuranceAddr;
    }

    public String getInsurancePhNo() {
        return insurancePhNo;
    }

    public void setInsurancePhNo(String insurancePhNo) {
        this.insurancePhNo = insurancePhNo;
    }

    public String getInsuranceDesc() {
        return insuranceDesc;
    }

    public void setInsuranceDesc(String insuranceDesc) {
        this.insuranceDesc = insuranceDesc;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }

    public LocalDate getEndOfContract() {
        return endOfContract;
    }

    public void setEndOfContract(LocalDate endOfContract) {
        this.endOfContract = endOfContract;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
