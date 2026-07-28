package com.vone.simrs.laborat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public class LaboratSaveRequest {
    @NotBlank
    private String mrCode;
    private String registrationCode;
    @NotNull
    private Integer unitId;
    @NotNull
    private Integer patientTypeId;
    private String doctorStaffId;
    private Integer escortId;
    private Boolean referencePatient;
    private List<LaboratLineItemRequest> treatments;
    private List<LaboratLineItemRequest> items;

    public String getMrCode() { return mrCode; }
    public void setMrCode(String mrCode) { this.mrCode = mrCode; }
    public String getRegistrationCode() { return registrationCode; }
    public void setRegistrationCode(String registrationCode) { this.registrationCode = registrationCode; }
    public Integer getUnitId() { return unitId; }
    public void setUnitId(Integer unitId) { this.unitId = unitId; }
    public Integer getPatientTypeId() { return patientTypeId; }
    public void setPatientTypeId(Integer patientTypeId) { this.patientTypeId = patientTypeId; }
    public String getDoctorStaffId() { return doctorStaffId; }
    public void setDoctorStaffId(String doctorStaffId) { this.doctorStaffId = doctorStaffId; }
    public Integer getEscortId() { return escortId; }
    public void setEscortId(Integer escortId) { this.escortId = escortId; }
    public Boolean getReferencePatient() { return referencePatient; }
    public void setReferencePatient(Boolean referencePatient) { this.referencePatient = referencePatient; }
    public List<LaboratLineItemRequest> getTreatments() { return treatments; }
    public void setTreatments(List<LaboratLineItemRequest> treatments) { this.treatments = treatments; }
    public List<LaboratLineItemRequest> getItems() { return items; }
    public void setItems(List<LaboratLineItemRequest> items) { this.items = items; }
}
