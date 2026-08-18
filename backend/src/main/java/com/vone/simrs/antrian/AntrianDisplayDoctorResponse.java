package com.vone.simrs.antrian;

import java.util.List;

/**
 * Satu dokter + antriannya untuk layar display. Migrasi dari legacy
 * {@code DoctorManagerImpl.getAntrianDokter()} (getDoctorForAntrian).
 */
public class AntrianDisplayDoctorResponse {

    private final Integer staffId;
    private final String name;
    private final List<DelayAntrianQueueRowResponse> queue;

    public AntrianDisplayDoctorResponse(Integer staffId, String name,
            List<DelayAntrianQueueRowResponse> queue) {
        this.staffId = staffId;
        this.name = name;
        this.queue = queue;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getName() {
        return name;
    }

    public List<DelayAntrianQueueRowResponse> getQueue() {
        return queue;
    }
}
