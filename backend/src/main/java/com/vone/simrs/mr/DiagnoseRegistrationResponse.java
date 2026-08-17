package com.vone.simrs.mr;

import java.util.List;

/**
 * Detail registrasi &amp; diagnosa existing (jika ada) untuk screen SC0206
 * (FORM REKAM MEDIS DIAGNOSA), tab DIAGNOSA PASIEN.
 */
public class DiagnoseRegistrationResponse {

    private final Integer regId;
    private final String mrCode;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String doctorName;
    private final String unitLabel;
    private final Integer patientTypeId;
    private final String patientTypeDesc;
    private final boolean ranap;
    private final Integer existingDiagnoseId;
    private final String notes;
    private final List<Integer> icdIds;
    private final List<String> icdNames;

    public DiagnoseRegistrationResponse(Integer regId, String mrCode, String patientName, String gender,
            String birthDate, String doctorName, String unitLabel, Integer patientTypeId, String patientTypeDesc,
            boolean ranap, Integer existingDiagnoseId, String notes, List<Integer> icdIds, List<String> icdNames) {
        this.regId = regId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.doctorName = doctorName;
        this.unitLabel = unitLabel;
        this.patientTypeId = patientTypeId;
        this.patientTypeDesc = patientTypeDesc;
        this.ranap = ranap;
        this.existingDiagnoseId = existingDiagnoseId;
        this.notes = notes;
        this.icdIds = icdIds;
        this.icdNames = icdNames;
    }

    public Integer getRegId() {
        return regId;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getPatientTypeDesc() {
        return patientTypeDesc;
    }

    public boolean isRanap() {
        return ranap;
    }

    public Integer getExistingDiagnoseId() {
        return existingDiagnoseId;
    }

    public String getNotes() {
        return notes;
    }

    public List<Integer> getIcdIds() {
        return icdIds;
    }

    public List<String> getIcdNames() {
        return icdNames;
    }
}
