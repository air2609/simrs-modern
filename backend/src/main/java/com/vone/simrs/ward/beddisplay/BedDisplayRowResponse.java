package com.vone.simrs.ward.beddisplay;

public class BedDisplayRowResponse {

    private final Integer bedId;
    private final String tariffClass;
    private final String roomName;
    private final String roomNumber;
    private final String bedCode;
    private final String bedDesc;
    private final String condition;
    private final boolean shown;
    private final String availableStatus;

    public BedDisplayRowResponse(Integer bedId, String tariffClass, String roomName, String roomNumber,
            String bedCode, String bedDesc, String condition, boolean shown, String availableStatus) {
        this.bedId = bedId;
        this.tariffClass = tariffClass;
        this.roomName = roomName;
        this.roomNumber = roomNumber;
        this.bedCode = bedCode;
        this.bedDesc = bedDesc;
        this.condition = condition;
        this.shown = shown;
        this.availableStatus = availableStatus;
    }

    public Integer getBedId() {
        return bedId;
    }

    public String getTariffClass() {
        return tariffClass;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getBedCode() {
        return bedCode;
    }

    public String getBedDesc() {
        return bedDesc;
    }

    public String getCondition() {
        return condition;
    }

    public boolean isShown() {
        return shown;
    }

    public String getAvailableStatus() {
        return availableStatus;
    }
}
