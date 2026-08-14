package com.vone.simrs.master.treatmentgroup;

/**
 * Request simpan/edit treatment group (SCM0023). Mengikuti field yang diisi
 * pada form legacy {@code TreatmentGroupController} (kode + nama).
 */
public class TreatmentGroupSaveRequest {

    private Integer id;
    private String code;
    private String name;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
