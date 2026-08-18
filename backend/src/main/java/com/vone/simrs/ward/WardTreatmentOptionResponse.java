package com.vone.simrs.ward;

/**
 * Tindakan (ms_treatment_fee) untuk dialog TAMBAH TINDAKAN.
 */
public class WardTreatmentOptionResponse {

    private final Integer treatmentFeeId;
    private final Integer treatmentId;
    private final String code;
    private final String name;
    private final Double price;
    private final Double doctorFee;

    public WardTreatmentOptionResponse(Integer treatmentFeeId, Integer treatmentId, String code,
            String name, Double price, Double doctorFee) {
        this.treatmentFeeId = treatmentFeeId;
        this.treatmentId = treatmentId;
        this.code = code;
        this.name = name;
        this.price = price;
        this.doctorFee = doctorFee;
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

    public Double getPrice() {
        return price;
    }

    public Double getDoctorFee() {
        return doctorFee;
    }
}
