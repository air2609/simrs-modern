package com.vone.simrs.accounting;

/**
 * Baris data TRIAL BALANCE (SC0207). Migrasi dari legacy
 * {@code TrialBalanceController.getGLAll()} yang membaca hasil fungsi
 * {@code report.get_trial_balance(...)} berformat "NAMA[NO_AKUN]".
 */
public class TrialBalanceRowResponse {

    private final String acctNo;
    private final String acctName;
    private final Double debit;
    private final Double credit;
    private final Double balance;

    public TrialBalanceRowResponse(String acctNo, String acctName, Double debit, Double credit,
            Double balance) {
        this.acctNo = acctNo;
        this.acctName = acctName;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public String getAcctName() {
        return acctName;
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
