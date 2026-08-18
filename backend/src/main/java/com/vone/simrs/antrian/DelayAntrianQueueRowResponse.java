package com.vone.simrs.antrian;

/**
 * Satu pasien dalam antrian dokter. Migrasi dari legacy
 * {@code DoctorManagerImpl.getAntrian()} — nomor antrian = n_escort_primary_id.
 */
public class DelayAntrianQueueRowResponse {

    private final Integer registrationId;
    private final Integer number;
    private final String mrCode;
    private final String patientName;
    private final String registrationDate;

    public DelayAntrianQueueRowResponse(Integer registrationId, Integer number, String mrCode,
            String patientName, String registrationDate) {
        this.registrationId = registrationId;
        this.number = number;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.registrationDate = registrationDate;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public Integer getNumber() {
        return number;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }
}
