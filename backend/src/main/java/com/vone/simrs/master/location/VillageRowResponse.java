package com.vone.simrs.master.location;

public class VillageRowResponse {

    private final Integer villageId;
    private final String villageCode;
    private final String villageName;
    private final Integer districtId;
    private final String districtCode;
    private final String districtName;

    public VillageRowResponse(
        Integer villageId,
        String villageCode,
        String villageName,
        Integer districtId,
        String districtCode,
        String districtName
    ) {
        this.villageId = villageId;
        this.villageCode = villageCode;
        this.villageName = villageName;
        this.districtId = districtId;
        this.districtCode = districtCode;
        this.districtName = districtName;
    }

    public Integer getVillageId() {
        return villageId;
    }

    public String getVillageCode() {
        return villageCode;
    }

    public String getVillageName() {
        return villageName;
    }

    public Integer getDistrictId() {
        return districtId;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public String getDistrictName() {
        return districtName;
    }
}
