package com.vone.simrs.accounting;

import java.util.List;

/**
 * Data master untuk screen SCM0050 (FORM ACCT DEFAULT): daftar opsi COA
 * (v_acct_no - v_acct_name) dan nilai default yang sedang aktif dari
 * {@code ms_gim}. Migrasi dari legacy {@code AcctDefaultDataInput.init()}.
 */
public class AcctDefaultMastersResponse {

    private final List<CoaOption> coaOptions;
    private final String inAr;
    private final String outAr;
    private final String ap;
    private final String apPatient;
    private final String pph21;
    private final String miscTrx;
    private final String apStaff;

    public AcctDefaultMastersResponse(List<CoaOption> coaOptions, String inAr, String outAr,
            String ap, String apPatient, String pph21, String miscTrx, String apStaff) {
        this.coaOptions = coaOptions;
        this.inAr = inAr;
        this.outAr = outAr;
        this.ap = ap;
        this.apPatient = apPatient;
        this.pph21 = pph21;
        this.miscTrx = miscTrx;
        this.apStaff = apStaff;
    }

    public List<CoaOption> getCoaOptions() {
        return coaOptions;
    }

    public String getInAr() {
        return inAr;
    }

    public String getOutAr() {
        return outAr;
    }

    public String getAp() {
        return ap;
    }

    public String getApPatient() {
        return apPatient;
    }

    public String getPph21() {
        return pph21;
    }

    public String getMiscTrx() {
        return miscTrx;
    }

    public String getApStaff() {
        return apStaff;
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
