package com.vone.simrs.admission;

/**
 * Ruangan (ms_hall) untuk bandbox RUANGAN screen SC0005 (CariPasien.zul).
 * Migrasi dari legacy {@code HallManagerImpl.searchHall(PencarianPasientRanapController)}
 * + {@code MsHallDAO.searchHall()}.
 */
public class CariPasienHallResponse {

    private final Integer hallId;
    private final String code;
    private final String name;

    public CariPasienHallResponse(Integer hallId, String code, String name) {
        this.hallId = hallId;
        this.code = code;
        this.name = name;
    }

    public Integer getHallId() {
        return hallId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
