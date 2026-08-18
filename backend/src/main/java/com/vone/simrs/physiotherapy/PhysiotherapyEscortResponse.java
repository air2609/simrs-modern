package com.vone.simrs.physiotherapy;

/**
 * Tipe pembawa pasien (ms_patient_escort) untuk select TIPE PEMBAWA screen SC0141.
 */
public class PhysiotherapyEscortResponse {

    private final Integer escortId;
    private final String code;
    private final String type;

    public PhysiotherapyEscortResponse(Integer escortId, String code, String type) {
        this.escortId = escortId;
        this.code = code;
        this.type = type;
    }

    public Integer getEscortId() {
        return escortId;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
