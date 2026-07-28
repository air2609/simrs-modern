package com.vone.simrs.laborat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Request untuk menyimpan/mengupdate hasil lab (SC0043).
 */
public class LaboratResultSaveRequest {

    @NotNull
    private Integer examId;             // n_exam_id dari tb_examination

    private String mrCode;
    private String registrationCode;

    private String takeTime;            // v_jam
    private String escortDoctor;        // v_dr_pengirim
    private String laboratNo;           // v_resep

    @NotNull
    private List<LaboratResultLineRequest> lines;

    public Integer getExamId() { return examId; }
    public void setExamId(Integer examId) { this.examId = examId; }
    public String getMrCode() { return mrCode; }
    public void setMrCode(String mrCode) { this.mrCode = mrCode; }
    public String getRegistrationCode() { return registrationCode; }
    public void setRegistrationCode(String registrationCode) { this.registrationCode = registrationCode; }
    public String getTakeTime() { return takeTime; }
    public void setTakeTime(String takeTime) { this.takeTime = takeTime; }
    public String getEscortDoctor() { return escortDoctor; }
    public void setEscortDoctor(String escortDoctor) { this.escortDoctor = escortDoctor; }
    public String getLaboratNo() { return laboratNo; }
    public void setLaboratNo(String laboratNo) { this.laboratNo = laboratNo; }
    public List<LaboratResultLineRequest> getLines() { return lines; }
    public void setLines(List<LaboratResultLineRequest> lines) { this.lines = lines; }
}
