package com.vone.simrs.accounting;

/**
 * Baris detail jurnal pada dialog LIHAT JOURNAL / LIHAT HISTORY PEMBAYARAN
 * (SC0196). Migrasi dari legacy
 * {@code JournalManagerImpl.getJournalByBatch()} dan
 * {@code getJournalByApId()}.
 */
public class AccountPayableJournalResponse {

    private final String batchId;
    private final String voucherNo;
    private final String acctName;
    private final String description;
    private final Double debit;
    private final Double credit;
    private final String aplDate;

    public AccountPayableJournalResponse(String batchId, String voucherNo, String acctName,
            String description, Double debit, Double credit, String aplDate) {
        this.batchId = batchId;
        this.voucherNo = voucherNo;
        this.acctName = acctName;
        this.description = description;
        this.debit = debit;
        this.credit = credit;
        this.aplDate = aplDate;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public String getAcctName() {
        return acctName;
    }

    public String getDescription() {
        return description;
    }

    public Double getDebit() {
        return debit;
    }

    public Double getCredit() {
        return credit;
    }

    public String getAplDate() {
        return aplDate;
    }
}
