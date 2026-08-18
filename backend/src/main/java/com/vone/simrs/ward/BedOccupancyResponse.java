package com.vone.simrs.ward;

import java.util.List;

/**
 * Satu bed occupancy pada tree HISTORY BED PASIEN. Migrasi dari legacy
 * {@code BedTransactionDAO.getBocsBaseOnRegistration()}.
 */
public class BedOccupancyResponse {

    private final Integer bedId;
    private final String bedDesc;
    private final String checkIn;
    private final String checkOut;
    private final List<BedDayResponse> days;

    public BedOccupancyResponse(Integer bedId, String bedDesc, String checkIn, String checkOut,
            List<BedDayResponse> days) {
        this.bedId = bedId;
        this.bedDesc = bedDesc;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.days = days;
    }

    public Integer getBedId() {
        return bedId;
    }

    public String getBedDesc() {
        return bedDesc;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public List<BedDayResponse> getDays() {
        return days;
    }
}
