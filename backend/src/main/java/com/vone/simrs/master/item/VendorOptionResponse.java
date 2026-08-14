package com.vone.simrs.master.item;

/**
 * Opsi supplier/vendor (SCM0038). Mengikuti entity legacy {@code MsVendor}
 * (tabel ms_vendor).
 */
public class VendorOptionResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public VendorOptionResponse(Integer id, String code, String name) {
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
