package com.vone.simrs.master.room;

/**
 * Opsi ruangan (hall) untuk bandbox pencarian NAMA RUANGAN pada SCM0019.
 * Mengikuti legacy {@code MsHallDAO.searchHall} yang menampilkan
 * RUANGAN dan KELAS TARIF.
 */
public class HallOptionResponse {

    private final Integer hallId;
    private final String hallName;
    private final String tariffClass;

    public HallOptionResponse(Integer hallId, String hallName, String tariffClass) {
        this.hallId = hallId;
        this.hallName = hallName;
        this.tariffClass = tariffClass;
    }

    public Integer getHallId() {
        return hallId;
    }

    public String getHallName() {
        return hallName;
    }

    public String getTariffClass() {
        return tariffClass;
    }
}
