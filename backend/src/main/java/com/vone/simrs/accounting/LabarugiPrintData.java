package com.vone.simrs.accounting;

import java.util.ArrayList;
import java.util.List;

/**
 * Data cetak LABA RUGI (tombol CETAK / CETAK BY DATE screen SC0203). Migrasi
 * dari legacy {@code LabarugiController.printLabarugi()} (profit_loss_bydate)
 * dan {@code AccountingReport.openCurrentLabarugi()} (v_profit_loss), keduanya
 * memakai report {@code jasper/laba_rugi.jrxml}.
 */
public class LabarugiPrintData {

    private final String dateParam;
    private final List<Group> groups = new ArrayList<>();

    public LabarugiPrintData(String dateParam) {
        this.dateParam = dateParam;
    }

    public String getDateParam() {
        return dateParam;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void addGroup(Group group) {
        groups.add(group);
    }

    /** Satu grup laba rugi (INCOME / OTHER INCOME / EXPENSE / RINGKASAN). */
    public static class Group {
        private final String caption;
        private final List<Line> lines;

        public Group(String caption, List<Line> lines) {
            this.caption = caption;
            this.lines = lines;
        }

        public String getCaption() {
            return caption;
        }

        public List<Line> getLines() {
            return lines;
        }
    }

    /** Baris akun pada cetakan laba rugi. */
    public static class Line {
        private final String acctName;
        private final Double balance;

        public Line(String acctName, Double balance) {
            this.acctName = acctName;
            this.balance = balance;
        }

        public String getAcctName() {
            return acctName;
        }

        public Double getBalance() {
            return balance;
        }
    }
}
