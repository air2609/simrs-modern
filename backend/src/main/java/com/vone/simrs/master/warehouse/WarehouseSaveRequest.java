package com.vone.simrs.master.warehouse;

/**
 * Request simpan/edit gudang (SCM0035). Mengikuti field yang diisi pada form
 * legacy {@code WarehouseController} (kode, nama, lokasi, gudang utama, no.
 * coa).
 */
public class WarehouseSaveRequest {

    private Integer id;
    private String whouseCode;
    private String whouseName;
    private String whouseLoc;
    private Integer superiorId;
    private Integer coaId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getWhouseCode() {
        return whouseCode;
    }

    public void setWhouseCode(String whouseCode) {
        this.whouseCode = whouseCode;
    }

    public String getWhouseName() {
        return whouseName;
    }

    public void setWhouseName(String whouseName) {
        this.whouseName = whouseName;
    }

    public String getWhouseLoc() {
        return whouseLoc;
    }

    public void setWhouseLoc(String whouseLoc) {
        this.whouseLoc = whouseLoc;
    }

    public Integer getSuperiorId() {
        return superiorId;
    }

    public void setSuperiorId(Integer superiorId) {
        this.superiorId = superiorId;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }
}
