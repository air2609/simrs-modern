package com.vone.simrs.vitalsign;

import java.math.BigDecimal;

/**
 * Request penyimpanan data tanda-tanda vital (TTV).
 */
public class VitalSignSaveRequest {

    private String registrationCode;
    /** Format ISO local date-time, mis. {@code 2026-08-25T10:58}. Kosong = sekarang. */
    private String ttvDate;
    private Integer systole;
    private Integer diastole;
    private Integer pulse;
    private Integer respiratoryRate;
    private BigDecimal temperature;
    private Integer spo2;
    private BigDecimal weight;
    private BigDecimal height;
    private Integer painScale;
    private String consciousness;
    private String note;

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }

    public String getTtvDate() {
        return ttvDate;
    }

    public void setTtvDate(String ttvDate) {
        this.ttvDate = ttvDate;
    }

    public Integer getSystole() {
        return systole;
    }

    public void setSystole(Integer systole) {
        this.systole = systole;
    }

    public Integer getDiastole() {
        return diastole;
    }

    public void setDiastole(Integer diastole) {
        this.diastole = diastole;
    }

    public Integer getPulse() {
        return pulse;
    }

    public void setPulse(Integer pulse) {
        this.pulse = pulse;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Integer respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public Integer getSpo2() {
        return spo2;
    }

    public void setSpo2(Integer spo2) {
        this.spo2 = spo2;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public Integer getPainScale() {
        return painScale;
    }

    public void setPainScale(Integer painScale) {
        this.painScale = painScale;
    }

    public String getConsciousness() {
        return consciousness;
    }

    public void setConsciousness(String consciousness) {
        this.consciousness = consciousness;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
