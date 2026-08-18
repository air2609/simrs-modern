package com.vone.simrs.admission;

/**
 * Request batal pendaftaran rawat inap (restore registrasi lama + bed).
 */
public class RanapCancelRequest {

    private Integer newRegId;
    private Integer oldRegId;
    private Integer bedId;

    public Integer getNewRegId() {
        return newRegId;
    }

    public void setNewRegId(Integer newRegId) {
        this.newRegId = newRegId;
    }

    public Integer getOldRegId() {
        return oldRegId;
    }

    public void setOldRegId(Integer oldRegId) {
        this.oldRegId = oldRegId;
    }

    public Integer getBedId() {
        return bedId;
    }

    public void setBedId(Integer bedId) {
        this.bedId = bedId;
    }
}
