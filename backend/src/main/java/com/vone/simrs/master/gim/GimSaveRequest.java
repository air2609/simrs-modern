package com.vone.simrs.master.gim;

/**
 * Request simpan/edit general information (SCM0047). Mengikuti field yang
 * diisi pada form legacy {@code GimController} (key dan value).
 */
public class GimSaveRequest {

    private Integer id;
    private String key;
    private String value;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
