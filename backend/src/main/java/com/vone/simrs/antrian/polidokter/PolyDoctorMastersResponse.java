package com.vone.simrs.antrian.polidokter;

import java.util.List;

/**
 * Data master untuk form dokter poli (SCM0059): opsi status poli dan opsi
 * booking. Mengikuti pilihan yang ada pada legacy {@code PoliDokterController}.
 */
public class PolyDoctorMastersResponse {

    private final List<String> polyStatusOptions;
    private final List<String> bookingOptions;

    public PolyDoctorMastersResponse(List<String> polyStatusOptions, List<String> bookingOptions) {
        this.polyStatusOptions = polyStatusOptions;
        this.bookingOptions = bookingOptions;
    }

    public List<String> getPolyStatusOptions() {
        return polyStatusOptions;
    }

    public List<String> getBookingOptions() {
        return bookingOptions;
    }
}
