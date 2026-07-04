package com.vone.simrs.master.location;

public class ProvinceRowResponse {

    private final Integer provinceId;
    private final String provinceCode;
    private final String provinceName;

    public ProvinceRowResponse(Integer provinceId, String provinceCode, String provinceName) {
        this.provinceId = provinceId;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
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
