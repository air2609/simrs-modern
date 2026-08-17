package com.vone.simrs.mr;

import java.util.List;

/**
 * Request body untuk mengajukan peminjaman berkas rekam medis (screen SC0175).
 */
public class MrBorrowRequestBody {

    private Integer unitId;
    private List<String> mrCodes;

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public List<String> getMrCodes() {
        return mrCodes;
    }

    public void setMrCodes(List<String> mrCodes) {
        this.mrCodes = mrCodes;
    }
}
