package com.vone.simrs.accounting;

import java.util.List;

/**
 * Grup baris NERACA (SC0202 / neraca.zul). Migrasi dari legacy
 * {@code NeracaController.createGroupTree()} yang mengelompokkan akun
 * berdasarkan {@code n_row}: 1 = AKTIVA, 2 = KEWAJIBAN, 3 = MODAL.
 */
public class NeracaGroupResponse {

    private final int row;
    private final String caption;
    private final List<NeracaRowResponse> items;

    public NeracaGroupResponse(int row, String caption, List<NeracaRowResponse> items) {
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

    public List<NeracaRowResponse> getItems() {
        return items;
    }
}
