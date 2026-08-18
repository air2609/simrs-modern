package com.vone.simrs.emergency;

/**
 * Hasil pencarian pasien yang memiliki NO. MR. Migrasi dari legacy
 * {@code PatientManagerImpl.cariPasienYgPunyaMr()} +
 * {@code MsPatientDAO.searchPatient()}.
 */
public class EmergencyPatientOptionResponse {

    private final Integer mrId;
    private final String mrCode;
    private final String patientName;
    private final String birthDate;
    private final String address;

    public EmergencyPatientOptionResponse(Integer mrId, String mrCode, String patientName,
            String birthDate, String address) {
        this.mrId = mrId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.birthDate = birthDate;
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

    public String getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }
}
