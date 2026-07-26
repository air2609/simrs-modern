package com.vone.simrs.apotik;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class ApotikSaveRequest {

    @NotNull
    private Integer unitId;

    @NotNull
    private Boolean referencePatient;

    private String existingMrCode;
    private Integer patientTypeId;
    private String patientName;
    private String gender;
    private String birthDate;
    private String address;
    private String receiptNumber;
    private String noteNumber;

    @NotEmpty
    private List<ApotikLineItemRequest> lines;

    public Integer getUnitId() { return unitId; }
    public void setUnitId(Integer unitId) { this.unitId = unitId; }
    public Boolean getReferencePatient() { return referencePatient; }
    public void setReferencePatient(Boolean referencePatient) { this.referencePatient = referencePatient; }
    public String getExistingMrCode() { return existingMrCode; }
    public void setExistingMrCode(String existingMrCode) { this.existingMrCode = existingMrCode; }
    public Integer getPatientTypeId() { return patientTypeId; }
    public void setPatientTypeId(Integer patientTypeId) { this.patientTypeId = patientTypeId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getNoteNumber() { return noteNumber; }
    public void setNoteNumber(String noteNumber) { this.noteNumber = noteNumber; }
    public List<ApotikLineItemRequest> getLines() { return lines; }
    public void setLines(List<ApotikLineItemRequest> lines) { this.lines = lines; }
}
