package com.vone.simrs.master.location;

public class DistrictRowResponse {

    private final Integer districtId;
    private final String districtCode;
    private final String districtName;
    private final Integer regencyId;
    private final String regencyCode;
    private final String regencyName;

    public DistrictRowResponse(
        Integer districtId,
        String districtCode,
        String districtName,
        Integer regencyId,
        String regencyCode,
        String regencyName
    ) {
        this.districtId = districtId;
        this.districtCode = districtCode;
        this.districtName = districtName;
        this.regencyId = regencyId;
        this.regencyCode = regencyCode;
        this.regencyName = regencyName;
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

    public Integer getRegencyId() {
        return regencyId;
    }

    public String getRegencyCode() {
        return regencyCode;
    }

    public String getRegencyName() {
        return regencyName;
    }
}
