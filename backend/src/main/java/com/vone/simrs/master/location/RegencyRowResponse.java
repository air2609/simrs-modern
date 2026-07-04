package com.vone.simrs.master.location;

public class RegencyRowResponse {

    private final Integer regencyId;
    private final String regencyCode;
    private final String regencyName;
    private final Integer provinceId;
    private final String provinceCode;
    private final String provinceName;

    public RegencyRowResponse(
        Integer regencyId,
        String regencyCode,
        String regencyName,
        Integer provinceId,
        String provinceCode,
        String provinceName
    ) {
        this.regencyId = regencyId;
        this.regencyCode = regencyCode;
        this.regencyName = regencyName;
        this.provinceId = provinceId;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
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

    public Integer getProvinceId() {
        return provinceId;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }
}
