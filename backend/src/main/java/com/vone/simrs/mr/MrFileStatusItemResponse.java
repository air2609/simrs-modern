package com.vone.simrs.mr;

/**
 * DTO satu baris berkas rekam medis pada screen SC0081 (FORM BERKAS REKAM
 * MEDIS).
 */
public class MrFileStatusItemResponse {

    private final String mrCode;
    private final String patientName;
    private final String statusLabel;
    private final String locationLabel;

    public MrFileStatusItemResponse(String mrCode, String patientName, String statusLabel, String locationLabel) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.statusLabel = statusLabel;
        this.locationLabel = locationLabel;
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

    public String getLocationLabel() {
        return locationLabel;
    }
}
