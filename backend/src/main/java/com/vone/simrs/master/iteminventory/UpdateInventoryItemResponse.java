package com.vone.simrs.master.iteminventory;

/**
 * Baris daftar item (screen SCM0057 updateInventory.zul). Migrasi dari legacy
 * {@code ItemDAO.serachItemUnderBuffer()} — item dengan total stok di bawah
 * batas buffer (tampilan hanya yang stoknya 0, sesuai legacy).
 */
public class UpdateInventoryItemResponse {

    private final Integer itemId;
    private final String code;
    private final String name;
    private final String unit;
    private final Integer buffer;
    private final Integer jumlah;

    public UpdateInventoryItemResponse(Integer itemId, String code, String name, String unit,
            Integer buffer, Integer jumlah) {
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.buffer = buffer;
        this.jumlah = jumlah;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getBuffer() {
        return buffer;
    }

    public Integer getJumlah() {
        return jumlah;
    }
}
