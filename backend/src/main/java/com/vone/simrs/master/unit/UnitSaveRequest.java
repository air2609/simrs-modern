package com.vone.simrs.master.unit;

/**
 * Request simpan/edit unit (SCM0024). Mengikuti field yang diisi pada form
 * legacy {@code unit.zul} (kode, divisi, nama unit, type unit, gudang unit,
 * dan no. coa).
 */
public class UnitSaveRequest {

    private Integer id;
    private String code;
    private String name;
    private Integer divisionId;
    private Integer unitType;
    private Integer warehouseId;
    private Integer coaId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDivisionId() {
        return divisionId;
    }

    public void setDivisionId(Integer divisionId) {
        this.divisionId = divisionId;
    }

    public Integer getUnitType() {
        return unitType;
    }

    public void setUnitType(Integer unitType) {
        this.unitType = unitType;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }
}
