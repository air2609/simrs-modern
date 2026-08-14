package com.vone.simrs.master.treatment;

/**
 * Opsi dropdown group tindakan (SCM0026). Mengikuti data ms_treatment_group.
 */
public class TreatmentGroupOptionResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public TreatmentGroupOptionResponse(Integer id, String code, String name) {
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
