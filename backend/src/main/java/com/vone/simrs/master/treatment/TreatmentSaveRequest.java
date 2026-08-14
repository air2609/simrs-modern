package com.vone.simrs.master.treatment;

/**
 * Request simpan/edit treatment (SCM0026). Mengikuti field yang diisi pada
 * form legacy {@code TreatmentController} (kode, nama, group, kelas tarif,
 * jasa RS, jasa dokter, jasa medik, total biaya, dan COA).
 */
public class TreatmentSaveRequest {

    private Integer treatmentFeeId;
    private Integer treatmentId;
    private String code;
    private String name;
    private Integer treatmentGroupId;
    private Integer treatmentClassId;
    private Double hospitalFee;
    private Double doctorFee;
    private Double medicFee;
    private Double totalFee;
    private Integer coaId;

    public Integer getTreatmentFeeId() {
        return treatmentFeeId;
    }

    public void setTreatmentFeeId(Integer treatmentFeeId) {
        this.treatmentFeeId = treatmentFeeId;
    }

    public Integer getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(Integer treatmentId) {
        this.treatmentId = treatmentId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTreatmentGroupId() {
        return treatmentGroupId;
    }

    public void setTreatmentGroupId(Integer treatmentGroupId) {
        this.treatmentGroupId = treatmentGroupId;
    }

    public Integer getTreatmentClassId() {
        return treatmentClassId;
    }

    public void setTreatmentClassId(Integer treatmentClassId) {
        this.treatmentClassId = treatmentClassId;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(Double doctorFee) {
        this.doctorFee = doctorFee;
    }

    public Double getMedicFee() {
        return medicFee;
    }

    public void setMedicFee(Double medicFee) {
        this.medicFee = medicFee;
    }

    public Double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(Double totalFee) {
        this.totalFee = totalFee;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }
}
