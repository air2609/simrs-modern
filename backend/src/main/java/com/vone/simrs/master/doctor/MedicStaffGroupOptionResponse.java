package com.vone.simrs.master.doctor;

/**
 * Opsi dropdown group staff medis untuk form dokter (SCM0030).
 * Mengikuti pilihan statis pada legacy {@code msDokter.zul}
 * (medicStaffGroupList):
 * 4 = DOKTER, 5 = ANASTESI, 10 = RADIOGRAFER.
 */
public class MedicStaffGroupOptionResponse {

    private final Integer id;
    private final String name;

    public MedicStaffGroupOptionResponse(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
