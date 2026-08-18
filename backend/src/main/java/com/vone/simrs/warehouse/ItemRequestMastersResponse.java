package com.vone.simrs.warehouse;

import java.util.List;

/**
 * Masters screen SC0174: gudang sumber (milik user) + gudang tujuan.
 * Migrasi dari legacy {@code WarehouseManagerImpl.initItemRequest()}.
 */
public class ItemRequestMastersResponse {

    private final List<ItemRequestWarehouseResponse> sourceWarehouses;
    private final List<ItemRequestWarehouseResponse> targetWarehouses;

    public ItemRequestMastersResponse(List<ItemRequestWarehouseResponse> sourceWarehouses,
            List<ItemRequestWarehouseResponse> targetWarehouses) {
        this.sourceWarehouses = sourceWarehouses;
        this.targetWarehouses = targetWarehouses;
    }

    public List<ItemRequestWarehouseResponse> getSourceWarehouses() {
        return sourceWarehouses;
    }

    public List<ItemRequestWarehouseResponse> getTargetWarehouses() {
        return targetWarehouses;
    }
}
