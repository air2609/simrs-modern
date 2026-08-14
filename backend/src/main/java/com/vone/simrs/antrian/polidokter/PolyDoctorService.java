package com.vone.simrs.antrian.polidokter;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0059 (POLI DOKTER). Mengikuti logika legacy
 * {@code AntrianManagerImpl} + {@code MsPolyDoctorDAO} +
 * {@code DoctorManagerImpl}.
 */
@Service
public class PolyDoctorService {

    private static final short GRUP_DOKTER = 4;

    private final JdbcTemplate jdbcTemplate;

    public PolyDoctorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar dokter poli. Mengikuti {@code MsPolyDoctorDAO.getAll(cari)} yang
     * mencari berdasarkan nama staff.
     */
    public List<PolyDoctorRowResponse> getPolyDoctors(String search) {
        String keyword = "%" + (search == null ? "" : search.trim()) + "%";
        String sql = "select pd.id, pd.doctor_id, st.v_staff_code, st.v_staff_name, "
                + "pd.poly_status, pd.max_patient, pd.unit, pd.booking_flag, pd.photo, "
                + "pd.doctor_description, pd.schedule_from, pd.schedule_to, "
                + "pd.mon_schedule, pd.tue_schedule, pd.wed_schedule, pd.thu_schedule, "
                + "pd.fri_schedule, pd.sat_schedule, pd.sun_schedule "
                + "from ms_poly_doctor pd "
                + "join ms_staff st on st.n_staff_id = pd.doctor_id "
                + "where st.v_staff_name like ? "
                + "order by st.v_staff_name";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new PolyDoctorRowResponse(
                resultSet.getInt("id"),
                resultSet.getInt("doctor_id"),
                resultSet.getString("v_staff_code"),
                resultSet.getString("v_staff_name"),
                resultSet.getString("poly_status"),
                resultSet.getObject("max_patient") == null ? null : resultSet.getInt("max_patient"),
                resultSet.getString("unit"),
                resultSet.getString("booking_flag"),
                resultSet.getString("photo"),
                resultSet.getString("doctor_description"),
                toTimeString(resultSet.getTime("schedule_from")),
                toTimeString(resultSet.getTime("schedule_to")),
                resultSet.getString("mon_schedule"),
                resultSet.getString("tue_schedule"),
                resultSet.getString("wed_schedule"),
                resultSet.getString("thu_schedule"),
                resultSet.getString("fri_schedule"),
                resultSet.getString("sat_schedule"),
                resultSet.getString("sun_schedule")),
                keyword);
    }

    /**
     * Pencarian dokter (grup dokter). Mengikuti {@code MsDoctorDAO.searchDocttor}
     * + {@code DoctorManagerImpl.searchDoctor} (unit dari ms_staff_in_unit).
     */
    public List<DoctorOptionResponse> searchDoctors(String code, String name) {
        String codeKeyword = "%" + (code == null ? "" : code.trim().toUpperCase(Locale.ROOT)) + "%";
        String nameKeyword = "%" + (name == null ? "" : name.trim().toUpperCase(Locale.ROOT)) + "%";

        String sql = "select st.n_staff_id, st.v_staff_code, st.v_staff_name, "
                + "coalesce(string_agg(u.v_unit_name, ',' order by u.v_unit_name), '') as unit "
                + "from ms_doctor dr "
                + "join ms_staff st on st.n_staff_id = dr.n_staff_id "
                + "left join ms_staff_in_unit siu on siu.n_staff_id = st.n_staff_id "
                + "left join ms_unit u on u.n_unit_id = siu.n_unit_id "
                + "where upper(st.v_staff_code) like ? "
                + "and upper(st.v_staff_name) like ? "
                + "and dr.n_msgroup_id = ? "
                + "and st.d_staff_fired_date is null "
                + "group by st.n_staff_id, st.v_staff_code, st.v_staff_name "
                + "order by st.v_staff_name";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new DoctorOptionResponse(
                resultSet.getInt("n_staff_id"),
                resultSet.getString("v_staff_code"),
                resultSet.getString("v_staff_name"),
                resultSet.getString("unit")),
                codeKeyword,
                nameKeyword,
                GRUP_DOKTER);
    }

    /**
     * Opsi master untuk form: status poli dan booking flag.
     */
    public PolyDoctorMastersResponse getMasters() {
        List<String> statusOptions = Arrays.asList("Aktif", "Non Aktif");
        List<String> bookingOptions = Arrays.asList("Y", "T");
        return new PolyDoctorMastersResponse(statusOptions, bookingOptions);
    }

    /**
     * Simpan / update dokter poli. Mengikuti {@code MsPolyDoctorDAO.save}.
     */
    @Transactional
    public void save(PolyDoctorSaveRequest request, String username) {
        if (request.getDoctorId() == null) {
            throw new IllegalArgumentException("Dokter harus dipilih.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId("ms_poly_doctor_id_seq");
            jdbcTemplate.update(
                    "insert into ms_poly_doctor (id, doctor_id, photo, poly_status, booking_flag, "
                            + "max_patient, schedule_from, schedule_to, unit, doctor_description, "
                            + "mon_schedule, tue_schedule, wed_schedule, thu_schedule, fri_schedule, "
                            + "sat_schedule, sun_schedule) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    id,
                    request.getDoctorId(),
                    request.getPhoto(),
                    request.getPolyStatus(),
                    request.getBookingFlag(),
                    request.getMaxPatient(),
                    toTime(request.getScheduleFrom()),
                    toTime(request.getScheduleTo()),
                    request.getUnit(),
                    request.getDoctorDescription(),
                    request.getMonSchedule(),
                    request.getTueSchedule(),
                    request.getWedSchedule(),
                    request.getThuSchedule(),
                    request.getFriSchedule(),
                    request.getSatSchedule(),
                    request.getSunSchedule());
        } else {
            jdbcTemplate.update(
                    "update ms_poly_doctor set doctor_id = ?, photo = ?, poly_status = ?, "
                            + "booking_flag = ?, max_patient = ?, schedule_from = ?, schedule_to = ?, "
                            + "unit = ?, doctor_description = ?, mon_schedule = ?, tue_schedule = ?, "
                            + "wed_schedule = ?, thu_schedule = ?, fri_schedule = ?, sat_schedule = ?, "
                            + "sun_schedule = ? where id = ?",
                    request.getDoctorId(),
                    request.getPhoto(),
                    request.getPolyStatus(),
                    request.getBookingFlag(),
                    request.getMaxPatient(),
                    toTime(request.getScheduleFrom()),
                    toTime(request.getScheduleTo()),
                    request.getUnit(),
                    request.getDoctorDescription(),
                    request.getMonSchedule(),
                    request.getTueSchedule(),
                    request.getWedSchedule(),
                    request.getThuSchedule(),
                    request.getFriSchedule(),
                    request.getSatSchedule(),
                    request.getSunSchedule(),
                    id);
        }
    }

    /**
     * Hapus dokter poli. Mengikuti {@code MsPolyDoctorDAO.deletePolyDoctor}.
     */
    @Transactional
    public void delete(Integer id) {
        jdbcTemplate.update("delete from ms_poly_doctor where id = ?", id);
    }

    /**
     * Jadwal praktek dokter per bulan. Mengikuti
     * {@code MsPolyDoctorDAO.getSchedules}.
     */
    public List<DoctorScheduleResponse> getSchedules(Integer doctorId, String month) {
        String sql = "select s.id, s.doctor_id, s.schedule, s.schedule_month "
                + "from tb_doctor_schedules s "
                + "where s.doctor_id = ? and s.schedule_month = ? "
                + "order by s.schedule";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new DoctorScheduleResponse(
                resultSet.getInt("id"),
                resultSet.getInt("doctor_id"),
                resultSet.getDate("schedule") == null ? null : resultSet.getDate("schedule").toLocalDate().toString(),
                resultSet.getString("schedule_month")),
                doctorId,
                month);
    }

    /**
     * Simpan jadwal praktek (bulk). Mengikuti
     * {@code MsPolyDoctorDAO.saveSchedules}.
     */
    @Transactional
    public void saveSchedules(Integer doctorId, List<String> dates, String username) {
        if (doctorId == null || dates == null || dates.isEmpty()) {
            return;
        }
        String actor = normalizeActor(username);
        for (String date : dates) {
            LocalDate localDate = LocalDate.parse(date);
            String month = localDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));
            Integer id = nextId("tb_doctor_schedules_id_seq");
            jdbcTemplate.update(
                    "insert into tb_doctor_schedules (id, doctor_id, schedule, created_at, created_by, schedule_month) "
                            + "values (?, ?, ?, now(), ?, ?)",
                    id,
                    doctorId,
                    java.sql.Date.valueOf(localDate),
                    actor,
                    month);
        }
    }

    /**
     * Hapus jadwal praktek per tanggal. Mengikuti
     * {@code MsPolyDoctorDAO.deleteSchedule}.
     */
    @Transactional
    public void deleteSchedule(Integer doctorId, String date) {
        LocalDate localDate = LocalDate.parse(date);
        jdbcTemplate.update(
                "delete from tb_doctor_schedules where doctor_id = ? and schedule = ?",
                doctorId,
                java.sql.Date.valueOf(localDate));
    }

    private Integer nextId(String sequence) {
        return jdbcTemplate.queryForObject("select nextval('" + sequence + "')", Integer.class);
    }

    private String toTimeString(Time time) {
        if (time == null) {
            return null;
        }
        return time.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private Time toTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Time.valueOf(LocalTime.parse(value.trim()));
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
