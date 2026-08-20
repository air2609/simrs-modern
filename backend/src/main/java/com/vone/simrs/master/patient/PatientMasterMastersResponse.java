package com.vone.simrs.master.patient;

import java.util.List;

/**
 * Data master utk dropdown form pasien (SCM0011). Migrasi dari legacy
 * {@code PatientController.init()} — list statis (agama, warga negara,
 * status kawin, pendidikan, pekerjaan, prioritas) + list DB
 * (tipe pasien, propinsi, kabupaten, kecamatan, kelurahan).
 */
public class PatientMasterMastersResponse {

    private final List<OptionResponse> religions;
    private final List<OptionResponse> nationalities;
    private final List<OptionResponse> maritalStatuses;
    private final List<OptionResponse> educations;
    private final List<OptionResponse> jobTypes;
    private final List<OptionResponse> priorities;
    private final List<OptionResponse> patientTypes;
    private final List<OptionResponse> provinces;
    private final List<OptionResponse> regencies;
    private final List<OptionResponse> subDistricts;
    private final List<OptionResponse> villages;

    public PatientMasterMastersResponse(List<OptionResponse> religions,
            List<OptionResponse> nationalities, List<OptionResponse> maritalStatuses,
            List<OptionResponse> educations, List<OptionResponse> jobTypes,
            List<OptionResponse> priorities, List<OptionResponse> patientTypes,
            List<OptionResponse> provinces, List<OptionResponse> regencies,
            List<OptionResponse> subDistricts, List<OptionResponse> villages) {
        this.religions = religions;
        this.nationalities = nationalities;
        this.maritalStatuses = maritalStatuses;
        this.educations = educations;
        this.jobTypes = jobTypes;
        this.priorities = priorities;
        this.patientTypes = patientTypes;
        this.provinces = provinces;
        this.regencies = regencies;
        this.subDistricts = subDistricts;
        this.villages = villages;
    }

    public List<OptionResponse> getReligions() {
        return religions;
    }

    public List<OptionResponse> getNationalities() {
        return nationalities;
    }

    public List<OptionResponse> getMaritalStatuses() {
        return maritalStatuses;
    }

    public List<OptionResponse> getEducations() {
        return educations;
    }

    public List<OptionResponse> getJobTypes() {
        return jobTypes;
    }

    public List<OptionResponse> getPriorities() {
        return priorities;
    }

    public List<OptionResponse> getPatientTypes() {
        return patientTypes;
    }

    public List<OptionResponse> getProvinces() {
        return provinces;
    }

    public List<OptionResponse> getRegencies() {
        return regencies;
    }

    public List<OptionResponse> getSubDistricts() {
        return subDistricts;
    }

    public List<OptionResponse> getVillages() {
        return villages;
    }
}
