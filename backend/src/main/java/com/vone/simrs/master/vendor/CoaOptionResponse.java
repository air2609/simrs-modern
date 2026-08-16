package com.vone.simrs.master.vendor;

/**
 * Opsi hasil pencarian COA (SCM0043). Mengikuti
 * {@code CoaDAO.getCoaByCodeAndName()} pada tabel ms_coa.
 */
public class CoaOptionResponse {

    private final Integer id;
    private final String coaNo;
    private final String coaName;

    public CoaOptionResponse(Integer id, String coaNo, String coaName) {
        this.id = id;
        this.coaNo = coaNo;
        this.coaName = coaName;
    }

    public Integer getId() {
        return id;
    }

    public String getCoaNo() {
        return coaNo;
    }

    public String getCoaName() {
        return coaName;
    }
}
