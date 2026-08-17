package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Data untuk cetak PDF PURCHASE ORDER (SC0193, tombol CETAK). Migrasi dari
 * legacy {@code POController.cetakPO()} + report
 * {@code jasper/orderPembelian.jrxml}.
 */
public class PurchaseOrderPrintData {

    private final String poCode;
    private final String supplier;
    private final String requestor;
    private final List<Line> lines;

    public PurchaseOrderPrintData(String poCode, String supplier, String requestor,
            List<Line> lines) {
        this.poCode = poCode;
        this.supplier = supplier;
        this.requestor = requestor;
        this.lines = lines;
    }

    public String getPoCode() {
        return poCode;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getRequestor() {
        return requestor;
    }

    public List<Line> getLines() {
        return lines;
    }

    public static class Line {

        private final String item;
        private final String satuanRequest;
        private final Integer quantityRequest;
        private final String satuanRealisasi;
        private final Integer quantityRealisasi;
        private final Double hargaSatuan;
        private final Double subtotal;

        public Line(String item, String satuanRequest, Integer quantityRequest,
                String satuanRealisasi, Integer quantityRealisasi, Double hargaSatuan,
                Double subtotal) {
            this.item = item;
            this.satuanRequest = satuanRequest;
            this.quantityRequest = quantityRequest;
            this.satuanRealisasi = satuanRealisasi;
            this.quantityRealisasi = quantityRealisasi;
            this.hargaSatuan = hargaSatuan;
            this.subtotal = subtotal;
        }

        public String getItem() {
            return item;
        }

        public String getSatuanRequest() {
            return satuanRequest;
        }

        public Integer getQuantityRequest() {
            return quantityRequest;
        }

        public String getSatuanRealisasi() {
            return satuanRealisasi;
        }

        public Integer getQuantityRealisasi() {
            return quantityRealisasi;
        }

        public Double getHargaSatuan() {
            return hargaSatuan;
        }

        public Double getSubtotal() {
            return subtotal;
        }
    }
}
