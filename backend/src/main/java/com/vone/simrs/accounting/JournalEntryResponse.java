package com.vone.simrs.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JournalEntryResponse {

    @JsonProperty("journalId")
    private final Integer journalId;

    @JsonProperty("journalBatchId")
    private final String journalBatchId;

    @JsonProperty("voucherNo")
    private final String voucherNo;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("debit")
    private final Double debit;

    @JsonProperty("credit")
    private final Double credit;

    @JsonProperty("aplDate")
    private final String aplDate;

    @JsonProperty("coaId")
    private final Integer coaId;

    @JsonProperty("accountNo")
    private final String accountNo;

    @JsonProperty("accountName")
    private final String accountName;

    public JournalEntryResponse(
            Integer journalId,
            String journalBatchId,
            String voucherNo,
            String description,
            Double debit,
            Double credit,
            String aplDate,
            Integer coaId,
            String accountNo,
            String accountName
    ) {
        this.journalId = journalId;
        this.journalBatchId = journalBatchId;
        this.voucherNo = voucherNo;
        this.description = description;
        this.debit = debit;
        this.credit = credit;
        this.aplDate = aplDate;
        this.coaId = coaId;
        this.accountNo = accountNo;
        this.accountName = accountName;
    }

    public Integer getJournalId() {
        return journalId;
    }

    public String getJournalBatchId() {
        return journalBatchId;
    }

    public String getVoucherNo() {
        return voucherNo;
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

    public Integer getCoaId() {
        return coaId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getAccountName() {
        return accountName;
    }
}
