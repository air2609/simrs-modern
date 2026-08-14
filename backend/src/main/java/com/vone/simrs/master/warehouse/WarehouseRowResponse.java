package com.vone.simrs.master.warehouse;

/**
 * Baris data gudang (SCM0035). Mengikuti entity legacy {@code MsWarehouse}
 * (tabel ms_warehouse).
 */
public class WarehouseRowResponse {

    private final Integer id;
    private final String whouseCode;
    private final String whouseName;
    private final String whouseLoc;
    private final Integer superiorId;
    private final String superiorName;
    private final Integer coaId;
    private final String coaNo;
    private final String coaName;

    public WarehouseRowResponse(Integer id, String whouseCode, String whouseName,
            String whouseLoc, Integer superiorId, String superiorName,
            Integer coaId, String coaNo, String coaName) {
        this.id = id;
        this.whouseCode = whouseCode;
        this.whouseName = whouseName;
        this.whouseLoc = whouseLoc;
        this.superiorId = superiorId;
        this.superiorName = superiorName;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.coaName = coaName;
    }

    public Integer getId() {
        return id;
    }

    public String getWhouseCode() {
        return whouseCode;
    }

    public String getWhouseName() {
        return whouseName;
    }

    public String getWhouseLoc() {
        return whouseLoc;
    }

    public Integer getSuperiorId() {
        return superiorId;
    }

    public String getSuperiorName() {
        return superiorName;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public String getCoaNo() {
        return coaNo;
    }

    public String getCoaName() {
        return coaName;
    }
}
