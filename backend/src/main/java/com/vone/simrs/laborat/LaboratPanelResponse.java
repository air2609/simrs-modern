package com.vone.simrs.laborat;

import java.util.List;

public class LaboratPanelResponse {
    private final String panelName;
    private final List<LaboratTreatmentOptionResponse> treatments;

    public LaboratPanelResponse(String panelName, List<LaboratTreatmentOptionResponse> treatments) {
        this.panelName = panelName;
        this.treatments = treatments;
    }

    public String getPanelName() { return panelName; }
    public List<LaboratTreatmentOptionResponse> getTreatments() { return treatments; }
}
