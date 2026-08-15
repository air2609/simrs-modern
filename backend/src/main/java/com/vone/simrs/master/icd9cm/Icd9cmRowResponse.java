package com.vone.simrs.master.icd9cm;

/**
 * Baris data ICD-9-CM (SCM0028). Mengikuti entity legacy {@code MsIcd9cm}
 * (tabel ms_icd_9cm).
 */
public class Icd9cmRowResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public Icd9cmRowResponse(Integer id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
