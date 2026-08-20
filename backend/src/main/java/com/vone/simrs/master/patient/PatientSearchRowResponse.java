package com.vone.simrs.master.patient;

/**
 * Baris hasil pencarian pasien (SCM0011). Migrasi legacy
 * {@code MsPatientDAO.searchPatient()}.
 */
public class PatientSearchRowResponse {

    private final String mrCode;
    private final String name;
    private final String dob;
    private final String address;

    public PatientSearchRowResponse(String mrCode, String name, String dob, String address) {
        this.mrCode = mrCode;
        this.name = name;
        this.dob = dob;
        this.address = address;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getName() {
        return name;
    }

    public String getDob() {
        return dob;
    }

    public String getAddress() {
        return address;
    }
}
