package com.vone.simrs.mr;

/**
 * DTO satu baris hasil pencarian pasien/berkas rekam medis pada screen SC0175
 * (FORM PEMINJAMAN BERKAS REKAM MEDIS).
 */
public class MrBorrowSearchResultResponse {

    private final String mrCode;
    private final String patientName;
    private final String nik;
    private final String birthDate;
    private final String address;
    private final String mrStatusLabel;

    public MrBorrowSearchResultResponse(String mrCode, String patientName, String nik,
            String birthDate, String address, String mrStatusLabel) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.nik = nik;
        this.birthDate = birthDate;
        this.address = address;
        this.mrStatusLabel = mrStatusLabel;
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

    public String getMrStatusLabel() {
        return mrStatusLabel;
    }
}
