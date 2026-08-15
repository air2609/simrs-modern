package com.vone.simrs.master.division;

/**
 * Baris data divisi (SCM0022). Mengikuti entity legacy
 * {@code MsDivision} (tabel ms_division).
 */
public class DivisionRowResponse {

    private final Integer id;
    private final String code;
    private final String name;
    private final String registrationUnit;
    private final Integer registrationCharge;

    public DivisionRowResponse(Integer id, String code, String name,
            String registrationUnit, Integer registrationCharge) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.registrationUnit = registrationUnit;
        this.registrationCharge = registrationCharge;
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

    public String getRegistrationUnit() {
        return registrationUnit;
    }

    public Integer getRegistrationCharge() {
        return registrationCharge;
    }
}
