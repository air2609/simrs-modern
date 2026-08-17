package com.vone.simrs.antrian.apotik;

/**
 * Request untuk menyimpan teks antrian apotik pada screen SCM0054.
 */
public class AntrianApotikSaveRequest {

    private String antrianText;

    public String getAntrianText() {
        return antrianText;
    }

    public void setAntrianText(String antrianText) {
        this.antrianText = antrianText;
    }
}
