package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0013 (LAPORAN PENDAPATAN DOKTER / laporanPendapatanDokter.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LaporanPendapatanDokter} + {@code NoteManagerImpl.getPendapatanDokter()}
 * / {@code getDoctorReportAll()} + {@code NoteDAO} — tiga tipe laporan:
 * PD (fungsi {@code report.get_doctor_report}), OBAT ({@code getNoteByDokter}),
 * ALL (rekapitulasi per dokter).
 */
@Service
public class PendapatanDokterService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public PendapatanDokterService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Pencarian dokter (grup 4). Migrasi {@code MsDoctorDAO.searchDocttor()}. */
    public List<DoctorOptionResponse> searchDoctors(String code, String name) {
        String codePattern = "%" + (hasText(code) ? code.trim().toUpperCase() : "") + "%";
        String namePattern = "%" + (hasText(name) ? name.trim().toUpperCase() : "") + "%";
        String sql = "select staff.n_staff_id, staff.v_staff_code, staff.v_staff_name, "
                + "string_agg(distinct unt.v_unit_name, ';' order by unt.v_unit_name) as units "
                + "from ms_doctor dr "
                + "join ms_staff staff on staff.n_staff_id = dr.n_staff_id "
                + "left join ms_staff_in_unit siu on siu.n_staff_id = staff.n_staff_id "
                + "left join ms_unit unt on unt.n_unit_id = siu.n_unit_id "
                + "where upper(staff.v_staff_code) like ? "
                + "and upper(staff.v_staff_name) like ? "
                + "and dr.n_msgroup_id = 4 "
                + "and staff.d_staff_fired_date is null "
                + "group by staff.n_staff_id, staff.v_staff_code, staff.v_staff_name "
                + "order by staff.v_staff_name";
        return jdbcTemplate.query(sql, new Object[] { codePattern, namePattern },
                (resultSet, rowNum) -> new DoctorOptionResponse(
                        resultSet.getInt("n_staff_id"),
                        resultSet.getString("v_staff_code"),
                        resultSet.getString("v_staff_name"),
                        resultSet.getString("units") == null ? "" : resultSet.getString("units")));
    }

    /**
     * Laporan pendapatan dokter.
     *
     * @param tipe        PD, OBAT, atau ALL
     * @param staffId     dokter (wajib utk PD/OBAT)
     * @param from        tanggal awal (yyyy-MM-dd)
     * @param to          tanggal akhir (yyyy-MM-dd)
     * @param patientType BPJS / NONBPJS / ALL
     */
    public PendapatanDokterResponse getReport(String tipe, Integer staffId, String from, String to,
            String patientType) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("Kedua tanggal harus diisi....!");
        }
        String reportType = hasText(tipe) ? tipe.toUpperCase() : "PD";
        String pasType = hasText(patientType) ? patientType.toUpperCase() : "ALL";
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        if ("ALL".equalsIgnoreCase(reportType)) {
            List<PendapatanDokterAllRowResponse> allRows = getDoctorReportAll(fromDate, toDate, pasType);
            return new PendapatanDokterResponse("ALL", 0, new ArrayList<>(), allRows);
        }

        if (staffId == null) {
            throw new IllegalArgumentException("Pilih data dokter terlebih dahulu");
        }
        if ("OBAT".equalsIgnoreCase(reportType)) {
            return getPenjualanObat(staffId, fromDate, toDate, pasType);
        }
        return getPendapatanTindakan(staffId, fromDate, toDate, pasType);
    }

    /** PD — migrasi {@code NoteDAO.getPendapatanDokter()} + tampilan legacy. */
    private PendapatanDokterResponse getPendapatanTindakan(Integer staffId, LocalDate from,
            LocalDate to, String patientType) {
        StringBuilder sql = new StringBuilder();
        sql.append("select nomor_nota as nota, nama_tindakan as tindakan, nama_pasien as pasien, ")
                .append("tipe_pasien as tipe, kelas_tarif as kelas, tgl_tindakan as tgl, ")
                .append("jasa_dokter as jasa, kode as kode, validate_by as validasi ")
                .append("from report.get_doctor_report(?, ?) ")
                .append("where n_staff_id = ? and jasa_dokter > 0 ");
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(from));
        params.add(java.sql.Date.valueOf(to));
        params.add(staffId);
        if (!"ALL".equalsIgnoreCase(patientType)) {
            sql.append("and tipe_pasien = ? ");
            params.add(patientType);
        }
        sql.append("order by tgl_tindakan, nomor_nota");

        List<PendapatanDokterRowResponse> rows = jdbcTemplate.query(sql.toString(),
                params.toArray(), (resultSet, rowNum) -> new PendapatanDokterRowResponse(
                        resultSet.getString("nota"),
                        resultSet.getString("kode"),
                        resultSet.getString("tindakan"),
                        resultSet.getString("validasi"),
                        resultSet.getString("pasien"),
                        resultSet.getString("tipe"),
                        resultSet.getString("kelas"),
                        toDisplayDate(resultSet.getDate("tgl")),
                        resultSet.getDouble("jasa")));

        double total = 0;
        for (PendapatanDokterRowResponse row : rows) {
            total += row.getJumlah();
        }
        return new PendapatanDokterResponse("PD", total, rows, new ArrayList<>());
    }

    /** OBAT — migrasi {@code NoteDAO.getNoteByDokter()} + perhitungan nilai legacy. */
    private PendapatanDokterResponse getPenjualanObat(Integer staffId, LocalDate from, LocalDate to,
            String patientType) {
        Timestamp tgl1 = Timestamp.valueOf(from.atStartOfDay());
        Timestamp tgl2 = Timestamp.valueOf(to.atTime(23, 59, 59));

        StringBuilder sql = new StringBuilder();
        sql.append("select x.nota as nota, x.pasien as pasien, x.tgl as tgl, x.nilai as nilai ")
                .append("from (select e.v_note_no as nota, p.v_patient_name as pasien, ")
                .append("e.d_whn_create as tgl, ")
                .append("(coalesce((select sum(it.n_amount_after_disc) from tb_item_trx it ")
                .append("where it.n_note_id = e.n_exam_id), 0) ")
                .append("+ coalesce((select sum(dg.n_amount_after_disc) from tb_drug_ingredients dg ")
                .append("where dg.n_note_id = e.n_exam_id), 0)) as nilai ")
                .append("from tb_examination e ")
                .append("join ms_patient p on p.n_patient_id = e.n_patient_id ")
                .append("join tb_registration r on r.n_reg_id = e.n_reg_id ")
                .append("where r.n_staff_id = ? and e.d_whn_create between ? and ? ");
        List<Object> params = new ArrayList<>();
        params.add(staffId);
        params.add(tgl1);
        params.add(tgl2);
        appendPatientFilter(sql, params, patientType);
        sql.append(") x where x.nilai > 0 order by x.tgl");

        List<PendapatanDokterRowResponse> rows = jdbcTemplate.query(sql.toString(),
                params.toArray(), (resultSet, rowNum) -> new PendapatanDokterRowResponse(
                        resultSet.getString("nota"),
                        null, null, null,
                        resultSet.getString("pasien"),
                        null, null,
                        toDisplayDateTime(resultSet.getTimestamp("tgl")),
                        resultSet.getDouble("nilai")));

        double total = 0;
        for (PendapatanDokterRowResponse row : rows) {
            total += row.getJumlah();
        }
        return new PendapatanDokterResponse("OBAT", total, rows, new ArrayList<>());
    }

    /** ALL — migrasi {@code NoteManagerImpl.getDoctorReportAll()}. */
    private List<PendapatanDokterAllRowResponse> getDoctorReportAll(LocalDate from, LocalDate to,
            String patientType) {
        List<DoctorOptionResponse> doctors = searchDoctors("", "");

        Timestamp tgl1 = Timestamp.valueOf(from.atStartOfDay());
        Timestamp tgl2 = Timestamp.valueOf(to.atTime(23, 59, 59));

        // jasa per dokter dari fungsi get_doctor_report
        StringBuilder jasaSql = new StringBuilder();
        jasaSql.append("select n_staff_id as staff_id, sum(jasa_dokter) as jasa ")
                .append("from report.get_doctor_report(?, ?) ")
                .append("where jasa_dokter > 0 ");
        List<Object> jasaParams = new ArrayList<>();
        jasaParams.add(java.sql.Date.valueOf(from));
        jasaParams.add(java.sql.Date.valueOf(to));
        if (!"ALL".equalsIgnoreCase(patientType)) {
            jasaSql.append("and tipe_pasien = ? ");
            jasaParams.add(patientType);
        }
        jasaSql.append("group by n_staff_id");
        Map<Integer, Double> jasaMap = new HashMap<>();
        jdbcTemplate.query(jasaSql.toString(), jasaParams.toArray(),
                new org.springframework.jdbc.core.ResultSetExtractor<Void>() {
                    @Override
                    public Void extractData(java.sql.ResultSet resultSet) throws java.sql.SQLException {
                        while (resultSet.next()) {
                            jasaMap.put(resultSet.getInt("staff_id"), resultSet.getDouble("jasa"));
                        }
                        return null;
                    }
                });

        // sumbangsih obat per dokter
        StringBuilder obatSql = new StringBuilder();
        obatSql.append("select r.n_staff_id as staff_id, ")
                .append("sum(coalesce((select sum(it.n_amount_after_disc) from tb_item_trx it ")
                .append("where it.n_note_id = e.n_exam_id), 0) ")
                .append("+ coalesce((select sum(dg.n_amount_after_disc) from tb_drug_ingredients dg ")
                .append("where dg.n_note_id = e.n_exam_id), 0)) as obat ")
                .append("from tb_examination e ")
                .append("join tb_registration r on r.n_reg_id = e.n_reg_id ")
                .append("where e.d_whn_create between ? and ? ");
        List<Object> obatParams = new ArrayList<>();
        obatParams.add(tgl1);
        obatParams.add(tgl2);
        appendPatientFilter(obatSql, obatParams, patientType);
        obatSql.append("group by r.n_staff_id");
        Map<Integer, Double> obatMap = new HashMap<>();
        jdbcTemplate.query(obatSql.toString(), obatParams.toArray(),
                new org.springframework.jdbc.core.ResultSetExtractor<Void>() {
                    @Override
                    public Void extractData(java.sql.ResultSet resultSet) throws java.sql.SQLException {
                        while (resultSet.next()) {
                            obatMap.put(resultSet.getInt("staff_id"), resultSet.getDouble("obat"));
                        }
                        return null;
                    }
                });

        List<PendapatanDokterAllRowResponse> rows = new ArrayList<>();
        for (DoctorOptionResponse doctor : doctors) {
            double jasa = jasaMap.getOrDefault(doctor.getStaffId(), 0.0);
            double obat = obatMap.getOrDefault(doctor.getStaffId(), 0.0);
            rows.add(new PendapatanDokterAllRowResponse(doctor.getName(), jasa, obat));
        }
        return rows;
    }

    /** Filter tipe pasien sesuai legacy (BPJS=8, COVID=9, NONBPJS=selain 8/9). */
    private void appendPatientFilter(StringBuilder sql, List<Object> params, String patientType) {
        if ("BPJS".equalsIgnoreCase(patientType)) {
            sql.append("and e.n_patient_id in (select p2.n_patient_id from ms_patient p2 ")
                    .append("where p2.n_patient_type_id = 8) ");
        } else if ("COVID".equalsIgnoreCase(patientType)) {
            sql.append("and e.n_patient_id in (select p2.n_patient_id from ms_patient p2 ")
                    .append("where p2.n_patient_type_id = 9) ");
        } else if ("NONBPJS".equalsIgnoreCase(patientType)) {
            sql.append("and e.n_patient_id in (select p2.n_patient_id from ms_patient p2 ")
                    .append("where p2.n_patient_type_id not in (8, 9) or p2.n_patient_type_id is null) ");
        }
    }

    private String toDisplayDate(java.sql.Date date) {
        return date == null ? "" : date.toLocalDate().format(DATE_DISPLAY);
    }

    private String toDisplayDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DATE_DISPLAY);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
