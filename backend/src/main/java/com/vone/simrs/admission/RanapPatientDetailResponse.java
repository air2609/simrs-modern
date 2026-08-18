package com.vone.simrs.admission;

import java.util.List;

/**
 * Detail pasien untuk pendaftaran rawat inap: MR + registrasi rajal terakhir
 * + riwayat nota + jumlah rawat inap sebelumnya. Migrasi dari legacy
 * {@code RegistrationManagerImpl.getPatientDetil()}.
 */
public class RanapPatientDetailResponse {

    private final Integer mrId;
    private final String mrCode;
    private final String patientName;
    private final String gender;
    private final Integer oldRegId;
    private final String oldRegNo;
    private final String oldRegDate;
    private final Integer ranapCount;
    private final List<RanapHistoryResponse> history;

    public RanapPatientDetailResponse(Integer mrId, String mrCode, String patientName,
            String gender, Integer oldRegId, String oldRegNo, String oldRegDate,
            Integer ranapCount, List<RanapHistoryResponse> history) {
        this.mrId = mrId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.gender = gender;
        this.oldRegId = oldRegId;
        this.oldRegNo = oldRegNo;
        this.oldRegDate = oldRegDate;
        this.ranapCount = ranapCount;
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

    public String getGender() {
        return gender;
    }

    public Integer getOldRegId() {
        return oldRegId;
    }

    public String getOldRegNo() {
        return oldRegNo;
    }

    public String getOldRegDate() {
        return oldRegDate;
    }

    public Integer getRanapCount() {
        return ranapCount;
    }

    public List<RanapHistoryResponse> getHistory() {
        return history;
    }
}
