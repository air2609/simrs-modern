package com.vone.simrs.antrian.polidokter;

/**
 * Jadwal praktek dokter (SCM0059). Mengikuti entity legacy
 * {@code TbDoctorSchedule} (tabel tb_doctor_schedules).
 */
public class DoctorScheduleResponse {

    private final Integer id;
    private final Integer doctorId;
    private final String schedule;
    private final String month;

    public DoctorScheduleResponse(Integer id, Integer doctorId, String schedule, String month) {
        this.id = id;
        this.doctorId = doctorId;
        this.schedule = schedule;
        this.month = month;
    }

    public Integer getId() {
        return id;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getMonth() {
        return month;
    }
}
