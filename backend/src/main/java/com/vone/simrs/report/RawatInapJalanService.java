package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0015 (LAPORAN RAWAT INAP/JALAN / laporanRawatInapJalan.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LaporanRawatInapJalan} + {@code NoteManagerImpl.getRawatInapJalan()}
 * + {@code NoteDAO.getBedTransaction()/getLaporanDiagnosaRajal()/getBpjsSettlement()/
 * getStatusPasien()/getDiagnose()} — dua tipe: RI (registrasi rawat inap beserta
 * bed trx & diagnosa) dan RJ (registrasi rawat jalan beserta diagnosa).
 */
@Service
public class RawatInapJalanService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public RawatInapJalanService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Laporan rawat inap / rawat jalan per rentang tanggal.
     *
     * @param tipe RI (RAWAT INAP) atau RJ (RAWAT JALAN)
     */
    public RawatInapJalanResponse getReport(String tipe, String from, String to) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("Kedua tanggal harus diisi....!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        String reportType = hasText(tipe) ? tipe.toUpperCase() : "RI";
        Timestamp tgl1 = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp tgl2 = Timestamp.valueOf(toDate.atTime(23, 59, 59));

        List<RawatInapJalanRowResponse> rows;
        if ("RJ".equalsIgnoreCase(reportType)) {
            rows = getRawatJalan(tgl1, tgl2);
        } else {
            rows = getRawatInap(tgl1, tgl2);
        }
        return new RawatInapJalanResponse(reportType, rows);
    }

    /** RI — migrasi {@code NoteDAO.getBedTransaction()} + logika legacy. */
    private List<RawatInapJalanRowResponse> getRawatInap(Timestamp tgl1, Timestamp tgl2) {
        String sql = "select r.n_reg_id as id, "
                + "mr.v_mr_code as mr_no, p.v_patient_name as nama, "
                + "case when p.v_patient_gender = 'M' then 'L' else 'P' end as jk, "
                + "p.d_patient_dob as tgl_lahir, p.v_patient_religion as agama, "
                + "case when p.v_patient_etnis is null or p.v_patient_etnis = 'kosong' "
                + "then '-' else p.v_patient_etnis end as etnis, "
                + "case when p.v_patient_language is null or p.v_patient_language = 'kosong' "
                + "then '-' else p.v_patient_language end as bahasa, "
                + "s.v_staff_name as dokter, r.d_whn_create as tgl_masuk, "
                + "(select max(bt.d_who_create) from tb_examination e2 "
                + "join tb_bed_trx bt on bt.n_note_id = e2.n_exam_id "
                + "where e2.n_reg_id = r.n_reg_id) as tgl_keluar, "
                + "(select coalesce(sum(bt.n_total_hour), 0) from tb_examination e3 "
                + "join tb_bed_trx bt on bt.n_note_id = e3.n_exam_id "
                + "where e3.n_reg_id = r.n_reg_id) as lama, "
                + "(select b.v_bed_desc from tb_examination e4 "
                + "join tb_bed_trx bt on bt.n_note_id = e4.n_exam_id "
                + "join ms_bed b on b.n_bed_id = bt.n_bed_id "
                + "where e4.n_reg_id = r.n_reg_id "
                + "order by bt.d_who_create desc limit 1) as bed, "
                + "(select tc.v_tclass_desc from tb_examination e5 "
                + "join tb_bed_trx bt on bt.n_note_id = e5.n_exam_id "
                + "join ms_bed b on b.n_bed_id = bt.n_bed_id "
                + "join ms_treatment_class tc on tc.n_tclass_id = b.n_tclass_id "
                + "where e5.n_reg_id = r.n_reg_id "
                + "order by bt.d_who_create desc limit 1) as kelas, "
                + "case when exists (select 1 from tb_patient_settlement ps "
                + "join tb_patient_bill pb on pb.n_pbill_id = ps.n_pbill_id "
                + "where pb.n_reg_id = r.n_reg_id and ps.n_insurance_id = 75) "
                + "then 'BPJS' else 'NONBPJS' end as tipe, "
                + "case when (select count(*) from tb_registration r2 "
                + "where r2.n_mr_id = r.n_mr_id and r2.n_transfer_reg_id is not null) > 1 "
                + "then 'PASIEN LAMA' else 'PASIEN BARU' end as status, "
                + "(select string_agg(i.v_icd_name, ',' order by i.v_icd_name) "
                + "from tb_diagnose d join tb_icd_diagnose id on id.n_diagnose_id = d.n_diagnose_id "
                + "join ms_icd i on i.n_icd_id = id.n_icd_id "
                + "where d.n_reg_id = r.n_reg_id) as diagnosa "
                + "from tb_registration r "
                + "join tb_medical_record mr on mr.n_mr_id = r.n_mr_id "
                + "join ms_patient p on p.n_patient_id = mr.n_patient_id "
                + "join ms_staff s on s.n_staff_id = r.n_staff_id "
                + "where r.d_whn_create between ? and ? "
                + "and substr(trim(r.v_reg_secondary_id), 1, 1) = 'I' "
                + "order by r.d_whn_create";

        return jdbcTemplate.query(sql, new Object[] { tgl1, tgl2 },
                (resultSet, rowNum) -> new RawatInapJalanRowResponse(
                        resultSet.getString("mr_no"),
                        resultSet.getString("nama"),
                        resultSet.getString("jk"),
                        toDisplayDate(resultSet.getDate("tgl_lahir")),
                        calculateAge(resultSet.getDate("tgl_lahir")),
                        resultSet.getString("tipe"),
                        resultSet.getString("status"),
                        resultSet.getString("agama"),
                        resultSet.getString("etnis"),
                        resultSet.getString("bahasa"),
                        resultSet.getString("dokter"),
                        resultSet.getString("bed"),
                        resultSet.getString("kelas"),
                        toDisplayDateTime(resultSet.getTimestamp("tgl_masuk")),
                        resultSet.getTimestamp("tgl_keluar") == null ? "-"
                                : toDisplayDateTime(resultSet.getTimestamp("tgl_keluar")),
                        resultSet.getInt("lama"),
                        nvl(resultSet.getString("diagnosa")),
                        "", ""));
    }

    /** RJ — migrasi {@code NoteDAO.getLaporanDiagnosaRajal()} + logika legacy. */
    private List<RawatInapJalanRowResponse> getRawatJalan(Timestamp tgl1, Timestamp tgl2) {
        String sql = "select r.n_reg_id as id, r.d_registration_date as tgl, "
                + "u.v_unit_name as unit, s.v_staff_name as dokter, mr.v_mr_code as mr_no, "
                + "p.v_patient_name as nama, "
                + "(select i.v_icd_name from tb_icd_diagnose id, ms_icd i, tb_diagnose d "
                + "where i.n_icd_id = id.n_icd_id and d.n_diagnose_id = id.n_diagnose_id "
                + "and d.n_reg_id = r.n_reg_id limit 1) as diagnosa, "
                + "p.d_patient_dob as tgl_lahir, p.v_patient_religion as agama, "
                + "p.v_patient_gender as jk, p.v_patient_etnis as etnis, "
                + "p.v_patient_language as bahasa, mr.n_mr_id as mr_id, "
                + "case when exists (select 1 from tb_patient_settlement ps "
                + "join tb_patient_bill pb on pb.n_pbill_id = ps.n_pbill_id "
                + "where pb.n_reg_id = r.n_reg_id and ps.n_insurance_id = 75) "
                + "then 'BPJS' else 'NONBPJS' end as tipe, "
                + "case when (select count(*) from tb_registration r2 "
                + "where r2.n_mr_id = r.n_mr_id and r2.n_transfer_reg_id is null) > 1 "
                + "then 'PASIEN LAMA' else 'PASIEN BARU' end as status "
                + "from tb_registration r, ms_unit u, ms_staff s, tb_medical_record mr, ms_patient p "
                + "where r.d_registration_date between ? and ? and r.n_transfer_reg_id is null "
                + "and u.n_unit_id = r.n_unit_id and s.n_staff_id = r.n_staff_id "
                + "and mr.n_mr_id = r.n_mr_id and p.n_patient_id = mr.n_patient_id "
                + "order by r.d_registration_date";

        return jdbcTemplate.query(sql, new Object[] { tgl1, tgl2 },
                (resultSet, rowNum) -> new RawatInapJalanRowResponse(
                        resultSet.getString("mr_no"),
                        resultSet.getString("nama"),
                        "M".equalsIgnoreCase(resultSet.getString("jk")) ? "L" : "P",
                        toDisplayDate(resultSet.getDate("tgl_lahir")),
                        calculateAge(resultSet.getDate("tgl_lahir")),
                        resultSet.getString("tipe"),
                        resultSet.getString("status"),
                        resultSet.getString("agama"),
                        nvlOrDash(resultSet.getString("etnis")),
                        nvlOrDash(resultSet.getString("bahasa")),
                        resultSet.getString("dokter"),
                        null, null, "",
                        "",
                        0,
                        nvl(resultSet.getString("diagnosa")),
                        toDisplayDateTime(resultSet.getTimestamp("tgl")),
                        resultSet.getString("unit")));
    }

    /** Migrasi {@code NoteManagerImpl.calculateAge()} — usia dalam tahun (desimal). */
    private double calculateAge(java.sql.Date dob) {
        if (dob == null) {
            return 0;
        }
        long diffMillis = System.currentTimeMillis() - dob.toLocalDate().atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return (diffMillis / (24.0 * 60 * 60 * 1000)) / 365.0;
    }

    private String toDisplayDate(java.sql.Date date) {
        return date == null ? "" : date.toLocalDate().format(DATE_DISPLAY);
    }

    private String toDisplayDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DATE_DISPLAY);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String nvlOrDash(String value) {
        return (value == null || value.trim().isEmpty() || "kosong".equalsIgnoreCase(value.trim()))
                ? "-" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
