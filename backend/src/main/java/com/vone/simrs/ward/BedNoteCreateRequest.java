package com.vone.simrs.ward;

import java.util.List;

/**
 * Request buat nota bed (tombol BUAT NOTA).
 */
public class BedNoteCreateRequest {

    private Integer registrationId;
    private Integer unitId;
    private List<BedNoteRowRequest> rows;

    public Integer getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Integer registrationId) {
        this.registrationId = registrationId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public List<BedNoteRowRequest> getRows() {
        return rows;
    }

    public void setRows(List<BedNoteRowRequest> rows) {
        this.rows = rows;
    }
}
