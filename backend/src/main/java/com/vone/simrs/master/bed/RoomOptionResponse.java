package com.vone.simrs.master.bed;

/**
 * Opsi kamar (room) untuk bandbox pencarian NAMA KAMAR pada SCM0020.
 * Mengikuti legacy {@code MsRoomDAO.searchRoomByName} yang menampilkan
 * NAMA KAMAR dan KELAS TARIF.
 */
public class RoomOptionResponse {

    private final Integer roomId;
    private final String roomName;
    private final String tariffClass;

    public RoomOptionResponse(Integer roomId, String roomName, String tariffClass) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.tariffClass = tariffClass;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getTariffClass() {
        return tariffClass;
    }
}
