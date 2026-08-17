package com.vone.simrs.accounting;

/**
 * Baris daftar ACCOUNT PAYABLE (SC0196). Migrasi dari legacy
 * {@code JournalManagerImpl.getAllAp()} yang menampilkan NAMA SUPPLIER,
 * JOURNAL BATCH ID, TOTAL TERHUTANG, dan TANGGAL JATUH TEMPO.
 */
public class AccountPayableRowResponse {

    private final Integer apId;
    private final String supplierName;
    private final String journalBatchId;
    private final Double totalRemaining;
    private final String dueDate;

    public AccountPayableRowResponse(Integer apId, String supplierName, String journalBatchId,
            Double totalRemaining, String dueDate) {
        this.apId = apId;
        this.supplierName = supplierName;
        this.journalBatchId = journalBatchId;
        this.totalRemaining = totalRemaining;
        this.dueDate = dueDate;
    }

    public Integer getApId() {
        return apId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getJournalBatchId() {
        return journalBatchId;
    }

    public Double getTotalRemaining() {
        return totalRemaining;
    }

    public String getDueDate() {
        return dueDate;
    }
}
