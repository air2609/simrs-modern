package com.vone.simrs.master.treatmentclass;

/**
 * Request simpan/edit kelas tarif (SCM0021). Mengikuti field yang diisi pada
 * form legacy {@code TreatmentClassController} (kode + nama).
 */
public class TreatmentClassSaveRequest {

    private Integer id;
    private String code;
    private String description;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
