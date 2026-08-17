package com.vone.simrs.antrian.apotikdisplay;

import java.util.List;

/**
 * Response untuk screen RPT0020 (papan display OBAT PASIEN SUDAH JADI).
 * Berisi daftar obat yang sudah jadi, teks berjalan (rolling text apotik),
 * dan interval refresh (delay antrian, dalam milidetik).
 */
public class AntrianApotikDisplayResponse {

    private final List<AntrianApotikDisplayItemResponse> items;
    private final String antrianText;
    private final int delayMillis;

    public AntrianApotikDisplayResponse(List<AntrianApotikDisplayItemResponse> items,
            String antrianText, int delayMillis) {
        this.items = items;
        this.antrianText = antrianText;
        this.delayMillis = delayMillis;
    }

    public List<AntrianApotikDisplayItemResponse> getItems() {
        return items;
    }

    public String getAntrianText() {
        return antrianText;
    }

    public int getDelayMillis() {
        return delayMillis;
    }
}
