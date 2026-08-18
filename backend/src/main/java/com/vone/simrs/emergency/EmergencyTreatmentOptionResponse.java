package com.vone.simrs.emergency;

/**
 * Tindakan (ms_treatment_fee) untuk dialog TAMBAH TINDAKAN. Migrasi dari
 * legacy {@code CommonTreatmentController.searchTreatment()} +
 * {@code MsTreatmentDAO.getSearchTreatmentByUnit()}.
 */
public class EmergencyTreatmentOptionResponse {

    private final Integer treatmentFeeId;
    private final Integer treatmentId;
    private final String code;
    private final String name;
    private final Double price;
    private final Double doctorFee;

    public EmergencyTreatmentOptionResponse(Integer treatmentFeeId, Integer treatmentId,
            String code, String name, Double price, Double doctorFee) {
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
