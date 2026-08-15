package com.vone.simrs.master.icd;

/**
 * Baris data ICD (SCM0027). Mengikuti entity legacy {@code MsIcd}
 * (tabel ms_icd).
 */
public class IcdRowResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public IcdRowResponse(Integer id, String code, String name) {
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
