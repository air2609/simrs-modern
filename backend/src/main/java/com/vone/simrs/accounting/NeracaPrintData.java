package com.vone.simrs.accounting;

import java.util.ArrayList;
import java.util.List;

/**
 * Data cetak NERACA (tombol CETAK screen SC0202). Migrasi dari legacy
 * {@code AccountingReport.openCurrentNeraca()} yang memakai query
 * {@code select * from report.balance_sheet()}.
 */
public class NeracaPrintData {

    private final List<Group> groups = new ArrayList<>();

    public List<Group> getGroups() {
        return groups;
    }

    public void addGroup(Group group) {
        groups.add(group);
    }

    /** Satu grup neraca (AKTIVA / HUTANG / EQUITY) beserta total saldonya. */
    public static class Group {
        private final String caption;
        private final double total;
        private final List<Line> lines;

        public Group(String caption, double total, List<Line> lines) {
            this.caption = caption;
            this.total = total;
            this.lines = lines;
        }

        public String getCaption() {
            return caption;
        }

        public double getTotal() {
            return total;
        }

        public List<Line> getLines() {
            return lines;
        }
    }

    /** Baris akun pada cetakan neraca. */
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
