package com.vone.simrs.ward.bedinfo;

public class BedInfoRowResponse {

    private final String tariffClass;
    private final String hallName;
    private final int totalBeds;
    private final int occupiedBeds;
    private final int bookedBeds;
    private final int inServiceBeds;
    private final int emptyBeds;

    public BedInfoRowResponse(String tariffClass, String hallName, int totalBeds,
            int occupiedBeds, int bookedBeds, int inServiceBeds, int emptyBeds) {
        this.tariffClass = tariffClass;
        this.hallName = hallName;
        this.totalBeds = totalBeds;
        this.occupiedBeds = occupiedBeds;
        this.bookedBeds = bookedBeds;
        this.inServiceBeds = inServiceBeds;
        this.emptyBeds = emptyBeds;
    }

    public String getTariffClass() {
        return tariffClass;
    }

    public String getHallName() {
        return hallName;
    }

    public int getTotalBeds() {
        return totalBeds;
    }

    public int getOccupiedBeds() {
        return occupiedBeds;
    }

    public int getBookedBeds() {
        return bookedBeds;
    }

    public int getInServiceBeds() {
        return inServiceBeds;
    }

    public int getEmptyBeds() {
        return emptyBeds;
    }
}
