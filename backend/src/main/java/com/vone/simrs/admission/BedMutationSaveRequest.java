package com.vone.simrs.admission;

/**
 * Request simpan mutasi kamar (SC0002).
 *
 * <p>
 * Tanpa {@code createdDate} = mutasi baru (check-out bed lama, check-in bed baru).
 * Dengan {@code createdDate} = ubah bed aktif (d_whn_create baris history terpilih).
 */
public class BedMutationSaveRequest {

    private String regNo;
    private Integer bedId;
    private String createdDate;

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public Integer getBedId() {
        return bedId;
    }

    public void setBedId(Integer bedId) {
        this.bedId = bedId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }
}
