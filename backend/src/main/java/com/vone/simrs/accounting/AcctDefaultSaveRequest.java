package com.vone.simrs.accounting;

/**
 * Request simpan FORM ACCT DEFAULT (SCM0050): nilai v_acct_no untuk masing
 * masing kunci GIM. Migrasi dari legacy {@code AcctDefaultDataInput.doSave()}.
 *
 * <p>
 * Field yang kosong diabaikan (tidak mengubah nilai GIM yang ada), mengikuti
 * perilaku legacy yang melewati nilai kosong/placeholder.
 */
public class AcctDefaultSaveRequest {

    private String inAr;
    private String outAr;
    private String ap;
    private String apPatient;
    private String pph21;
    private String miscTrx;
    private String apStaff;

    public String getInAr() {
        return inAr;
    }

    public void setInAr(String inAr) {
        this.inAr = inAr;
    }

    public String getOutAr() {
        return outAr;
    }

    public void setOutAr(String outAr) {
        this.outAr = outAr;
    }

    public String getAp() {
        return ap;
    }

    public void setAp(String ap) {
        this.ap = ap;
    }

    public String getApPatient() {
        return apPatient;
    }

    public void setApPatient(String apPatient) {
        this.apPatient = apPatient;
    }

    public String getPph21() {
        return pph21;
    }

    public void setPph21(String pph21) {
        this.pph21 = pph21;
    }

    public String getMiscTrx() {
        return miscTrx;
    }

    public void setMiscTrx(String miscTrx) {
        this.miscTrx = miscTrx;
    }

    public String getApStaff() {
        return apStaff;
    }

    public void setApStaff(String apStaff) {
        this.apStaff = apStaff;
    }
}
