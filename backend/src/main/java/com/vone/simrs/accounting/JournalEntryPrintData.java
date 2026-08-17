package com.vone.simrs.accounting;

import java.util.List;

/**
 * Data cetak MANUAL JOURNAL (SC0199, tombol CETAK). Migrasi dari legacy
 * {@code JournalEntryController.printToPdf()} + report
 * {@code jasper/manual_jurnal.jrxml}.
 */
public class JournalEntryPrintData {

    private final String voucherNo;
    private final String inputBy;
    private final String inputDate; // dd/MM/yyyy
    private final List<Line> lines;

    public JournalEntryPrintData(String voucherNo, String inputBy, String inputDate,
            List<Line> lines) {
        this.voucherNo = voucherNo;
        this.inputBy = inputBy;
        this.inputDate = inputDate;
        this.lines = lines;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public String getInputBy() {
        return inputBy;
    }

    public String getInputDate() {
        return inputDate;
    }

    public List<Line> getLines() {
        return lines;
    }

    public static class Line {

        private final String description;
        private final String account;
        private final Double debit;
        private final Double credit;

        public Line(String description, String account, Double debit, Double credit) {
            this.description = description;
            this.account = account;
            this.debit = debit;
            this.credit = credit;
        }

        public String getDescription() {
            return description;
        }

        public String getAccount() {
            return account;
        }

        public Double getDebit() {
            return debit;
        }

        public Double getCredit() {
            return credit;
        }
    }
}
