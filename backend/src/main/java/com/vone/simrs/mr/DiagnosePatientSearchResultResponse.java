package com.vone.simrs.mr;

/**
 * Satu baris hasil pencarian pasien terdaftar (bandbox NO. MR) pada screen
 * SC0206.
 */
public class DiagnosePatientSearchResultResponse {

    private final String mrCode;
    private final String patientName;
    private final String birthDate;
    private final String address;

    public DiagnosePatientSearchResultResponse(String mrCode, String patientName, String birthDate, String address) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.birthDate = birthDate;
        this.address = address;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }
}
