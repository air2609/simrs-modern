package com.vone.simrs.master.treatmentclass;

/**
 * Baris data kelas tarif (SCM0021). Mengikuti entity legacy
 * {@code MsTreatmentClass} (tabel ms_treatment_class).
 */
public class TreatmentClassRowResponse {

    private final Integer id;
    private final String code;
    private final String description;

    public TreatmentClassRowResponse(Integer id, String code, String description) {
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
