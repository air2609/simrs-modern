package com.vone.simrs.admission;

import java.util.List;

/**
 * Detail pasien untuk form mutasi kamar (SC0002): MR + registrasi rawat inap
 * aktif terakhir + riwayat mutasi bed. Migrasi dari legacy
 * {@code MutasiKamarManagerImpl.getPatientRanapDetil()}.
 */
public class BedMutationDetailResponse {

    private final Integer mrId;
    private final String mrCode;
    private final String patientName;
    private final Integer regId;
    private final String regNo;          // v_reg_secondary_id
    private final String registrationDate;
    private final List<BedMutationHistoryResponse> history;

    public BedMutationDetailResponse(Integer mrId, String mrCode, String patientName,
            Integer regId, String regNo, String registrationDate,
            List<BedMutationHistoryResponse> history) {
        this.mrId = mrId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.regId = regId;
        this.regNo = regNo;
        this.registrationDate = registrationDate;
        this.history = history;
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

    public Integer getRegId() {
        return regId;
    }

    public String getRegNo() {
        return regNo;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public List<BedMutationHistoryResponse> getHistory() {
        return history;
    }
}
