package com.vone.simrs.mr;

/**
 * Satu baris history diagnosa pasien (tab HISTORY DIAGNOSA, screen SC0206).
 */
public class DiagnoseHistoryItemResponse {

    private final String date;
    private final String unitName;
    private final String doctorName;
    private final String notes;
    private final String diagnosisNames;
    private final String labResultLabel;
    private final String receiptText;

    public DiagnoseHistoryItemResponse(String date, String unitName, String doctorName, String notes,
            String diagnosisNames, String labResultLabel, String receiptText) {
        this.date = date;
        this.unitName = unitName;
        this.doctorName = doctorName;
        this.notes = notes;
        this.diagnosisNames = diagnosisNames;
        this.labResultLabel = labResultLabel;
        this.receiptText = receiptText;
    }

    public String getDate() {
        return date;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getNotes() {
        return notes;
    }

    public String getDiagnosisNames() {
        return diagnosisNames;
    }

    public String getLabResultLabel() {
        return labResultLabel;
    }

    public String getReceiptText() {
        return receiptText;
    }
}
