package com.vone.simrs.accounting;

/**
 * Baris data LABA RUGI (SC0203 / labaRugi.zul). Migrasi dari legacy
 * {@code LabarugiController.cariClick()} yang membaca hasil fungsi
 * {@code report.profit_loss_bydate(...)} dan menampilkan kolom
 * NO. REKENING, NAMA, JUMLAH.
 */
public class LabarugiRowResponse {

    private final String acctNo;
    private final String acctName;
    private final Double balance;

    public LabarugiRowResponse(String acctNo, String acctName, Double balance) {
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
