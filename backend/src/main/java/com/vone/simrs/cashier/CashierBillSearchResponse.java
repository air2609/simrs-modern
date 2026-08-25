package com.vone.simrs.cashier;

/**
 * Hasil pencarian kwitansi (tb_patient_bill) untuk re-print. Migrasi dari
 * legacy {@code CashierManagerImpl.getPatientBills()} + {@code CashierDAO.getPatientBill()}.
 */
public class CashierBillSearchResponse {

    private final Integer billId;
    private final String billCode;
    private final String nameOnBill;
    private final String date;

    public CashierBillSearchResponse(Integer billId, String billCode, String nameOnBill,
            String date) {
        this.billId = billId;
        this.billCode = billCode;
        this.nameOnBill = nameOnBill;
        this.date = date;
    }

    public Integer getBillId() {
        return billId;
    }

    public String getBillCode() {
        return billCode;
    }

    public String getNameOnBill() {
        return nameOnBill;
    }

    public String getDate() {
        return date;
    }
}
