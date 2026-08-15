package com.vone.simrs.master.icd;

/**
 * Request simpan/edit ICD (SCM0027). Mengikuti field yang diisi pada form
 * legacy {@code msICD.zul} (kode ICD dan nama penyakit).
 */
public class IcdSaveRequest {

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
