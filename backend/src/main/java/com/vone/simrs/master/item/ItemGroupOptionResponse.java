package com.vone.simrs.master.item;

/**
 * Opsi group item (SCM0038). Mengikuti entity legacy {@code MsItemGroup}
 * (tabel ms_item_group).
 */
public class ItemGroupOptionResponse {

    private final Integer id;
    private final String code;
    private final String name;

    public ItemGroupOptionResponse(Integer id, String code, String name) {
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
