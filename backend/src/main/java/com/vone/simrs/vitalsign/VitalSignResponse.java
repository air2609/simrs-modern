package com.vone.simrs.vitalsign;

import java.math.BigDecimal;

/**
 * Data tanda-tanda vital (TTV) untuk ditampilkan.
 */
public class VitalSignResponse {

    private final Long id;
    private final String registrationCode;
    private final String ttvDate;
    private final Integer systole;
    private final Integer diastole;
    private final Integer pulse;
    private final Integer respiratoryRate;
    private final BigDecimal temperature;
    private final Integer spo2;
    private final BigDecimal weight;
    private final BigDecimal height;
    private final Integer painScale;
    private final String consciousness;
    private final String note;

    public VitalSignResponse(Long id, String registrationCode, String ttvDate, Integer systole, Integer diastole,
            Integer pulse, Integer respiratoryRate, BigDecimal temperature, Integer spo2, BigDecimal weight,
            BigDecimal height, Integer painScale, String consciousness, String note) {
        this.id = id;
        this.registrationCode = registrationCode;
        this.ttvDate = ttvDate;
        this.systole = systole;
        this.diastole = diastole;
        this.pulse = pulse;
        this.respiratoryRate = respiratoryRate;
        this.temperature = temperature;
        this.spo2 = spo2;
        this.weight = weight;
        this.height = height;
        this.painScale = painScale;
        this.consciousness = consciousness;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public String getTtvDate() {
        return ttvDate;
    }

    public Integer getSystole() {
        return systole;
    }

    public Integer getDiastole() {
        return diastole;
    }

    public Integer getPulse() {
        return pulse;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public Integer getSpo2() {
        return spo2;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public Integer getPainScale() {
        return painScale;
    }

    public String getConsciousness() {
        return consciousness;
    }

    public String getNote() {
        return note;
    }
}
