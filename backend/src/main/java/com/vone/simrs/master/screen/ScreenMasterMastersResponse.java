package com.vone.simrs.master.screen;

import java.util.List;

public class ScreenMasterMastersResponse {

    private final List<SubsystemOptionResponse> subsystems;
    private final List<UnitOptionResponse> units;

    public ScreenMasterMastersResponse(List<SubsystemOptionResponse> subsystems, List<UnitOptionResponse> units) {
        this.subsystems = subsystems;
        this.units = units;
    }

    public List<SubsystemOptionResponse> getSubsystems() {
        return subsystems;
    }

    public List<UnitOptionResponse> getUnits() {
        return units;
    }
}
