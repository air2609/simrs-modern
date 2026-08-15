package com.vone.simrs.master.bed;

/**
 * Baris data bed (SCM0020 BED MASTER).
 * Mengikuti legacy {@code BedManagerImpl.getAllBed} yang menampilkan
 * RUANGAN, NAMA BED, KELAS TARIF, NO. KAMAR, KODE BED, HARGA, dan STATUS.
 */
public class BedRowResponse {

    private final Integer id;
    private final Integer roomId;
    private final String roomName;
    private final String bedDesc;
    private final String tariffClass;
    private final String bedCode;
    private final Double bedPrice;
    private final String activeStatus;
    private final Integer coaId;
    private final String coaNo;
    private final String coaName;
    private final Integer treatmentClassId;

    public BedRowResponse(Integer id, Integer roomId, String roomName, String bedDesc,
            String tariffClass, String bedCode, Double bedPrice, String activeStatus,
            Integer coaId, String coaNo, String coaName, Integer treatmentClassId) {
        this.id = id;
        this.roomId = roomId;
        this.roomName = roomName;
        this.bedDesc = bedDesc;
        this.tariffClass = tariffClass;
        this.bedCode = bedCode;
        this.bedPrice = bedPrice;
        this.activeStatus = activeStatus;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.coaName = coaName;
        this.treatmentClassId = treatmentClassId;
    }

    public Integer getId() {
        return id;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getBedDesc() {
        return bedDesc;
    }

    public String getTariffClass() {
        return tariffClass;
    }

    public String getBedCode() {
        return bedCode;
    }

    public Double getBedPrice() {
        return bedPrice;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public String getCoaNo() {
        return coaNo;
    }

    public String getCoaName() {
        return coaName;
    }

    public Integer getTreatmentClassId() {
        return treatmentClassId;
    }
}
