package com.vone.simrs.master.icd9cm;

/**
 * Request simpan/edit ICD-9-CM (SCM0028). Mengikuti field yang diisi pada form
 * legacy {@code msICD9CM.zul} (kode ICD-9-CM dan nama tindakan).
 */
public class Icd9cmSaveRequest {

    private Integer id;
    private String code;
    private String name;

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
}
