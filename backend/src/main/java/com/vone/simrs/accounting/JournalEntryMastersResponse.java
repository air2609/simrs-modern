package com.vone.simrs.accounting;

import java.util.List;

/**
 * Data master untuk screen SC0199 (MANUAL JOURNAL ENTRY): daftar opsi COA.
 * Migrasi dari legacy {@code CoaController.getCoaForSelect()} pada
 * {@code JournalEntryController.init()}.
 */
public class JournalEntryMastersResponse {

    private final List<CoaOption> coaOptions;

    public JournalEntryMastersResponse(List<CoaOption> coaOptions) {
        this.coaOptions = coaOptions;
    }

    public List<CoaOption> getCoaOptions() {
        return coaOptions;
    }

    public static class CoaOption {

        private final Integer coaId;
        private final String acctNo;
        private final String acctName;

        public CoaOption(Integer coaId, String acctNo, String acctName) {
            this.coaId = coaId;
            this.acctNo = acctNo;
            this.acctName = acctName;
        }

        public Integer getCoaId() {
            return coaId;
        }

        public String getAcctNo() {
            return acctNo;
        }

        public String getAcctName() {
            return acctName;
        }
    }
}
