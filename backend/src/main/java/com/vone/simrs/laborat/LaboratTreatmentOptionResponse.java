package com.vone.simrs.laborat;

public class LaboratTreatmentOptionResponse {
    private final Integer treatmentId;
    private final String code;
    private final String name;
    private final double tariff;

    public LaboratTreatmentOptionResponse(Integer treatmentId, String code, String name, double tariff) {
        this.treatmentId = treatmentId;
        this.code = code;
        this.name = name;
        this.tariff = tariff;
    }

    public Integer getTreatmentId() { return treatmentId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public double getTariff() { return tariff; }
}
