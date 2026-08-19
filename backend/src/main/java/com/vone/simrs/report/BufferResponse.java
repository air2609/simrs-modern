package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil buffer monitoring (RPT0018).
 */
public class BufferResponse {

    private final String unitName;
    private final List<BufferRowResponse> rows;

    public BufferResponse(String unitName, List<BufferRowResponse> rows) {
        this.unitName = unitName;
        this.rows = rows;
    }

    public String getUnitName() {
        return unitName;
    }

    public List<BufferRowResponse> getRows() {
        return rows;
    }
}
