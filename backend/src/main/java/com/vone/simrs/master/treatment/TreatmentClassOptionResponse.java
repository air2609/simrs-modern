package com.vone.simrs.master.treatment;

/**
 * Opsi dropdown kelas tarif (SCM0026). Mengikuti data ms_treatment_class.
 */
public class TreatmentClassOptionResponse {

    private final Integer id;
    private final String code;
    private final String description;

    public TreatmentClassOptionResponse(Integer id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
