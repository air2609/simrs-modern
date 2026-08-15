package com.vone.simrs.master.doctor;

/**
 * Opsi dropdown unit untuk form dokter (SCM0030).
 * Mengikuti {@code UnitManager.getMsUnitForSelect} pada tabel ms_unit.
 */
public class UnitOptionResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public UnitOptionResponse(Integer id, String code, String name) {
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
