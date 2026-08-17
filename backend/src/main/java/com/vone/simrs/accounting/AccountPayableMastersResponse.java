package com.vone.simrs.accounting;

import java.util.List;

/**
 * Data master untuk dialog PEMBAYARAN AP (SC0196): daftar opsi COA (VIA ACCT).
 * Migrasi dari legacy {@code CoaController.getCoaForSelect(viaList, COA_ALL)}.
 */
public class AccountPayableMastersResponse {

    private final List<JournalEntryMastersResponse.CoaOption> coaOptions;

    public AccountPayableMastersResponse(List<JournalEntryMastersResponse.CoaOption> coaOptions) {
        this.coaOptions = coaOptions;
    }

    public List<JournalEntryMastersResponse.CoaOption> getCoaOptions() {
        return coaOptions;
    }
}
