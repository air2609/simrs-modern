package com.vone.simrs.master.treatmentgroup;

/**
 * Baris data treatment group (SCM0023). Mengikuti entity legacy
 * {@code MsTreatmentGroup} (tabel ms_treatment_group).
 */
public class TreatmentGroupRowResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public TreatmentGroupRowResponse(Integer id, String code, String name) {
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
