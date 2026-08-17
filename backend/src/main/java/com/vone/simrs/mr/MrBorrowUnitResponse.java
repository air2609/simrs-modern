package com.vone.simrs.mr;

/**
 * DTO unit lokasi (LOKASI) untuk screen SC0175 (FORM PEMINJAMAN BERKAS REKAM
 * MEDIS).
 */
public class MrBorrowUnitResponse {

    private final Integer unitId;
    private final String unitCode;
    private final String unitName;

    public MrBorrowUnitResponse(Integer unitId, String unitCode, String unitName) {
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }
}
