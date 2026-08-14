package com.vone.simrs.antrian.polidokter;

/**
 * Request simpan/edit data dokter poli (SCM0059). Mengikuti field yang diisi
 * pada form legacy {@code PoliDokterController.save()}.
 */
public class PolyDoctorSaveRequest {

    private Integer id;
    private Integer doctorId;
    private String doctorDescription;
    private String polyStatus;
    private String bookingFlag;
    private Integer maxPatient;
    private String scheduleFrom;
    private String scheduleTo;
    private String unit;
    private String photo;
    private String monSchedule;
    private String tueSchedule;
    private String wedSchedule;
    private String thuSchedule;
    private String friSchedule;
    private String satSchedule;
    private String sunSchedule;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorDescription() {
        return doctorDescription;
    }

    public void setDoctorDescription(String doctorDescription) {
        this.doctorDescription = doctorDescription;
    }

    public String getPolyStatus() {
        return polyStatus;
    }

    public void setPolyStatus(String polyStatus) {
        this.polyStatus = polyStatus;
    }

    public String getBookingFlag() {
        return bookingFlag;
    }

    public void setBookingFlag(String bookingFlag) {
        this.bookingFlag = bookingFlag;
    }

    public Integer getMaxPatient() {
        return maxPatient;
    }

    public void setMaxPatient(Integer maxPatient) {
        this.maxPatient = maxPatient;
    }

    public String getScheduleFrom() {
        return scheduleFrom;
    }

    public void setScheduleFrom(String scheduleFrom) {
        this.scheduleFrom = scheduleFrom;
    }

    public String getScheduleTo() {
        return scheduleTo;
    }

    public void setScheduleTo(String scheduleTo) {
        this.scheduleTo = scheduleTo;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getMonSchedule() {
        return monSchedule;
    }

    public void setMonSchedule(String monSchedule) {
        this.monSchedule = monSchedule;
    }

    public String getTueSchedule() {
        return tueSchedule;
    }

    public void setTueSchedule(String tueSchedule) {
        this.tueSchedule = tueSchedule;
    }

    public String getWedSchedule() {
        return wedSchedule;
    }

    public void setWedSchedule(String wedSchedule) {
        this.wedSchedule = wedSchedule;
    }

    public String getThuSchedule() {
        return thuSchedule;
    }

    public void setThuSchedule(String thuSchedule) {
        this.thuSchedule = thuSchedule;
    }

    public String getFriSchedule() {
        return friSchedule;
    }

    public void setFriSchedule(String friSchedule) {
        this.friSchedule = friSchedule;
    }

    public String getSatSchedule() {
        return satSchedule;
    }

    public void setSatSchedule(String satSchedule) {
        this.satSchedule = satSchedule;
    }

    public String getSunSchedule() {
        return sunSchedule;
    }

    public void setSunSchedule(String sunSchedule) {
        this.sunSchedule = sunSchedule;
    }
}
