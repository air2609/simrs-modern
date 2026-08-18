package com.vone.simrs.ward;

/**
 * Data modal INVENTORY PASIEN (list item + riwayat per item).
 */
public class WardPatientInventoryResponse {

    private final String mrCode;
    private final String patientName;
    private final Integer registrationId;
    private final String registrationNumber;
    private final java.util.List<WardPatientInventoryItemResponse> items;
    private final java.util.List<WardPatientInventoryHistoryResponse> history;

    public WardPatientInventoryResponse(String mrCode, String patientName, Integer registrationId,
            String registrationNumber, java.util.List<WardPatientInventoryItemResponse> items,
            java.util.List<WardPatientInventoryHistoryResponse> history) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.registrationId = registrationId;
        this.registrationNumber = registrationNumber;
        this.items = items;
        this.history = history;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public java.util.List<WardPatientInventoryItemResponse> getItems() {
        return items;
    }

    public java.util.List<WardPatientInventoryHistoryResponse> getHistory() {
        return history;
    }
}
