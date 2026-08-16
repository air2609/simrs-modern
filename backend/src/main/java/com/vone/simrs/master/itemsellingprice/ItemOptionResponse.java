package com.vone.simrs.master.itemsellingprice;

/**
 * Opsi item untuk bandbox KODE pada form harga jual item (SCM0041).
 * Mengikuti data ms_item.
 */
public class ItemOptionResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public ItemOptionResponse(Integer id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
