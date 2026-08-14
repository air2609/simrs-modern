package com.vone.simrs.master.treatment;

/**
 * Baris data treatment (SCM0026). Mengikuti entity legacy
 * {@code MsTreatmentFee} + {@code MsTreatment} (tabel ms_treatment_fee
 * join ms_treatment).
 */
public class TreatmentRowResponse {

    private final Integer treatmentFeeId;
    private final Integer treatmentId;
    private final String code;
    private final String name;
    private final Integer treatmentGroupId;
    private final String treatmentGroupName;
    private final Integer treatmentClassId;
    private final String treatmentClassDesc;
    private final Double hospitalFee;
    private final Double doctorFee;
    private final Double medicFee;
    private final Double totalFee;
    private final Integer coaId;
    private final String coaNo;
    private final String coaName;

    public TreatmentRowResponse(Integer treatmentFeeId, Integer treatmentId, String code, String name,
            Integer treatmentGroupId, String treatmentGroupName, Integer treatmentClassId,
            String treatmentClassDesc, Double hospitalFee, Double doctorFee, Double medicFee,
            Double totalFee, Integer coaId, String coaNo, String coaName) {
        this.treatmentFeeId = treatmentFeeId;
        this.treatmentId = treatmentId;
        this.code = code;
        this.name = name;
        this.treatmentGroupId = treatmentGroupId;
        this.treatmentGroupName = treatmentGroupName;
        this.treatmentClassId = treatmentClassId;
        this.treatmentClassDesc = treatmentClassDesc;
        this.hospitalFee = hospitalFee;
        this.doctorFee = doctorFee;
        this.medicFee = medicFee;
        this.totalFee = totalFee;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.coaName = coaName;
    }

    public Integer getTreatmentFeeId() {
        return treatmentFeeId;
    }

    public Integer getTreatmentId() {
        return treatmentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getTreatmentGroupId() {
        return treatmentGroupId;
    }

    public String getTreatmentGroupName() {
        return treatmentGroupName;
    }

    public Integer getTreatmentClassId() {
        return treatmentClassId;
    }

    public String getTreatmentClassDesc() {
        return treatmentClassDesc;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public Double getDoctorFee() {
        return doctorFee;
    }

    public Double getMedicFee() {
        return medicFee;
    }

    public Double getTotalFee() {
        return totalFee;
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
