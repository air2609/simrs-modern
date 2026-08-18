package com.vone.simrs.cashier;

/**
 * Detail pasien untuk kasir: registrasi + bed + saldo deposit.
 */
public class CashierPatientDetailResponse {

    private final String mrCode;
    private final Integer registrationId;
    private final String registrationNumber;
    private final String patientName;
    private final String address;
    private final String patientTypeName;
    private final String bed;
    private final boolean ranap;
    private final Double depositBalance;

    public CashierPatientDetailResponse(String mrCode, Integer registrationId,
            String registrationNumber, String patientName, String address, String patientTypeName,
            String bed, boolean ranap, Double depositBalance) {
        this.mrCode = mrCode;
        this.registrationId = registrationId;
        this.registrationNumber = registrationNumber;
        this.patientName = patientName;
        this.address = address;
        this.patientTypeName = patientTypeName;
        this.bed = bed;
        this.ranap = ranap;
        this.depositBalance = depositBalance;
    }

    public String getMrCode() {
        return mrCode;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getAddress() {
        return address;
    }

    public String getPatientTypeName() {
        return patientTypeName;
    }

    public String getBed() {
        return bed;
    }

    public boolean isRanap() {
        return ranap;
    }

    public Double getDepositBalance() {
        return depositBalance;
    }
}
