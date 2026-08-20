package com.vone.simrs.master.patient;

/**
 * Opsi dropdown (id + label) untuk master pasien (SCM0011).
 */
public class OptionResponse {

    private final Integer id;
    private final String label;

    public OptionResponse(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public OptionResponse(String label) {
        this(null, label);
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
