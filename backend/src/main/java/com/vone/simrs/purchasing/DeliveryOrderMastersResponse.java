package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Data master untuk screen SC0195 (PENERIMAAN BARANG): daftar gudang
 * (LOKASI GUDANG) untuk staff yang login dan nama user (DITERIMA OLEH).
 * Migrasi dari legacy {@code DOController.init()} yang memakai
 * {@code WarehouseManager.getWhouseByStaffId()}.
 */
public class DeliveryOrderMastersResponse {

    private final List<Warehouse> warehouses;
    private final String recBy;

    public DeliveryOrderMastersResponse(List<Warehouse> warehouses, String recBy) {
        this.warehouses = warehouses;
        this.recBy = recBy;
    }

    public List<Warehouse> getWarehouses() {
        return warehouses;
    }

    public String getRecBy() {
        return recBy;
    }

    public static class Warehouse {

        private final Integer warehouseId;
        private final String warehouseCode;
        private final String warehouseName;

        public Warehouse(Integer warehouseId, String warehouseCode, String warehouseName) {
            this.warehouseId = warehouseId;
            this.warehouseCode = warehouseCode;
            this.warehouseName = warehouseName;
        }

        public Integer getWarehouseId() {
            return warehouseId;
        }

        public String getWarehouseCode() {
            return warehouseCode;
        }

        public String getWarehouseName() {
            return warehouseName;
        }
    }
}
