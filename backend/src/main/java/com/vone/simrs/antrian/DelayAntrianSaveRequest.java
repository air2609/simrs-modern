package com.vone.simrs.antrian;

/**
 * Request simpan master antrian (delay + text).
 */
public class DelayAntrianSaveRequest {

    private Integer delayAntrian;
    private String textAntrian;

    public Integer getDelayAntrian() {
        return delayAntrian;
    }

    public void setDelayAntrian(Integer delayAntrian) {
        this.delayAntrian = delayAntrian;
    }

    public String getTextAntrian() {
        return textAntrian;
    }

    public void setTextAntrian(String textAntrian) {
        this.textAntrian = textAntrian;
    }
}
