package com.vone.simrs.master.doctor;

/**
 * Opsi dropdown COA untuk form dokter (SCM0030).
 * Mengikuti {@code CoaController.getCoaForSelect} pada tabel ms_coa.
 */
public class CoaOptionResponse {

    private final Integer id;
    private final String no;
    private final String name;

    public CoaOptionResponse(Integer id, String no, String name) {
        this.id = id;
        this.no = no;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getNo() {
        return no;
    }

    public String getName() {
        return name;
    }
}
