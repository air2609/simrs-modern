package com.vone.simrs.master.treatmentbatch;

/**
 * Baris data untuk screen SCM0056 (UPDATE MASTER TINDAKAN / batch).
 * Mengikuti kolom listbox legacy {@code BatchTreatmentController}:
 * KODE, NAMA TINDAKAN, KELAS TARIF, JASA RS, JASA DOKTER, JASA MEDIK,
 * TOTAL BIAYA, NO. COA.
 */
public class BatchTreatmentRowResponse {

    private final Integer treatmentFeeId;
    private final Integer treatmentId;
    private final String code;
    private final String name;
    private final String treatmentClassDesc;
    private final Double hospitalFee;
    private final Double doctorFee;
    private final Double medicFee;
    private final Double totalFee;
    private final String coaNo;

    public BatchTreatmentRowResponse(Integer treatmentFeeId, Integer treatmentId, String code,
            String name, String treatmentClassDesc, Double hospitalFee, Double doctorFee,
            Double medicFee, Double totalFee, String coaNo) {
        this.treatmentFeeId = treatmentFeeId;
        this.treatmentId = treatmentId;
        this.code = code;
        this.name = name;
        this.treatmentClassDesc = treatmentClassDesc;
        this.hospitalFee = hospitalFee;
        this.doctorFee = doctorFee;
        this.medicFee = medicFee;
        this.totalFee = totalFee;
        this.coaNo = coaNo;
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

    public String getCoaNo() {
        return coaNo;
    }
}
