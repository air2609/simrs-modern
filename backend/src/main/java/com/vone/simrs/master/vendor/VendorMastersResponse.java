package com.vone.simrs.master.vendor;

import java.util.List;

/**
 * Data master untuk form vendor (SCM0043 - VENDOR MASTER).
 * Berisi opsi COA untuk bandbox NO. COA.
 */
public class VendorMastersResponse {

    private final List<CoaOptionResponse> coaOptions;

    public VendorMastersResponse(List<CoaOptionResponse> coaOptions) {
        this.coaOptions = coaOptions;
    }

    public List<CoaOptionResponse> getCoaOptions() {
        return coaOptions;
    }
}
