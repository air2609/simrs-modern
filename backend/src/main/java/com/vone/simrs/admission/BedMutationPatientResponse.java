package com.vone.simrs.admission;

/**
 * Hasil pencarian pasien rawat inap untuk form mutasi kamar (SC0002).
 * Migrasi dari legacy {@code MsPatientDAO.searchRanapPatient()} — hanya pasien
 * dengan registrasi rawat inap aktif (no registrasi berawalan "I").
 */
public class BedMutationPatientResponse {

    private final Integer mrId;
    private final String mrCode;
    private final String patientName;
    private final String address;

    public BedMutationPatientResponse(Integer mrId, String mrCode, String patientName,
            String address) {
        this.mrId = mrId;
        this.mrCode = mrCode;
        this.patientName = patientName;
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

    public String getAddress() {
        return address;
    }
}
