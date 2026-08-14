package com.vone.simrs.antrian.polidokter;

/**
 * Baris data dokter poli (SCM0059). Mengikuti kolom yang ditampilkan pada
 * list legacy {@code PoliDokterController} (nama dokter, status, max pasien,
 * unit) plus field detail yang dibutuhkan untuk form edit.
 */
public class PolyDoctorRowResponse {

    private final Integer id;
    private final Integer doctorId;
    private final String doctorCode;
    private final String doctorName;
    private final String polyStatus;
    private final Integer maxPatient;
    private final String unit;
    private final String bookingFlag;
    private final String photo;
    private final String doctorDescription;
    private final String scheduleFrom;
    private final String scheduleTo;
    private final String monSchedule;
    private final String tueSchedule;
    private final String wedSchedule;
    private final String thuSchedule;
    private final String friSchedule;
    private final String satSchedule;
    private final String sunSchedule;

    public PolyDoctorRowResponse(
            Integer id,
            Integer doctorId,
            String doctorCode,
            String doctorName,
            String polyStatus,
            Integer maxPatient,
            String unit,
            String bookingFlag,
            String photo,
            String doctorDescription,
            String scheduleFrom,
            String scheduleTo,
            String monSchedule,
            String tueSchedule,
            String wedSchedule,
            String thuSchedule,
            String friSchedule,
            String satSchedule,
            String sunSchedule) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorCode = doctorCode;
        this.doctorName = doctorName;
        this.polyStatus = polyStatus;
        this.maxPatient = maxPatient;
        this.unit = unit;
        this.bookingFlag = bookingFlag;
        this.photo = photo;
        this.doctorDescription = doctorDescription;
        this.scheduleFrom = scheduleFrom;
        this.scheduleTo = scheduleTo;
        this.monSchedule = monSchedule;
        this.tueSchedule = tueSchedule;
        this.wedSchedule = wedSchedule;
        this.thuSchedule = thuSchedule;
        this.friSchedule = friSchedule;
        this.satSchedule = satSchedule;
        this.sunSchedule = sunSchedule;
    }

    public Integer getId() {
        return id;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getPolyStatus() {
        return polyStatus;
    }

    public Integer getMaxPatient() {
        return maxPatient;
    }

    public String getUnit() {
        return unit;
    }

    public String getBookingFlag() {
        return bookingFlag;
    }

    public String getPhoto() {
        return photo;
    }

    public String getDoctorDescription() {
        return doctorDescription;
    }

    public String getScheduleFrom() {
        return scheduleFrom;
    }

    public String getScheduleTo() {
        return scheduleTo;
    }

    public String getMonSchedule() {
        return monSchedule;
    }

    public String getTueSchedule() {
        return tueSchedule;
    }

    public String getWedSchedule() {
        return wedSchedule;
    }

    public String getThuSchedule() {
        return thuSchedule;
    }

    public String getFriSchedule() {
        return friSchedule;
    }

    public String getSatSchedule() {
        return satSchedule;
    }

    public String getSunSchedule() {
        return sunSchedule;
    }
}
