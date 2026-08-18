package com.vone.simrs.accounting;

/**
 * Baris data NERACA (SC0202 / neraca.zul). Migrasi dari legacy
 * {@code NeracaController.openNeraca()} yang membaca hasil fungsi
 * {@code report.get_neraca_by_date(...)} dan mengambil
 * {@code ms_coa.v_acct_no} / {@code v_acct_name} untuk tiap {@code n_coa_id}.
 *
 * <p>
 * Kolom yang ditampilkan: NO. REKENING, NAMA, SALDO.
 */
public class NeracaRowResponse {

    private final String acctNo;
    private final String acctName;
    private final Double balance;

    public NeracaRowResponse(String acctNo, String acctName, Double balance) {
        this.acctNo = acctNo;
        this.acctName = acctName;
        this.balance = balance;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public String getAcctName() {
        return acctName;
    }

    public Double getBalance() {
        return balance;
    }
}
