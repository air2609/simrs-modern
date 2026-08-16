package com.vone.simrs.master.batchitem;

/**
 * Baris data item untuk screen SCM0055 (UPDATE BATCH ITEM).
 * Mengikuti query {@code ItemDAO.getObatDetail()} pada tabel ms_item.
 */
public class BatchItemRowResponse {

    private final Integer id;
    private final String code;
    private final String name;
    private final Integer buffer;
    private final Integer maxOrder;
    private final Double buyPrice;
    private final Double sellPrice;

    public BatchItemRowResponse(Integer id, String code, String name, Integer buffer,
            Integer maxOrder, Double buyPrice, Double sellPrice) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.buffer = buffer;
        this.maxOrder = maxOrder;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
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

    public Integer getBuffer() {
        return buffer;
    }

    public Integer getMaxOrder() {
        return maxOrder;
    }

    public Double getBuyPrice() {
        return buyPrice;
    }

    public Double getSellPrice() {
        return sellPrice;
    }
}
