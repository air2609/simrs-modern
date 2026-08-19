package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.ward.WardPatientOptionResponse;
import com.vone.simrs.ward.WardUnitResponse;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0006 (LAPORAN HARIAN BANGSAL / laporanHarianBangsal.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LaporanHarianPasien} + {@code PatientManagerImpl.getPatientBaseOnWard()}
 * + {@code WardTransactionManagerImpl.getRegistration()} + fungsi database
 * {@code report.fungsi_rekap_pasien_bangsal} (rincian transaksi treatment/item/misc
 * per pasien rawat inap).
 */
@Service
public class LaporanHarianBangsalService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public LaporanHarianBangsalService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Unit lokasi user (dropdown LOKASI), migrasi {@code UserManagerImpl.getUnitUser()}. */
    public List<WardUnitResponse> getUnits(String username) {
        return jdbcTemplate.query(
                "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name, unt.n_whouse_id "
                        + "from ms_unit unt "
                        + "join ms_staff_in_unit stfunit on stfunit.n_unit_id = unt.n_unit_id "
                        + "join ms_user usr on usr.n_staff_id = stfunit.n_staff_id "
                        + "where upper(usr.v_user_name) = ? "
                        + "order by unt.v_unit_name",
                (resultSet, rowNum) -> new WardUnitResponse(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        getNullableInteger(resultSet, "n_whouse_id")),
                normalizeUsername(username));
    }

    /**
     * Cari pasien rawat inap aktif (modal NO. MR). Menggunakan pola
     * {@code PatientController.searchRanapPatient()} — pasien berbed aktif.
     */
    public List<WardPatientOptionResponse> searchPatients(String mrCode, String patientName,
            String address) {
        return jdbcTemplate.query(
                "select distinct mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, "
                        + "pat.v_patient_main_addr, pat.n_patient_type_id "
                        + "from ms_patient pat "
                        + "join tb_medical_record mr on mr.n_patient_id = pat.n_patient_id "
                        + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                        + "join tb_bed_occupancy boc on boc.n_reg_primary_id = reg.n_reg_id "
                        + "where reg.reg_status = 1 and boc.d_check_out_time is null "
                        + "and mr.v_mr_code like ? and pat.v_patient_name like ? "
                        + "and pat.v_patient_main_addr like ? limit 100",
                (resultSet, rowNum) -> {
                    Integer typeId = getNullableInteger(resultSet, "n_patient_type_id");
                    return new WardPatientOptionResponse(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getString("v_patient_name"),
                            typeId != null && typeId == 8 ? "BPJS" : "NON BPJS",
                            resultSet.getString("v_patient_main_addr"));
                },
                like(normalizeOptionalUpper(mrCode)),
                like(normalizeOptionalUpper(patientName)),
                like(normalizeOptionalUpper(address)));
    }

    /**
     * Detail registrasi pasien rawat inap aktif: regId, regNo, bed, ruangan, kelas.
     * Migrasi {@code getRegistrationBaseOnWard()} / {@code getLastRanap()}
     * + {@code getBedOccupanyByRegId()}.
     */
    public PatientRegistrationDetail getRegistration(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        List<PatientRegistrationDetail> rows = jdbcTemplate.query(
                "select reg.n_reg_id, reg.v_reg_secondary_id, mr.v_mr_code, "
                        + "pat.v_patient_name, bed.v_bed_desc, hall.v_hall_name, "
                        + "tclass.v_tclass_desc "
                        + "from tb_registration reg "
                        + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                        + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                        + "join tb_bed_occupancy boc on boc.n_reg_primary_id = reg.n_reg_id "
                        + "and boc.d_check_out_time is null "
                        + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                        + "join ms_room room on room.n_room_id = bed.n_room_id "
                        + "join ms_hall hall on hall.n_hall_id = room.n_hall_id "
                        + "left join ms_treatment_class tclass on tclass.n_tclass_id = bed.n_tclass_id "
                        + "where reg.reg_status = 1 and reg.v_reg_secondary_id like 'I%' "
                        + "and upper(mr.v_mr_code) = ? "
                        + "order by reg.d_registration_date desc limit 1",
                (resultSet, rowNum) -> new PatientRegistrationDetail(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_reg_secondary_id"),
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("v_patient_name"),
                        resultSet.getString("v_bed_desc"),
                        resultSet.getString("v_hall_name"),
                        resultSet.getString("v_tclass_desc")),
                normalizeMrCode(mrCode));
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("PASIEN TIDAK TERDAFTAR DI BANGSAL INI");
        }
        return rows.get(0);
    }

    /**
     * Rincian transaksi pasien bangsal per periode & pola no nota unit.
     * Migrasi {@code report.fungsi_rekap_pasien_bangsal(regid, startdate, enddate, unitname)}
     * — treatment trx + item trx + misc trx, plus baris TOTAL.
     */
    public LaporanHarianBangsalResponse getReport(Integer regId, String unitCode, String from,
            String to) {
        if (regId == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        Timestamp tgl1 = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp tgl2 = Timestamp.valueOf(toDate.atTime(23, 59, 59));
        String unitPattern = "%" + (hasText(unitCode) ? unitCode.trim().toUpperCase() : "") + "%";

        String sql = "select x.nomor_transaksi, x.kode_transaksi, x.keterangan, "
                + "x.jumlah, x.nilai from ( "
                + "select nota.v_note_no as nomor_transaksi, "
                + "treat.v_treatment_code as kode_transaksi, "
                + "treat.v_treatment_name as keterangan, "
                + "trx.n_qty as jumlah, trx.n_amount_after_disc as nilai "
                + "from tb_examination nota "
                + "join tb_treatment_trx trx on trx.n_note_id = nota.n_exam_id "
                + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = trx.n_treatment_fee_id "
                + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                + "where nota.n_reg_id = ? and nota.v_note_no like ? "
                + "and nota.d_whn_create between ? and ? "
                + "union all "
                + "select nota.v_note_no, item.v_item_code, item.v_item_name, "
                + "trx.n_qty, trx.n_amount_after_disc "
                + "from tb_examination nota "
                + "join tb_item_trx trx on trx.n_note_id = nota.n_exam_id "
                + "join ms_item item on item.n_item_id = trx.n_item_id "
                + "where nota.n_reg_id = ? and nota.v_note_no like ? "
                + "and nota.d_whn_create between ? and ? "
                + "union all "
                + "select nota.v_note_no, 'MISC-001', trx.v_misc_name, "
                + "trx.n_qty, trx.n_amount_after_disc "
                + "from tb_examination nota "
                + "join tb_misc_trx trx on trx.n_note_id = nota.n_exam_id "
                + "where nota.n_reg_id = ? and nota.v_note_no like ? "
                + "and nota.d_whn_create between ? and ? "
                + ") x order by x.nomor_transaksi, x.keterangan";

        Object[] params = new Object[] { regId, unitPattern, tgl1, tgl2,
                regId, unitPattern, tgl1, tgl2,
                regId, unitPattern, tgl1, tgl2 };

        List<LaporanHarianBangsalRowResponse> rows = jdbcTemplate.query(sql, params,
                (resultSet, rowNum) -> new LaporanHarianBangsalRowResponse(
                        rowNum + 1,
                        resultSet.getString("nomor_transaksi"),
                        resultSet.getString("kode_transaksi"),
                        resultSet.getString("keterangan"),
                        getNullableInteger(resultSet, "jumlah"),
                        toDouble(resultSet.getObject("nilai"))));

        double total = 0;
        for (LaporanHarianBangsalRowResponse row : rows) {
            total += row.getNilai() == null ? 0 : row.getNilai();
        }
        rows.add(new LaporanHarianBangsalRowResponse(null, null, null, "T  O  T  A  L",
                null, total));

        PatientRegistrationDetail reg = getRegistrationByRegId(regId);
        return new LaporanHarianBangsalResponse(reg.mrNo, reg.namaPasien, reg.regNo,
                reg.bed, reg.ruangan, reg.kelas, total, rows);
    }

    private PatientRegistrationDetail getRegistrationByRegId(Integer regId) {
        List<PatientRegistrationDetail> rows = jdbcTemplate.query(
                "select reg.n_reg_id, reg.v_reg_secondary_id, mr.v_mr_code, "
                        + "pat.v_patient_name, bed.v_bed_desc, hall.v_hall_name, "
                        + "tclass.v_tclass_desc "
                        + "from tb_registration reg "
                        + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                        + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                        + "left join tb_bed_occupancy boc on boc.n_reg_primary_id = reg.n_reg_id "
                        + "and boc.d_check_out_time is null "
                        + "left join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                        + "left join ms_room room on room.n_room_id = bed.n_room_id "
                        + "left join ms_hall hall on hall.n_hall_id = room.n_hall_id "
                        + "left join ms_treatment_class tclass on tclass.n_tclass_id = bed.n_tclass_id "
                        + "where reg.n_reg_id = ?",
                (resultSet, rowNum) -> new PatientRegistrationDetail(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_reg_secondary_id"),
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("v_patient_name"),
                        resultSet.getString("v_bed_desc"),
                        resultSet.getString("v_hall_name"),
                        resultSet.getString("v_tclass_desc")),
                regId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase();
    }

    private String normalizeMrCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private String normalizeOptionalUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String like(String value) {
        return "%" + value + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    /** Detail registrasi pasien (dipakai laporan & PDF). */
    public static final class PatientRegistrationDetail {
        private final Integer regId;
        private final String regNo;
        private final String mrNo;
        private final String namaPasien;
        private final String bed;
        private final String ruangan;
        private final String kelas;

        private PatientRegistrationDetail(Integer regId, String regNo, String mrNo,
                String namaPasien, String bed, String ruangan, String kelas) {
            this.regId = regId;
            this.regNo = regNo;
            this.mrNo = mrNo;
            this.namaPasien = namaPasien;
            this.bed = bed;
            this.ruangan = ruangan;
            this.kelas = kelas;
        }

        public Integer getRegId() {
            return regId;
        }

        public String getRegNo() {
            return regNo;
        }

        public String getMrNo() {
            return mrNo;
        }

        public String getNamaPasien() {
            return namaPasien;
        }

        public String getBed() {
            return bed;
        }

        public String getRuangan() {
            return ruangan;
        }

        public String getKelas() {
            return kelas;
        }
    }
}
