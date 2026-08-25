package com.vone.simrs.cashier;

import java.util.List;

/**
 * Detail kwitansi (tb_patient_bill) beserta seluruh nota dan baris transaksinya.
 * Migrasi dari legacy {@code CashierManagerImpl.getBillDetil()} — dipakai untuk
 * menampilkan ulang isi kwitansi saat re-print.
 */
public class CashierBillDetailResponse {

    private final Integer billId;
    private final String billCode;
    private final String date;
    private final String nameOnBill;
    private final String addrOnBill;
    private final Double subTotal;
    private final Double totalPaid;
    private final Double discount;
    private final Double tax;
    private final Double cashAmount;
    private final Double depositAmount;
    private final Double nonCashAmount;

    private final String mrCode;
    private final String patientName;
    private final String patientTypeName;
    private final String address;
    private final String bed;
    private final Double depositBalance;

    private final String noteNos;               // nomor nota dipisah ";"
    private final List<CashierNoteLineResponse> lines;

    public CashierBillDetailResponse(Integer billId, String billCode, String date,
            String nameOnBill, String addrOnBill, Double subTotal, Double totalPaid, Double discount,
            Double tax, Double cashAmount, Double depositAmount, Double nonCashAmount,
            String mrCode, String patientName, String patientTypeName, String address,
            String bed, Double depositBalance, String noteNos,
            List<CashierNoteLineResponse> lines) {
        this.billId = billId;
        this.billCode = billCode;
        this.date = date;
        this.nameOnBill = nameOnBill;
        this.addrOnBill = addrOnBill;
        this.subTotal = subTotal;
        this.totalPaid = totalPaid;
        this.discount = discount;
        this.tax = tax;
        this.cashAmount = cashAmount;
        this.depositAmount = depositAmount;
        this.nonCashAmount = nonCashAmount;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.patientTypeName = patientTypeName;
        this.address = address;
        this.bed = bed;
        this.depositBalance = depositBalance;
        this.noteNos = noteNos;
        this.lines = lines;
    }

    public Integer getBillId() {
        return billId;
    }

    public String getBillCode() {
        return billCode;
    }

    public String getDate() {
        return date;
    }

    public Double getSubTotal() {
        return subTotal;
    }

    public String getNameOnBill() {
        return nameOnBill;
    }

    public String getAddrOnBill() {
        return addrOnBill;
    }

    public Double getTotalPaid() {
        return totalPaid;
    }

    public Double getDiscount() {
        return discount;
    }

    public Double getTax() {
        return tax;
    }

    public Double getCashAmount() {
        return cashAmount;
    }

    public Double getDepositAmount() {
        return depositAmount;
    }

    public Double getNonCashAmount() {
        return nonCashAmount;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientTypeName() {
        return patientTypeName;
    }

    public String getAddress() {
        return address;
    }

    public String getBed() {
        return bed;
    }

    public Double getDepositBalance() {
        return depositBalance;
    }

    public String getNoteNos() {
        return noteNos;
    }

    public List<CashierNoteLineResponse> getLines() {
        return lines;
    }
}
