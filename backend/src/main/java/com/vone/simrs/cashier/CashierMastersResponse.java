package com.vone.simrs.cashier;

import com.vone.simrs.ward.WardUnitResponse;
import java.util.List;

/**
 * Masters screen SC0021: unit lokasi (kasir) + bank + asuransi.
 */
public class CashierMastersResponse {

    private final List<WardUnitResponse> units;
    private final List<CashierBankResponse> banks;
    private final List<CashierInsuranceResponse> insurances;

    public CashierMastersResponse(List<WardUnitResponse> units, List<CashierBankResponse> banks,
            List<CashierInsuranceResponse> insurances) {
        this.units = units;
        this.banks = banks;
        this.insurances = insurances;
    }

    public List<WardUnitResponse> getUnits() {
        return units;
    }

    public List<CashierBankResponse> getBanks() {
        return banks;
    }

    public List<CashierInsuranceResponse> getInsurances() {
        return insurances;
    }
}
