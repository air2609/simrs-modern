package com.vone.simrs.master.bed;

/**
 * Request simpan / ubah bed (SCM0020 BED MASTER).
 * Mengikuti legacy {@code BedController.doSaveAdd} + {@code doSaveModify}.
 */
public class BedSaveRequest {

    private Integer id;
    private Integer roomId;
    private Integer treatmentClassId;
    private String bedCode;
    private String bedDesc;
    private Double bedPrice;
    private Integer coaId;
    private String activeStatus;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getTreatmentClassId() {
        return treatmentClassId;
    }

    public void setTreatmentClassId(Integer treatmentClassId) {
        this.treatmentClassId = treatmentClassId;
    }

    public String getBedCode() {
        return bedCode;
    }

    public void setBedCode(String bedCode) {
        this.bedCode = bedCode;
    }

    public String getBedDesc() {
        return bedDesc;
    }

    public void setBedDesc(String bedDesc) {
        this.bedDesc = bedDesc;
    }

    public Double getBedPrice() {
        return bedPrice;
    }

    public void setBedPrice(Double bedPrice) {
        this.bedPrice = bedPrice;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }
}
