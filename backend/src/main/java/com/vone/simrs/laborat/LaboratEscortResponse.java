package com.vone.simrs.laborat;

public class LaboratEscortResponse {
    private final Integer escortId;
    private final String name;

    public LaboratEscortResponse(Integer escortId, String name) {
        this.escortId = escortId;
        this.name = name;
    }

    public Integer getEscortId() { return escortId; }
    public String getName() { return name; }
}
