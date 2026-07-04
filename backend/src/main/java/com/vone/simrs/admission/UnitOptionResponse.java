package com.vone.simrs.admission;

public class UnitOptionResponse {

    private final Integer unitId;
    private final Integer divisionId;
    private final String unitCode;
    private final String unitName;
    private final String divisionName;
    private final Integer registrationCharge;

    public UnitOptionResponse(
        Integer unitId,
        Integer divisionId,
        String unitCode,
        String unitName,
        String divisionName,
        Integer registrationCharge
    ) {
        this.unitId = unitId;
        this.divisionId = divisionId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.divisionName = divisionName;
        this.registrationCharge = registrationCharge;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public Integer getDivisionId() {
        return divisionId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getDivisionName() {
        return divisionName;
    }

    public Integer getRegistrationCharge() {
        return registrationCharge;
    }
}
