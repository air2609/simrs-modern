package com.vone.simrs.admission;

import java.util.List;

/**
 * Masters pendaftaran rawat inap (SC0001 tab 2): kelas tarif.
 */
public class RanapMastersResponse {

    private final List<OptionResponse> treatmentClasses;

    public RanapMastersResponse(List<OptionResponse> treatmentClasses) {
        this.treatmentClasses = treatmentClasses;
    }

    public List<OptionResponse> getTreatmentClasses() {
        return treatmentClasses;
    }
}
