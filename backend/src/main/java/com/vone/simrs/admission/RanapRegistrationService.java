package com.vone.simrs.admission;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk pendaftaran pasien RAWAT INAP (SC0001 tab 2 — PasienRanap.zul).
 *
 * <p>
 * Migrasi dari legacy {@code RanapController} + {@code RegistrationManagerImpl.saveRanap()}
 * + {@code TbRegistrationDAO.saveRanapRegistration()/cancelRanap()}.
 */
@Service
public class RanapRegistrationService {

    private static final int REG_ACTIVE = 1;
    private static final int REG_NON_ACTIVE = 0;
    private static final String MAIN_DOCTOR = "Y";
    private static final String BED_KOSONG = "0";
    private static final String BED_TERPAKAI = "1";
    private static final String AVAILABLE_A = "A";
    private static final DateTimeFormatter RANAP_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public RanapRegistrationService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ------------------------------------------------------------------ masters

    /** Kelas tarif untuk select KELAS TARIF / ANTRIAN KELAS. */
    public RanapMastersResponse getMasters() {
        List<OptionResponse> classes = jdbcTemplate.query(
                "select n_tclass_id, v_tclass_desc from ms_treatment_class order by n_tclass_id",
                (resultSet, rowNum) -> new OptionResponse(
                        resultSet.getString("n_tclass_id"),
                        resultSet.getString("v_tclass_desc")));
        return new RanapMastersResponse(classes);
    }

    // ------------------------------------------------------------------ pasien

    /** Cari pasien terdaftar dengan NIK. Migrasi dari searchPatientRegisteredWithNik. */
    public List<RanapPatientOptionResponse> searchPatients(String mrCode, String patientName,
            String nik, String birthDate, String address) {
        StringBuilder sql = new StringBuilder();
        sql.append("select distinct mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, pat.nik, ")
                .append("pat.d_patient_dob, pat.v_patient_main_addr ")
                .append("from tb_medical_record mr ")
                .append("join ms_patient pat on pat.n_patient_id = mr.n_patient_id ")
                .append("join tb_registration reg on reg.n_mr_id = mr.n_mr_id ")
                .append("where reg.reg_status = ? and mr.v_mr_code like ? ")
                .append("and pat.v_patient_name like ? and pat.nik like ? ")
                .append("and pat.v_patient_main_addr like ? ");
        List<Object> params = new ArrayList<>();
        params.add(REG_ACTIVE);
        params.add(like(normalizeOptionalUpper(mrCode)));
        params.add(like(normalizeOptionalUpper(patientName)));
        params.add(like(normalizeOptionalUpper(nik)));
        params.add(like(normalizeOptionalUpper(address)));
        if (hasText(birthDate)) {
            sql.append("and pat.d_patient_dob = ? ");
            params.add(LocalDate.parse(birthDate));
        }
        sql.append("limit 100");
        return jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> new RanapPatientOptionResponse(
                        resultSet.getInt("n_mr_id"),
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("v_patient_name"),
                        resultSet.getString("nik"),
                        toIsoDate(resultSet.getDate("d_patient_dob")),
                        resultSet.getString("v_patient_main_addr")));
    }

    /**
     * Detail pasien untuk ranap: MR + registrasi rajal terakhir + riwayat nota.
     * Migrasi dari {@code RegistrationManagerImpl.getPatientDetil()}.
     */
    public RanapPatientDetailResponse getPatientDetail(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        PatientRow patient = findPatientByMrCode(normalizeMrCode(mrCode));
        RegistrationRow oldReg = findLastRegistration(patient.mrId);
        if (oldReg != null && oldReg.regNo != null && oldReg.regNo.startsWith("I")) {
            throw new IllegalArgumentException("admission.ranap.not.allowed");
        }
        List<RanapHistoryResponse> history = oldReg == null ? new ArrayList<>()
                : getNoteHistory(oldReg.regId);
        return new RanapPatientDetailResponse(
                patient.mrId, patient.mrCode, patient.patientName, patient.gender,
                oldReg == null ? null : oldReg.regId,
                oldReg == null ? null : oldReg.regNo,
                oldReg == null ? null : displayDate(oldReg.regDate),
                countRanap(patient.mrId),
                history);
    }

    private PatientRow findPatientByMrCode(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select mr.n_mr_id, mr.v_mr_code, pat.n_patient_id, pat.v_patient_name, "
                            + "pat.v_patient_gender "
                            + "from tb_medical_record mr "
                            + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                            + "where upper(mr.v_mr_code) = ?",
                    (resultSet, rowNum) -> new PatientRow(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getInt("n_patient_id"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_gender")),
                    mrCode);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("common.data.notfound");
        }
    }

    private RegistrationRow findLastRegistration(Integer mrId) {
        List<RegistrationRow> rows = jdbcTemplate.query(
                "select n_reg_id, v_reg_secondary_id, d_registration_date from tb_registration "
                        + "where n_mr_id = ? and reg_status = ? "
                        + "order by d_registration_date desc limit 1",
                (resultSet, rowNum) -> new RegistrationRow(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_reg_secondary_id"),
                        resultSet.getTimestamp("d_registration_date")),
                mrId, REG_ACTIVE);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<RanapHistoryResponse> getNoteHistory(Integer regId) {
        return jdbcTemplate.query(
                "select note.d_whn_create, note.v_note_no, unit.v_unit_name "
                        + "from tb_examination note "
                        + "left join ms_unit unit on unit.n_unit_id = note.n_unit_id "
                        + "where note.n_reg_id = ? order by note.d_whn_create",
                (resultSet, rowNum) -> new RanapHistoryResponse(
                        toIsoDateTime(resultSet.getTimestamp("d_whn_create")),
                        resultSet.getString("v_note_no"),
                        resultSet.getString("v_unit_name")),
                regId);
    }

    private Integer countRanap(Integer mrId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select count(*) from tb_registration where n_mr_id = ? "
                            + "and v_reg_secondary_id like 'I%'",
                    Integer.class, mrId);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ hall & bed

    /**
     * Ruangan berdasarkan kelas tarif + jumlah bed tersedia. Migrasi dari
     * {@code RegistrationManagerImpl.getHallListByTclassId()}.
     */
    public List<RanapHallResponse> getHallsByClass(Integer classId) {
        if (classId == null) {
            throw new IllegalArgumentException("PILIH KELAS TARIF TERLEBIH DAHULU!");
        }
        return jdbcTemplate.query(
                "select h.n_hall_id, h.v_hall_name, "
                        + "coalesce(sum(case when bed.v_bed_active_status = 'A' "
                        + "and coalesce(bed.v_bed_status, '0') <> '1' then 1 else 0 end), 0) as available "
                        + "from ms_hall h "
                        + "join ms_room room on room.n_hall_id = h.n_hall_id "
                        + "join ms_bed bed on bed.n_room_id = room.n_room_id "
                        + "where h.n_tclass_id = ? "
                        + "group by h.n_hall_id, h.v_hall_name order by h.v_hall_name",
                (resultSet, rowNum) -> new RanapHallResponse(
                        resultSet.getInt("n_hall_id"),
                        resultSet.getString("v_hall_name"),
                        getNullableInteger(resultSet, "available")),
                classId);
    }

    /**
     * Bed pada ruangan. Migrasi dari {@code RegistrationManagerImpl.getBedBaseOnHallId()}.
     */
    public List<RanapBedResponse> getBedsByHall(Integer hallId) {
        if (hallId == null) {
            throw new IllegalArgumentException("PILIH RUANGAN TERLEBIH DAHULU!");
        }
        return jdbcTemplate.query(
                "select bed.n_bed_id, bed.v_bed_desc, bed.v_bed_status, bed.available_status, "
                        + "boc.d_check_in_time, mr.v_mr_code, pat.v_patient_name "
                        + "from ms_bed bed "
                        + "join ms_room room on room.n_room_id = bed.n_room_id "
                        + "left join tb_bed_occupancy boc on boc.n_bed_primary_id = bed.n_bed_id "
                        + "and boc.d_check_out_time is null "
                        + "left join tb_registration reg on reg.n_reg_id = boc.n_reg_primary_id "
                        + "left join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                        + "left join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                        + "where room.n_hall_id = ? and bed.v_bed_active_status = 'A' "
                        + "order by bed.v_bed_desc",
                (resultSet, rowNum) -> {
                    String bedDesc = resultSet.getString("v_bed_desc");
                    String availableStatus = resultSet.getString("available_status");
                    String label;
                    if ("B".equals(availableStatus)) {
                        label = bedDesc + " - Dipesan";
                    } else if ("C".equals(availableStatus)) {
                        label = bedDesc + " - Dalam Perbaikan";
                    } else {
                        label = bedDesc;
                    }
                    return new RanapBedResponse(
                            resultSet.getInt("n_bed_id"),
                            bedDesc,
                            resultSet.getString("v_bed_status"),
                            availableStatus,
                            label,
                            resultSet.getString("v_mr_code"),
                            resultSet.getString("v_patient_name"));
                },
                hallId);
    }

    // ------------------------------------------------------------------ simpan / batal

    /**
     * Simpan pendaftaran rawat inap. Migrasi dari
     * {@code RegistrationManagerImpl.saveRanap()} + {@code TbRegistrationDAO.saveRanapRegistration()}.
     */
    @Transactional
    public RanapSaveResultResponse save(RanapSaveRequest request, String username) {
        if (!hasText(request.getMrCode())) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        if (request.getBedId() == null) {
            throw new IllegalArgumentException("BED HARUS DI ISI!");
        }
        if (request.getDoctorId() == null) {
            throw new IllegalArgumentException("DOKTER UTAMA HARUS DI ISI!");
        }
        PatientRow patient = findPatientByMrCode(normalizeMrCode(request.getMrCode()));
        RegistrationRow oldReg = findLastRegistration(patient.mrId);
        if (oldReg != null && oldReg.regNo != null && oldReg.regNo.startsWith("I")) {
            throw new IllegalArgumentException("admission.ranap.not.allowed");
        }
        BedInfo bed = findBed(request.getBedId());
        if (BED_TERPAKAI.equals(bed.status)) {
            throw new IllegalArgumentException("master.bed.used");
        }
        if (hasActiveRanap(patient.mrId)) {
            throw new IllegalArgumentException("admission.ranap.not.allowed");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Integer regId = getNextSequence("tb_registration_n_reg_id_seq");
        Integer ranapNumber = getNextSequence("ranap_number");
        String regNo = "I-" + now.toLocalDateTime().format(RANAP_DATE) + "-"
                + String.format("%03d", ranapNumber);

        jdbcTemplate.update(
                "insert into tb_registration (n_reg_id, n_mr_id, n_staff_id, d_registration_date, "
                        + "v_reg_secondary_id, reg_status, v_main_doctor_status, v_who_create, "
                        + "d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                regId, patient.mrId, request.getDoctorId(), now, regNo, REG_ACTIVE,
                MAIN_DOCTOR, username, now);

        if (oldReg != null) {
            jdbcTemplate.update(
                    "update tb_registration set reg_status = ?, v_who_change = ?, d_whn_change = ? "
                            + "where n_reg_id = ?",
                    REG_NON_ACTIVE, username, now, oldReg.regId);
        }

        if (request.getAntriKelasId() != null) {
            jdbcTemplate.update(
                    "insert into tb_room_reservation (n_room_rsv_id, n_reg_id, n_tclass_id, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?)",
                    getNextSequence("tb_room_reservation_n_room_rsv_id_seq"),
                    regId, request.getAntriKelasId(), username, now);
        }

        jdbcTemplate.update(
                "insert into tb_bed_occupancy (n_reg_primary_id, n_bed_primary_id, "
                        + "d_check_in_time, d_whn_create, v_who_create) values (?, ?, ?, ?, ?)",
                regId, request.getBedId(), now, now, username);

        jdbcTemplate.update("update ms_bed set v_bed_status = ? where n_bed_id = ?",
                BED_TERPAKAI, request.getBedId());

        migrateObatRajal(oldReg, patient, regId, username, now);

        return new RanapSaveResultResponse(
                true, "Pendaftaran rawat inap berhasil disimpan.", regNo,
                displayDate(now), countRanap(patient.mrId));
    }

    /**
     * Migrasi obat rawat jalan dari registrasi lama ke inventory pasien. Migrasi
     * dari {@code TbRegistrationDAO.saveRanapRegistration()} (getObatRajal).
     */
    private void migrateObatRajal(RegistrationRow oldReg, PatientRow patient, Integer newRegId,
            String username, Timestamp now) {
        if (oldReg == null) {
            return;
        }
        jdbcTemplate.query(
                "select trx.n_item_id, trx.n_qty, trx.n_item_trx_id "
                        + "from tb_item_trx trx "
                        + "join tb_examination note on note.n_exam_id = trx.n_note_id "
                        + "where note.n_reg_id = ?",
                (resultSet, rowNum) -> {
                    jdbcTemplate.update(
                            "insert into tb_patient_inventory (n_item_trx_id, v_who_create, "
                                    + "d_whn_create, n_qty, n_item_id, n_pat_id, n_reg_id) "
                                    + "values (?, ?, ?, ?, ?, ?, ?)",
                            resultSet.getInt("n_item_trx_id"), username, now,
                            resultSet.getInt("n_qty"),
                            resultSet.getInt("n_item_id"),
                            patient.patientId, newRegId);
                    return null;
                },
                oldReg.regId);
    }

    private boolean hasActiveRanap(Integer mrId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from tb_registration "
                            + "where n_mr_id = ? and reg_status = ? and v_reg_secondary_id like 'I%'",
                    Integer.class, mrId, REG_ACTIVE);
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    /**
     * Batalkan pendaftaran rawat inap. Migrasi dari {@code TbRegistrationDAO.cancelRanap()}.
     */
    @Transactional
    public RanapActionResultResponse cancel(RanapCancelRequest request) {
        if (request.getNewRegId() == null || request.getBedId() == null
                || request.getOldRegId() == null) {
            throw new IllegalArgumentException("Data registrasi tidak lengkap!");
        }
        jdbcTemplate.update("update ms_bed set v_bed_status = ? where n_bed_id = ?",
                BED_KOSONG, request.getBedId());
        jdbcTemplate.update(
                "delete from tb_bed_occupancy where n_reg_primary_id = ? and n_bed_primary_id = ?",
                request.getNewRegId(), request.getBedId());
        jdbcTemplate.update("update tb_registration set reg_status = ? where n_reg_id = ?",
                REG_ACTIVE, request.getOldRegId());
        jdbcTemplate.update("delete from tb_room_reservation where n_reg_id = ?", request.getNewRegId());
        jdbcTemplate.update("delete from tb_registration where n_reg_id = ?", request.getNewRegId());
        return new RanapActionResultResponse(true, "Pembatalan registrasi berhasil.");
    }

    // ------------------------------------------------------------------ helpers

    private BedInfo findBed(Integer bedId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_bed_id, v_bed_desc, v_bed_status from ms_bed where n_bed_id = ?",
                    (resultSet, rowNum) -> new BedInfo(
                            resultSet.getInt("n_bed_id"),
                            resultSet.getString("v_bed_desc"),
                            resultSet.getString("v_bed_status")),
                    bedId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("master.bed.notfound");
        }
    }

    private Integer getNextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private String displayDate(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DISPLAY_DATE);
    }

    private String normalizeMrCode(String mrCode) {
        return mrCode == null ? "" : mrCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String like(String value) {
        return "%" + (value != null ? value : "") + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String toIsoDate(java.sql.Date date) {
        return date == null ? "" : date.toLocalDate().toString();
    }

    private String toIsoDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString();
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static final class PatientRow {
        private final int mrId;
        private final String mrCode;
        private final int patientId;
        private final String patientName;
        private final String gender;

        private PatientRow(int mrId, String mrCode, int patientId, String patientName,
                String gender) {
            this.mrId = mrId;
            this.mrCode = mrCode;
            this.patientId = patientId;
            this.patientName = patientName;
            this.gender = gender;
        }
    }

    private static final class RegistrationRow {
        private final int regId;
        private final String regNo;
        private final Timestamp regDate;

        private RegistrationRow(int regId, String regNo, Timestamp regDate) {
            this.regId = regId;
            this.regNo = regNo;
            this.regDate = regDate;
        }
    }

    private static final class BedInfo {
        private final int bedId;
        private final String bedDesc;
        private final String status;

        private BedInfo(int bedId, String bedDesc, String status) {
            this.bedId = bedId;
            this.bedDesc = bedDesc;
            this.status = status;
        }
    }
}
