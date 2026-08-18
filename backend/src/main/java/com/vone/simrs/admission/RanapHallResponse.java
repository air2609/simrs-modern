package com.vone.simrs.admission;

/**
 * Ruangan (hall) berdasarkan kelas tarif dengan jumlah bed tersisa. Migrasi
 * dari legacy {@code RegistrationManagerImpl.getHallListByTclassId()}.
 */
public class RanapHallResponse {

    private final Integer hallId;
    private final String name;
    private final Integer availableBeds;

    public RanapHallResponse(Integer hallId, String name, Integer availableBeds) {
        this.hallId = hallId;
        this.name = name;
        this.availableBeds = availableBeds;
    }

    public Integer getHallId() {
        return hallId;
    }

    public String getName() {
        return name;
    }

    public Integer getAvailableBeds() {
        return availableBeds;
    }
}
