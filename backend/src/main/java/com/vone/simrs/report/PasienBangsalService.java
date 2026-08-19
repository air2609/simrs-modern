package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0005 (LAPORAN PASIEN RAWAT INAP / laporanPasienBangsal.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LaporanPasienBangsalController} — memakai query
 * fungsi database {@code report.laporan_pasien_masuk_bangsal} (registrasi rawat
 * inap + bed occupancy per bangsal) namun ditulis ulang dalam satu SQL agregat
 * tanpa bug truncation varchar(15) pada nama bed.
 */
@Service
public class PasienBangsalService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public PasienBangsalService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Daftar bangsal (LOKASI) — ms_ward, filter nama tidak kosong. */
    public List<WardOptionResponse> getWards() {
        return jdbcTemplate.query(
                "select n_ward_id, v_ward_name from ms_ward "
                        + "where v_ward_name is not null and trim(v_ward_name) <> '' "
                        + "order by v_ward_name",
                (resultSet, rowNum) -> new WardOptionResponse(
                        resultSet.getInt("n_ward_id"),
                        resultSet.getString("v_ward_name")));
    }

    /**
     * Pasien masuk rawat inap per bangsal & rentang tanggal.
     * Migrasi fungsi {@code report.laporan_pasien_masuk_bangsal}.
     */
    public PasienBangsalResponse getReport(String wardName, String from, String to) {
        if (!hasText(wardName)) {
            throw new IllegalArgumentException("LOKASI HARUS DIPILIH!");
        }
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        Timestamp tgl1 = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp tgl2 = Timestamp.valueOf(toDate.atTime(23, 59, 59));

        String sql = "select reg.v_reg_secondary_id as no_registrasi, "
                + "mr.v_mr_code as no_rm, pat.v_patient_name as nama_pasien, "
                + "reg.d_whn_create as tgl_daftar, bed.v_bed_desc as nama_bed, "
                + "coalesce(pt.v_tpatient_desc, '') as jenis_pasien, "
                + "pat.v_patient_main_addr as alamat_pasien, hall.v_hall_name as ruangan "
                + "from tb_registration reg "
                + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                + "join tb_bed_occupancy boc on boc.n_reg_primary_id = reg.n_reg_id "
                + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                + "join ms_room room on room.n_room_id = bed.n_room_id "
                + "join ms_hall hall on hall.n_hall_id = room.n_hall_id "
                + "join ms_ward bangsal on bangsal.n_ward_id = hall.n_ward_id "
                + "left join ms_patient_type pt on pt.n_patient_type_id = pat.n_patient_type_id "
                + "where reg.v_reg_secondary_id like 'I%' "
                + "and reg.d_whn_create between ? and ? "
                + "and bangsal.v_ward_name = ? "
                + "order by reg.d_whn_create";

        List<PasienBangsalRowResponse> rows = jdbcTemplate.query(sql,
                new Object[] { tgl1, tgl2, wardName },
                (resultSet, rowNum) -> new PasienBangsalRowResponse(
                        rowNum + 1,
                        resultSet.getString("no_registrasi"),
                        resultSet.getString("no_rm"),
                        resultSet.getString("nama_pasien"),
                        toDisplayDateTime(resultSet.getTimestamp("tgl_daftar")),
                        resultSet.getString("nama_bed"),
                        resultSet.getString("jenis_pasien"),
                        resultSet.getString("alamat_pasien"),
                        resultSet.getString("ruangan")));

        return new PasienBangsalResponse(wardName, rows);
    }

    private String toDisplayDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DATE_DISPLAY);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
