package com.vone.simrs.emergency;

/**
 * Masters untuk screen SC0061 (TRANSAKSI UGD). Migrasi dari legacy
 * {@code EmergencyController.init()} yang mengisi patient type list,
 * patient escort list, dan unit UGD.
 */
public class EmergencyMastersResponse {

    private final Integer unitId;
    private final String unitCode;
    private final String unitName;
    private final Integer warehouseId;
    private final java.util.List<EmergencyPatientTypeResponse> patientTypes;
    private final java.util.List<EmergencyEscortResponse> escorts;

    public EmergencyMastersResponse(Integer unitId, String unitCode, String unitName,
            Integer warehouseId, java.util.List<EmergencyPatientTypeResponse> patientTypes,
            java.util.List<EmergencyEscortResponse> escorts) {
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.warehouseId = warehouseId;
        this.patientTypes = patientTypes;
        this.escorts = escorts;
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

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public java.util.List<EmergencyPatientTypeResponse> getPatientTypes() {
        return patientTypes;
    }

    public java.util.List<EmergencyEscortResponse> getEscorts() {
        return escorts;
    }
}
