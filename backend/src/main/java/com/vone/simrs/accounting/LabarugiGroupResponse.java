package com.vone.simrs.accounting;

import java.util.List;

/**
 * Grup baris LABA RUGI (SC0203 / labaRugi.zul). Migrasi dari legacy
 * {@code LabarugiController.createGroupTree()} yang mengelompokkan akun
 * berdasarkan {@code n_row}: 1-5 = jenis COA ({@code ms_coa_type.v_ct_name}),
 * 6 = "RINGKASAN".
 */
public class LabarugiGroupResponse {

    private final int row;
    private final String caption;
    private final List<LabarugiRowResponse> items;

    public LabarugiGroupResponse(int row, String caption, List<LabarugiRowResponse> items) {
        this.row = row;
        this.caption = caption;
        this.items = items;
    }

    public int getRow() {
        return row;
    }

    public String getCaption() {
        return caption;
    }

    public List<LabarugiRowResponse> getItems() {
        return items;
    }
}
