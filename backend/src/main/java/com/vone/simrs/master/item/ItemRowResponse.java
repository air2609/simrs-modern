package com.vone.simrs.master.item;

import java.util.List;

/**
 * Baris data item (SCM0038). Mengikuti entity legacy {@code MsItem}
 * (tabel ms_item) beserta relasi group, satuan, dan supplier.
 */
public class ItemRowResponse {

    private final Integer id;
    private final String itemCode;
    private final String itemName;
    private final String barcodeNo;
    private final Integer itemGroupId;
    private final String itemGroupName;
    private final Integer measurementId;
    private final String measurementName;
    private final String itemReturnable;
    private final Short itemType;
    private final Short r;
    private final Short bufferLimit;
    private final Short plafon;
    private final Integer maxOrder;
    private final List<String> suppliers;

    public ItemRowResponse(Integer id, String itemCode, String itemName, String barcodeNo,
            Integer itemGroupId, String itemGroupName, Integer measurementId, String measurementName,
            String itemReturnable, Short itemType, Short r, Short bufferLimit, Short plafon,
            Integer maxOrder, List<String> suppliers) {
        this.id = id;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.barcodeNo = barcodeNo;
        this.itemGroupId = itemGroupId;
        this.itemGroupName = itemGroupName;
        this.measurementId = measurementId;
        this.measurementName = measurementName;
        this.itemReturnable = itemReturnable;
        this.itemType = itemType;
        this.r = r;
        this.bufferLimit = bufferLimit;
        this.plafon = plafon;
        this.maxOrder = maxOrder;
        this.suppliers = suppliers;
    }

    public Integer getId() {
        return id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getBarcodeNo() {
        return barcodeNo;
    }

    public Integer getItemGroupId() {
        return itemGroupId;
    }

    public String getItemGroupName() {
        return itemGroupName;
    }

    public Integer getMeasurementId() {
        return measurementId;
    }

    public String getMeasurementName() {
        return measurementName;
    }

    public String getItemReturnable() {
        return itemReturnable;
    }

    public Short getItemType() {
        return itemType;
    }

    public Short getR() {
        return r;
    }

    public Short getBufferLimit() {
        return bufferLimit;
    }

    public Short getPlafon() {
        return plafon;
    }

    public Integer getMaxOrder() {
        return maxOrder;
    }

    public List<String> getSuppliers() {
        return suppliers;
    }
}
