package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Header + detail OPP (status APPROVED) yang dipilih pada bandbox NO. OPP
 * screen SC0193 untuk dijadikan ORDER PEMBELIAN. Migrasi dari legacy
 * {@code POManagerImpl.redraw()} yang mengisi listbox DAFTAR ORDER PEMBELIAN
 * dengan kolom KODE, KETERANGAN, ORD/A, SATUAN, ORD/S, HRG SAT, JLH ORD.,
 * SAT ORD., BONUS, DISKON, dan SUBTOTAL.
 */
public class PurchaseOrderOppDetailResponse {

    private final String prCode;
    private final Integer supplierId;
    private final String supplierCode;
    private final String supplierName;
    private final String supplierAddress;
    private final String supplierTelp;
    private final List<Line> lines;

    public PurchaseOrderOppDetailResponse(String prCode, Integer supplierId, String supplierCode,
            String supplierName, String supplierAddress, String supplierTelp, List<Line> lines) {
        this.prCode = prCode;
        this.supplierId = supplierId;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.supplierAddress = supplierAddress;
        this.supplierTelp = supplierTelp;
        this.lines = lines;
    }

    public String getPrCode() {
        return prCode;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getSupplierAddress() {
        return supplierAddress;
    }

    public String getSupplierTelp() {
        return supplierTelp;
    }

    public List<Line> getLines() {
        return lines;
    }

    public static class Line {

        private final Integer itemId;
        private final String itemCode;
        private final String itemName;
        private final Integer qtyRequested;
        private final String measurementCode;
        private final String measurementEndQuantify;
        private final Integer measurementId;
        private final Integer qtyRemaining;
        private final Double lastPrice;

        public Line(Integer itemId, String itemCode, String itemName, Integer qtyRequested,
                String measurementCode, String measurementEndQuantify, Integer measurementId,
                Integer qtyRemaining, Double lastPrice) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.qtyRequested = qtyRequested;
            this.measurementCode = measurementCode;
            this.measurementEndQuantify = measurementEndQuantify;
            this.measurementId = measurementId;
            this.qtyRemaining = qtyRemaining;
            this.lastPrice = lastPrice;
        }

        public Integer getItemId() {
            return itemId;
        }

        public String getItemCode() {
            return itemCode;
        }

        public String getItemName() {
            return itemName;
        }

        public Integer getQtyRequested() {
            return qtyRequested;
        }

        public String getMeasurementCode() {
            return measurementCode;
        }

        public String getMeasurementEndQuantify() {
            return measurementEndQuantify;
        }

        public Integer getMeasurementId() {
            return measurementId;
        }

        public Integer getQtyRemaining() {
            return qtyRemaining;
        }

        public Double getLastPrice() {
            return lastPrice;
        }
    }
}
