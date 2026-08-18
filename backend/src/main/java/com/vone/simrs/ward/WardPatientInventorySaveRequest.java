package com.vone.simrs.ward;

import java.util.List;

/**
 * Request simpan inventory pasien (pemakaian obat pasien ranap).
 */
public class WardPatientInventorySaveRequest {

    private Integer registrationId;
    private List<WardPatientInventoryLineRequest> lines;

    public Integer getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Integer registrationId) {
        this.registrationId = registrationId;
    }

    public List<WardPatientInventoryLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<WardPatientInventoryLineRequest> lines) {
        this.lines = lines;
    }
}
