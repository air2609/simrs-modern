package com.vone.simrs.admission;

/**
 * Baris hasil pencarian pasien rawat inap. Migrasi dari legacy
 * {@code PatientManagerImpl.searchPatient(...)} + {@code MsPatientDAO.searchRanapPatient()}
 * dengan kolom NO. MR, NAMA, TIPE PASIEN, ALAMAT, RUANGAN, BED, DOKTER, DURASI.
 */
public class CariPasienPatientResponse {

    private final String mrCode;
    private final String patientName;
    private final Integer patientTypeId;
    private final String patientType;
    private final String address;
    private final String hall;
    private final String bed;
    private final String doctor;
    private final Integer duration;

    public CariPasienPatientResponse(String mrCode, String patientName, Integer patientTypeId,
            String patientType, String address, String hall, String bed, String doctor,
            Integer duration) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.patientTypeId = patientTypeId;
        this.patientType = patientType;
        this.address = address;
        this.hall = hall;
        this.bed = bed;
        this.doctor = doctor;
        this.duration = duration;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getPatientType() {
        return patientType;
    }

    public String getAddress() {
        return address;
    }

    public String getHall() {
        return hall;
    }

    public String getBed() {
        return bed;
    }

    public String getDoctor() {
        return doctor;
    }

    public Integer getDuration() {
        return duration;
    }
}
