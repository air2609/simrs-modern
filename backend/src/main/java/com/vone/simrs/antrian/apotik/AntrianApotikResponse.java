package com.vone.simrs.antrian.apotik;

import java.util.List;

/**
 * Response untuk screen SCM0054 (KONTROL ANTRIAN APOTIK).
 * Berisi daftar nota yang sudah divalidasi, daftar obat yang sudah jadi,
 * dan teks antrian (rolling text apotik).
 */
public class AntrianApotikResponse {

    private final List<AntrianApotikNoteResponse> validatedNotes;
    private final List<AntrianApotikNoteResponse> readyNotes;
    private final String antrianText;
    private final boolean hasAntrianText;

    public AntrianApotikResponse(List<AntrianApotikNoteResponse> validatedNotes,
            List<AntrianApotikNoteResponse> readyNotes,
            String antrianText, boolean hasAntrianText) {
        this.validatedNotes = validatedNotes;
        this.readyNotes = readyNotes;
        this.antrianText = antrianText;
        this.hasAntrianText = hasAntrianText;
    }

    public List<AntrianApotikNoteResponse> getValidatedNotes() {
        return validatedNotes;
    }

    public List<AntrianApotikNoteResponse> getReadyNotes() {
        return readyNotes;
    }

    public String getAntrianText() {
        return antrianText;
    }

    public boolean isHasAntrianText() {
        return hasAntrianText;
    }
}
