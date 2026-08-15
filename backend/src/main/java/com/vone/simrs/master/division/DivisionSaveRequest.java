package com.vone.simrs.master.division;

/**
 * Request simpan/edit divisi (SCM0022). Mengikuti field yang diisi pada form
 * legacy {@code DivisionController} (kode, nama, klasifikasi unit registrasi,
 * dan biaya daftar).
 */
public class DivisionSaveRequest {

    private Integer id;
    private String code;
    private String name;
    private String registrationUnit;
    private Integer registrationCharge;

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

    public String getRegistrationUnit() {
        return registrationUnit;
    }

    public void setRegistrationUnit(String registrationUnit) {
        this.registrationUnit = registrationUnit;
    }

    public Integer getRegistrationCharge() {
        return registrationCharge;
    }

    public void setRegistrationCharge(Integer registrationCharge) {
        this.registrationCharge = registrationCharge;
    }
}
