package com.vone.simrs.antrian.apotikdisplay;

/**
 * DTO untuk satu baris pada screen RPT0020 (papan display OBAT PASIEN SUDAH
 * JADI).
 */
public class AntrianApotikDisplayItemResponse {

    private final String patientName;
    private final String mrCode;
    private final String drugType;

    public AntrianApotikDisplayItemResponse(String patientName, String mrCode, String drugType) {
        this.patientName = patientName;
        this.mrCode = mrCode;
        this.drugType = drugType;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getDrugType() {
        return drugType;
    }
}
