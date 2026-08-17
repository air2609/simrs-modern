package com.vone.simrs.accounting;

/**
 * Baris data GENERAL LEDGER (SC0198). Migrasi dari legacy
 * {@code GeneralLedgerController.getGLAll()}/{@code coaClick()} yang membaca
 * {@code report.func_gl_all_bydate(...)} / {@code func_gl_bydate_arif(...)}.
 */
public class GeneralLedgerRowResponse {

    private final String acctNo;
    private final String acctName;
    private final String batchId;
    private final String voucherNo;
    private final String description;
    private final String aplDate;
    private final Double debit;
    private final Double credit;
    private final Double balance;

    public GeneralLedgerRowResponse(String acctNo, String acctName, String batchId,
            String voucherNo, String description, String aplDate, Double debit, Double credit,
            Double balance) {
        this.acctNo = acctNo;
        this.acctName = acctName;
        this.batchId = batchId;
        this.voucherNo = voucherNo;
        this.description = description;
        this.aplDate = aplDate;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public String getAcctName() {
        return acctName;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public String getDescription() {
        return description;
    }

    public String getAplDate() {
        return aplDate;
    }

    public Double getDebit() {
        return debit;
    }

    public Double getCredit() {
        return credit;
    }

    public Double getBalance() {
        return balance;
    }
}
