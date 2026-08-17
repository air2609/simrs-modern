package com.vone.simrs.mr;

/**
 * DTO satu opsi ICD hasil pencarian diagnosa (screen SC0206).
 */
public class DiagnoseIcdOptionResponse {

    private final Integer icdId;
    private final String icdCode;
    private final String icdName;

    public DiagnoseIcdOptionResponse(Integer icdId, String icdCode, String icdName) {
        this.icdId = icdId;
        this.icdCode = icdCode;
        this.icdName = icdName;
    }

    public Integer getIcdId() {
        return icdId;
    }

    public String getIcdCode() {
        return icdCode;
    }

    public String getIcdName() {
        return icdName;
    }
}
