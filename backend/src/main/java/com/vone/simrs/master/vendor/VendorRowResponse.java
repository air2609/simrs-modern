package com.vone.simrs.master.vendor;

/**
 * Baris data vendor/supplier (SCM0043 - VENDOR MASTER).
 * Mengikuti entity legacy {@code MsVendor} (tabel ms_vendor).
 */
public class VendorRowResponse {

    private final Integer id;
    private final String code;
    private final String name;
    private final String address;
    private final String contactPerson;
    private final String contactNo;
    private final String altContactNo;
    private final String faxNo;
    private final Integer coaId;
    private final String coaNo;
    private final String coaName;

    public VendorRowResponse(Integer id, String code, String name, String address,
            String contactPerson, String contactNo, String altContactNo, String faxNo,
            Integer coaId, String coaNo, String coaName) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.address = address;
        this.contactPerson = contactPerson;
        this.contactNo = contactNo;
        this.altContactNo = altContactNo;
        this.faxNo = faxNo;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.coaName = coaName;
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

    public String getAddress() {
        return address;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getContactNo() {
        return contactNo;
    }

    public String getAltContactNo() {
        return altContactNo;
    }

    public String getFaxNo() {
        return faxNo;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public String getCoaNo() {
        return coaNo;
    }

    public String getCoaName() {
        return coaName;
    }
}
