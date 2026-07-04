package com.vone.simrs.admission;

public class AdmissionRegistrationResultResponse {

    private final boolean existingPatient;
    private final String mrCode;
    private final String registrationCode;
    private final String noteNumber;
    private final String patientName;

    public AdmissionRegistrationResultResponse(
        boolean existingPatient,
        String mrCode,
        String registrationCode,
        String noteNumber,
        String patientName
    ) {
        this.existingPatient = existingPatient;
        this.mrCode = mrCode;
        this.registrationCode = registrationCode;
        this.noteNumber = noteNumber;
        this.patientName = patientName;
    }

    public boolean isExistingPatient() {
        return existingPatient;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public String getPatientName() {
        return patientName;
    }
}
