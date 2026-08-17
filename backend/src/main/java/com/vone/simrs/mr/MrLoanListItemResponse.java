package com.vone.simrs.mr;

/**
 * DTO satu baris berkas rekam medis yang sedang dipinjam/diajukan pada screen
 * SC0082
 * (DAFTAR PEMINJAMAN BERKAS REKAM MEDIS).
 */
public class MrLoanListItemResponse {

    private final String mrCode;
    private final String patientName;
    private final String statusLabel;
    private final String unitName;

    public MrLoanListItemResponse(String mrCode, String patientName, String statusLabel, String unitName) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.statusLabel = statusLabel;
        this.unitName = unitName;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getUnitName() {
        return unitName;
    }
}
