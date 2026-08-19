package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan persediaan obat-bahan medis (RPT0008).
 */
public class LaporanPersediaanResponse {

    private final String unitName;
    private final Integer warehouseId;
    /** Label periode (nama bulan + tahun berjalan), sesuai legacy. */
    private final String periodeLabel;
    private final List<LaporanPersediaanRowResponse> rows;

    public LaporanPersediaanResponse(String unitName, Integer warehouseId, String periodeLabel,
            List<LaporanPersediaanRowResponse> rows) {
        this.unitName = unitName;
        this.warehouseId = warehouseId;
        this.periodeLabel = periodeLabel;
        this.rows = rows;
    }

    public String getUnitName() {
        return unitName;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public String getPeriodeLabel() {
        return periodeLabel;
    }

    public List<LaporanPersediaanRowResponse> getRows() {
        return rows;
    }
}
