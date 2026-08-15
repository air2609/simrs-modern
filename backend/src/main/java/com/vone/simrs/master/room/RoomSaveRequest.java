package com.vone.simrs.master.room;

/**
 * Request simpan / ubah kamar (SCM0019 ROOM MASTER).
 * Mengikuti legacy {@code RoomController.doSaveAdd} + {@code doSaveModify}.
 */
public class RoomSaveRequest {

    private Integer id;
    private Integer hallId;
    private String roomCode;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getHallId() {
        return hallId;
    }

    public void setHallId(Integer hallId) {
        this.hallId = hallId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}
