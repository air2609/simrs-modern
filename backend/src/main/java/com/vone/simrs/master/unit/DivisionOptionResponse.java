package com.vone.simrs.master.unit;

/**
 * Opsi dropdown divisi untuk form unit (SCM0024).
 */
public class DivisionOptionResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public DivisionOptionResponse(Integer id, String code, String name) {
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
