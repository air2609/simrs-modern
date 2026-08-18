package com.vone.simrs.admission;

/**
 * Bed pada ruangan (penempatan bed). Migrasi dari legacy
 * {@code RegistrationManagerImpl.getBedBaseOnHallId()}.
 */
public class RanapBedResponse {

    private final Integer bedId;
    private final String bedDesc;
    private final String status;          // "0" kosong, "1" terpakai
    private final String availableStatus; // null/"A" tersedia, "B" dipesan, "C" perbaikan
    private final String label;
    private final String mrCode;
    private final String patientName;

    public RanapBedResponse(Integer bedId, String bedDesc, String status, String availableStatus,
            String label, String mrCode, String patientName) {
        this.bedId = bedId;
        this.bedDesc = bedDesc;
        this.status = status;
        this.availableStatus = availableStatus;
        this.label = label;
        this.mrCode = mrCode;
        this.patientName = patientName;
    }

    public Integer getBedId() {
        return bedId;
    }

    public String getBedDesc() {
        return bedDesc;
    }

    public String getStatus() {
        return status;
    }

    public String getAvailableStatus() {
        return availableStatus;
    }

    public String getLabel() {
        return label;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }
}
