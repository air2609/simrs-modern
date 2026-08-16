package com.vone.simrs.master.itemsellingprice;

import com.vone.simrs.master.treatment.TreatmentClassOptionResponse;
import java.util.List;

/**
 * Data master untuk form harga jual item (SCM0041): opsi kelas tarif
 * (KELAS TARIF) dan opsi item (untuk bandbox KODE). Mengikuti pilihan
 * yang ada pada legacy {@code msItemSellingPrice.zul}.
 */
public class ItemSellingPriceMastersResponse {

    private final List<TreatmentClassOptionResponse> treatmentClassOptions;
    private final List<ItemOptionResponse> itemOptions;

    public ItemSellingPriceMastersResponse(
            List<TreatmentClassOptionResponse> treatmentClassOptions,
            List<ItemOptionResponse> itemOptions) {
        this.treatmentClassOptions = treatmentClassOptions;
        this.itemOptions = itemOptions;
    }

    public List<TreatmentClassOptionResponse> getTreatmentClassOptions() {
        return treatmentClassOptions;
    }

    public List<ItemOptionResponse> getItemOptions() {
        return itemOptions;
    }
}
