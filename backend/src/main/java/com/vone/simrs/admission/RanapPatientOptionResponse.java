package com.vone.simrs.admission;

/**
 * Hasil pencarian pasien terdaftar (dengan NIK). Migrasi dari legacy
 * {@code MsPatientDAO.searchPatientRegisteredWithNik()}.
 */
public class RanapPatientOptionResponse {

    private final Integer mrId;
    private final String mrCode;
    private final String patientName;
    private final String nik;
    private final String birthDate;
    private final String address;

    public RanapPatientOptionResponse(Integer mrId, String mrCode, String patientName, String nik,
            String birthDate, String address) {
        this.mrId = mrId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.nik = nik;
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

    public String getNik() {
        return nik;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }
}
