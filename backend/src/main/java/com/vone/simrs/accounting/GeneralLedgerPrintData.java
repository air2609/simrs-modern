package com.vone.simrs.accounting;

import java.util.List;

/**
 * Data cetak GENERAL LEDGER (SC0198, tombol PRINT / PRINT ALL). Migrasi dari
 * legacy {@code GeneralLedgerController.cetakClick()}/{@code cetakAllClick()}
 * + report {@code jasper/general_ledger.jrxml}.
 */
public class GeneralLedgerPrintData {

    private final String dateParam;
    private final List<Line> lines;

    public GeneralLedgerPrintData(String dateParam, List<Line> lines) {
        this.dateParam = dateParam;
        this.lines = lines;
    }

    public String getDateParam() {
        return dateParam;
    }

    public List<Line> getLines() {
        return lines;
    }

    public static class Line {

        private final String acctName;
        private final Long row;
        private final String batchId;
        private final String voucherNo;
        private final String description;
        private final String aplDate;
        private final Double debit;
        private final Double credit;
        private final Double balance;

        public Line(String acctName, Long row, String batchId, String voucherNo,
                String description, String aplDate, Double debit, Double credit, Double balance) {
            this.acctName = acctName;
            this.row = row;
            this.batchId = batchId;
            this.voucherNo = voucherNo;
            this.description = description;
            this.aplDate = aplDate;
            this.debit = debit;
            this.credit = credit;
            this.balance = balance;
        }

        public String getAcctName() {
            return acctName;
        }

        public Long getRow() {
            return row;
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
}
