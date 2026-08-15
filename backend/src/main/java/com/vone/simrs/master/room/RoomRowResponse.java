package com.vone.simrs.master.room;

/**
 * Baris data kamar (SCM0019 ROOM MASTER).
 * Mengikuti legacy {@code RoomController.getDataRoomList} yang menampilkan
 * RUANGAN, KELAS TARIF, NO. KAMAR, dan NAMA KAMAR.
 */
public class RoomRowResponse {

    private final Integer id;
    private final String hallName;
    private final String tariffClass;
    private final String roomCode;
    private final String roomName;

    public RoomRowResponse(Integer id, String hallName, String tariffClass,
            String roomCode, String roomName) {
        this.id = id;
        this.hallName = hallName;
        this.tariffClass = tariffClass;
        this.roomCode = roomCode;
        this.roomName = roomName;
    }

    public Integer getId() {
        return id;
    }

    public String getHallName() {
        return hallName;
    }

    public String getTariffClass() {
        return tariffClass;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getRoomName() {
        return roomName;
    }
}
