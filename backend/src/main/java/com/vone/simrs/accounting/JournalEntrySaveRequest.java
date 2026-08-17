package com.vone.simrs.accounting;

import java.util.List;

/**
 * Request simpan MANUAL JOURNAL ENTRY (SC0199). Migrasi dari legacy
 * {@code JournalEntryController.saveClick()}.
 */
public class JournalEntrySaveRequest {

    private String aplDate; // ISO yyyy-MM-dd
    private String voucherNo;
    private String description;
    private List<Line> lines;

    public String getAplDate() {
        return aplDate;
    }

    public void setAplDate(String aplDate) {
        this.aplDate = aplDate;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public static class Line {

        private Integer coaId;
        private Double debit;
        private Double credit;

        public Integer getCoaId() {
            return coaId;
        }

        public void setCoaId(Integer coaId) {
            this.coaId = coaId;
        }

        public Double getDebit() {
            return debit;
        }

        public void setDebit(Double debit) {
            this.debit = debit;
        }

        public Double getCredit() {
            return credit;
        }

        public void setCredit(Double credit) {
            this.credit = credit;
        }
    }
}
