package com.vone.simrs.master.doctor;

import java.util.List;

/**
 * Baris data dokter (SCM0030). Mengikuti entity legacy {@code MsDoctor}
 * (tabel ms_doctor) yang digabung dengan {@code MsStaff} (tabel ms_staff)
 * dan relasi {@code MsStaffInUnit} (tabel ms_staff_in_unit).
 */
public class DoctorRowResponse {

    private final Integer id;
    private final Integer staffId;
    private final String code;
    private final String name;
    private final String address;
    private final String phone;
    private final Integer coaId;
    private final String coaNo;
    private final Integer staffGroup;
    private final String staffGroupName;
    private final String levelOfExpertise;
    private final String status;
    private final Integer outPatientEarnings;
    private final String bankAccNo;
    private final Integer assistenOf;
    private final String assistenOfName;
    private final Integer percentageInPatientWage;
    private final Integer docType;
    private final Integer flagAntrian;
    private final String hiredDate;
    private final String firedDate;
    private final List<Integer> unitIds;

    public DoctorRowResponse(Integer id, Integer staffId, String code, String name,
            String address, String phone, Integer coaId, String coaNo,
            Integer staffGroup, String staffGroupName, String levelOfExpertise,
            String status, Integer outPatientEarnings, String bankAccNo,
            Integer assistenOf, String assistenOfName, Integer percentageInPatientWage,
            Integer docType, Integer flagAntrian, String hiredDate, String firedDate,
            List<Integer> unitIds) {
        this.id = id;
        this.staffId = staffId;
        this.code = code;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.staffGroup = staffGroup;
        this.staffGroupName = staffGroupName;
        this.levelOfExpertise = levelOfExpertise;
        this.status = status;
        this.outPatientEarnings = outPatientEarnings;
        this.bankAccNo = bankAccNo;
        this.assistenOf = assistenOf;
        this.assistenOfName = assistenOfName;
        this.percentageInPatientWage = percentageInPatientWage;
        this.docType = docType;
        this.flagAntrian = flagAntrian;
        this.hiredDate = hiredDate;
        this.firedDate = firedDate;
        this.unitIds = unitIds;
    }

    public Integer getId() {
        return id;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public String getCoaNo() {
        return coaNo;
    }

    public Integer getStaffGroup() {
        return staffGroup;
    }

    public String getStaffGroupName() {
        return staffGroupName;
    }

    public String getLevelOfExpertise() {
        return levelOfExpertise;
    }

    public String getStatus() {
        return status;
    }

    public Integer getOutPatientEarnings() {
        return outPatientEarnings;
    }

    public String getBankAccNo() {
        return bankAccNo;
    }

    public Integer getAssistenOf() {
        return assistenOf;
    }

    public String getAssistenOfName() {
        return assistenOfName;
    }

    public Integer getPercentageInPatientWage() {
        return percentageInPatientWage;
    }

    public Integer getDocType() {
        return docType;
    }

    public Integer getFlagAntrian() {
        return flagAntrian;
    }

    public String getHiredDate() {
        return hiredDate;
    }

    public String getFiredDate() {
        return firedDate;
    }

    public List<Integer> getUnitIds() {
        return unitIds;
    }
}
