package com.vone.simrs.ward;

import java.util.List;

/**
 * Masters screen SC0031: unit lokasi transaksi user.
 */
public class WardMastersResponse {

    private final List<WardUnitResponse> units;

    public WardMastersResponse(List<WardUnitResponse> units) {
        this.units = units;
    }

    public List<WardUnitResponse> getUnits() {
        return units;
    }
}
