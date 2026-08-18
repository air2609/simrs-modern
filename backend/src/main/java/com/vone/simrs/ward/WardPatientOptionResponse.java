package com.vone.simrs.ward;

/**
 * Hasil pencarian pasien rawat inap (bandbox NO. MR). Migrasi dari legacy
 * {@code PatientController.searchRanapPatient()} + {@code MsPatientDAO.searchRanapPatient()}.
 */
public class WardPatientOptionResponse {

    private final Integer mrId;
    private final String mrCode;
    private final String patientName;
    private final String patientType;
    private final String address;

    public WardPatientOptionResponse(Integer mrId, String mrCode, String patientName,
            String patientType, String address) {
        this.mrId = mrId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.patientType = patientType;
        this.address = address;
    }

    public Integer getMrId() {
        return mrId;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientType() {
        return patientType;
    }

    public String getAddress() {
        return address;
    }
}
