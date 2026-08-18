package com.vone.simrs.cashier;

/**
 * Baris rekap obat (SC0208). Migrasi dari legacy
 * {@code CashierDAO.getItemTrx(reg, type)} + {@code getRetur(reg, itemId)}.
 */
public class RekapObatRowResponse {

    private final Integer itemId;
    private final String code;
    private final String name;
    private final String drugType;
    private final Integer jmlTrx;
    private final Double totTrx;
    private final Integer jmlRetur;
    private final Double totRetur;
    private final Double balance;

    public RekapObatRowResponse(Integer itemId, String code, String name, String drugType,
            Integer jmlTrx, Double totTrx, Integer jmlRetur, Double totRetur, Double balance) {
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.drugType = drugType;
        this.jmlTrx = jmlTrx;
        this.totTrx = totTrx;
        this.jmlRetur = jmlRetur;
        this.totRetur = totRetur;
        this.balance = balance;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDrugType() {
        return drugType;
    }

    public Integer getJmlTrx() {
        return jmlTrx;
    }

    public Double getTotTrx() {
        return totTrx;
    }

    public Integer getJmlRetur() {
        return jmlRetur;
    }

    public Double getTotRetur() {
        return totRetur;
    }

    public Double getBalance() {
        return balance;
    }
}
