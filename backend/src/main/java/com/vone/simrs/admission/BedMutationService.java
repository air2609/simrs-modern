package com.vone.simrs.admission;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0002 (FORM MUTASI KAMAR / MutasiKamar.zul).
 *
 * <p>
 * Migrasi dari legacy {@code MutasiKamarController} +
 * {@code MutasiKamarManagerImpl} + {@code TbBedOccupancyDAO}:
 * <ul>
 *   <li>{@code getPatientRanapDetil()} — cari pasien + riwayat mutasi bed</li>
 *   <li>{@code getHistoryOfBedMove()} — daftar history mutasi bed</li>
 *   <li>{@code save()} — mutasi baru via {@code updateBocMove()} / ubah via {@code updateById()}</li>
 *   <li>{@code modify()} — validasi baris history terpilih (hanya bed aktif)</li>
 * </ul>
 */
@Service
public class BedMutationService {

    private static final int REG_ACTIVE = 1;
    private static final String BED_KOSONG = "0";
    private static final String BED_TERPAKAI = "1";
    private static final String OUT_NOTE_MUTASI = "P";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public BedMutationService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Cari pasien rawat inap (registrasi aktif, no registrasi "I%"). Migrasi dari
     * {@code MsPatientDAO.searchRanapPatient(code, name, address)}.
     */
    public List<BedMutationPatientResponse> searchRanapPatients(String mrCode,
            String patientName, String address) {
        String code = like(normalizeOptionalUpper(mrCode));
        String name = like(normalizeOptionalUpper(patientName));
        String addr = like(normalizeOptionalUpper(address));
        return jdbcTemplate.query(
                "select distinct mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, "
                        + "pat.v_patient_main_addr "
                        + "from tb_medical_record mr "
                        + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                        + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                        + "where mr.v_mr_code like ? and pat.v_patient_name like ? "
                        + "and pat.v_patient_main_addr like ? "
                        + "and reg.v_reg_secondary_id like 'I%' and reg.reg_status = ? "
                        + "limit 100",
                (resultSet, rowNum) -> new BedMutationPatientResponse(
                        resultSet.getInt("n_mr_id"),
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("v_patient_name"),
                        resultSet.getString("v_patient_main_addr")),
                code, name, addr, REG_ACTIVE);
    }

    /**
     * Detail pasien untuk mutasi kamar: MR + registrasi rawat inap terakhir +
     * riwayat mutasi bed. Migrasi dari
     * {@code MutasiKamarManagerImpl.getPatientRanapDetil()}.
     */
    public BedMutationDetailResponse getPatientDetail(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        PatientRow patient = findPatientByMrCode(normalizeMrCode(mrCode));
        RegistrationRow reg = findLastRanapRegistration(patient.mrId);
        if (reg == null) {
            throw new IllegalArgumentException("Anda Tidak Terdaftar Sebagai Pasien Rawat Inap..!\nMutasi Bed Tidak Dapat Dilakukan..!");
        }
        return new BedMutationDetailResponse(
                patient.mrId, patient.mrCode, patient.patientName,
                reg.regId, reg.regNo, displayDate(reg.regDate),
                getBedHistory(reg.regId));
    }

    /**
     * Riwayat mutasi bed untuk satu registrasi. Migrasi dari
     * {@code TbBedOccupancyDAO.getHistoryMove()} +
     * {@code MutasiKamarManagerImpl.getHistoryOfBedMove()}.
     */
    public List<BedMutationHistoryResponse> getBedHistory(Integer regId) {
        return jdbcTemplate.query(
                "select boc.d_whn_create, boc.d_check_in_time, boc.d_check_out_time, "
                        + "bed.n_bed_id, bed.v_bed_desc, "
                        + "hall.n_hall_id, hall.v_hall_name, hall.n_tclass_id "
                        + "from tb_bed_occupancy boc "
                        + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                        + "join ms_room room on room.n_room_id = bed.n_room_id "
                        + "join ms_hall hall on hall.n_hall_id = room.n_hall_id "
                        + "where boc.n_reg_primary_id = ? "
                        + "order by boc.d_whn_create",
                (resultSet, rowNum) -> {
                    Timestamp checkIn = resultSet.getTimestamp("d_check_in_time");
                    Timestamp checkOut = resultSet.getTimestamp("d_check_out_time");
                    return new BedMutationHistoryResponse(
                            resultSet.getTimestamp("d_whn_create").toLocalDateTime().toString(),
                            resultSet.getInt("n_bed_id"),
                            resultSet.getString("v_bed_desc"),
                            resultSet.getInt("n_hall_id"),
                            resultSet.getString("v_hall_name"),
                            resultSet.getInt("n_tclass_id"),
                            checkIn == null ? null : checkIn.toLocalDateTime().format(DISPLAY_DATE_TIME),
                            checkOut == null ? null : checkOut.toLocalDateTime().format(DISPLAY_DATE_TIME),
                            duration(checkIn, checkOut));
                },
                regId);
    }

    /**
     * Ruangan berdasarkan kelas tarif + jumlah bed tersisa. Migrasi dari
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

    /**
     * Simpan mutasi kamar. Migrasi dari {@code MutasiKamarManagerImpl.save()}:
     * <ul>
     *   <li>dengan {@code createdDate} → {@code TbBedOccupancyDAO.updateById()} (ubah bed aktif)</li>
     *   <li>tanpa {@code createdDate} → {@code TbBedOccupancyDAO.updateBocMove()} (mutasi baru)</li>
     * </ul>
     */
    @Transactional
    public BedMutationSaveResultResponse save(BedMutationSaveRequest request, String username) {
        String regNo = normalizeRequired(request.getRegNo(), "NO. REGISTRASI HARUS DI ISI!");
        if (request.getBedId() == null) {
            throw new IllegalArgumentException("BED MUTASI HARUS DI ISI!");
        }
        BedInfo bed = findBed(request.getBedId());
        if (bed == null) {
            throw new IllegalArgumentException("Kode Bed Tidak Ditemukan..!");
        }
        if (BED_TERPAKAI.equals(bed.status)) {
            throw new IllegalArgumentException("Bed Sudah Terpakai, Mohon Diganti Dengan Yang Lain..!");
        }
        RegistrationRow reg = findRegistrationByRegNo(regNo);
        if (reg == null) {
            throw new IllegalArgumentException("Pasien Tidak Terdaftar..!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());

        if (hasText(request.getCreatedDate())) {
            updateExistingBed(reg.regId, request.getCreatedDate(), bed, username, now);
        } else {
            moveToNewBed(reg.regId, bed, username, now);
        }

        return new BedMutationSaveResultResponse(
                true, "Pengubahan Data Berhasil..!", getBedHistory(reg.regId));
    }

    /** Mode ubah: ganti bed pada baris occupancy aktif. Migrasi dari updateById(). */
    private void updateExistingBed(Integer regId, String createdDate, BedInfo newBed,
            String username, Timestamp now) {
        Timestamp created = parseTimestamp(createdDate);
        Integer oldBedId;
        try {
            oldBedId = jdbcTemplate.queryForObject(
                    "select n_bed_primary_id from tb_bed_occupancy "
                            + "where n_reg_primary_id = ? and d_whn_create = ? "
                            + "and d_check_out_time is null",
                    Integer.class, regId, created);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Maaf..Data Bed Tidak Bisa Diubah..!");
        }
        if (oldBedId != null && !oldBedId.equals(newBed.bedId)) {
            jdbcTemplate.update("update ms_bed set v_bed_status = ? where n_bed_id = ?",
                    BED_KOSONG, oldBedId);
        }
        jdbcTemplate.update(
                "update tb_bed_occupancy set n_bed_primary_id = ?, v_who_change = ?, "
                        + "d_whn_change = ? where n_reg_primary_id = ? and d_whn_create = ?",
                newBed.bedId, normalizeActor(username), now, regId, created);
        jdbcTemplate.update("update ms_bed set v_bed_status = ? where n_bed_id = ?",
                BED_TERPAKAI, newBed.bedId);
    }

    /** Mode baru: check-out bed lama + check-in bed baru. Migrasi dari updateBocMove(). */
    private void moveToNewBed(Integer regId, BedInfo newBed, String username, Timestamp now) {
        Integer activeBedId;
        try {
            activeBedId = jdbcTemplate.queryForObject(
                    "select n_bed_primary_id from tb_bed_occupancy "
                            + "where n_reg_primary_id = ? and d_check_out_time is null "
                            + "order by d_whn_create desc limit 1",
                    Integer.class, regId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Anda Tidak Terdaftar Sebagai Pasien Rawat Inap..!\nMutasi Bed Tidak Dapat Dilakukan..!");
        }
        if (activeBedId != null && !activeBedId.equals(newBed.bedId)) {
            jdbcTemplate.update(
                    "update tb_bed_occupancy set d_check_out_time = ?, v_out_note = ? "
                            + "where n_reg_primary_id = ? and d_check_out_time is null",
                    now, OUT_NOTE_MUTASI, regId);
            jdbcTemplate.update("update ms_bed set v_bed_status = ? where n_bed_id = ?",
                    BED_KOSONG, activeBedId);
        }
        jdbcTemplate.update(
                "insert into tb_bed_occupancy (n_reg_primary_id, n_bed_primary_id, "
                        + "d_check_in_time, d_whn_create, v_who_create, v_out_note) "
                        + "values (?, ?, ?, ?, ?, ?)",
                regId, newBed.bedId, now, now, normalizeActor(username), OUT_NOTE_MUTASI);
        jdbcTemplate.update("update ms_bed set v_bed_status = ? where n_bed_id = ?",
                BED_TERPAKAI, newBed.bedId);
    }

    private PatientRow findPatientByMrCode(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select mr.n_mr_id, mr.v_mr_code, pat.n_patient_id, pat.v_patient_name "
                            + "from tb_medical_record mr "
                            + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                            + "where upper(mr.v_mr_code) = ?",
                    (resultSet, rowNum) -> new PatientRow(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getInt("n_patient_id"),
                            resultSet.getString("v_patient_name")),
                    mrCode);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Data Tidak Ditemukan..!");
        }
    }

    private RegistrationRow findLastRanapRegistration(Integer mrId) {
        List<RegistrationRow> rows = jdbcTemplate.query(
                "select n_reg_id, v_reg_secondary_id, d_registration_date from tb_registration "
                        + "where n_mr_id = ? and reg_status = ? "
                        + "and v_reg_secondary_id like 'I%' "
                        + "order by d_registration_date desc limit 1",
                (resultSet, rowNum) -> new RegistrationRow(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_reg_secondary_id"),
                        resultSet.getTimestamp("d_registration_date")),
                mrId, REG_ACTIVE);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private RegistrationRow findRegistrationByRegNo(String regNo) {
        List<RegistrationRow> rows = jdbcTemplate.query(
                "select n_reg_id, v_reg_secondary_id, d_registration_date from tb_registration "
                        + "where v_reg_secondary_id = ? limit 1",
                (resultSet, rowNum) -> new RegistrationRow(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_reg_secondary_id"),
                        resultSet.getTimestamp("d_registration_date")),
                regNo);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private BedInfo findBed(Integer bedId) {
        List<BedInfo> rows = jdbcTemplate.query(
                "select n_bed_id, v_bed_desc, v_bed_status from ms_bed "
                        + "where n_bed_id = ? and v_bed_active_status = 'A'",
                (resultSet, rowNum) -> new BedInfo(
                        resultSet.getInt("n_bed_id"),
                        resultSet.getString("v_bed_desc"),
                        resultSet.getString("v_bed_status")),
                bedId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String duration(Timestamp checkIn, Timestamp checkOut) {
        if (checkIn == null || checkOut == null) {
            return "-";
        }
        LocalDate masuk = checkIn.toLocalDateTime().toLocalDate();
        LocalDate keluar = checkOut.toLocalDateTime().toLocalDate();
        long selisihHari = ChronoUnit.DAYS.between(masuk, keluar);
        return (selisihHari + 1) + " Hari";
    }

    private Timestamp parseTimestamp(String value) {
        try {
            return Timestamp.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Maaf..Data Bed Tidak Bisa Diubah..!");
        }
    }

    private String displayDate(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DISPLAY_DATE);
    }

    private String normalizeRequired(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMrCode(String mrCode) {
        return mrCode == null ? "" : mrCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }

    private String like(String value) {
        return "%" + (value != null ? value : "") + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName)
            throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static final class PatientRow {
        private final int mrId;
        private final String mrCode;
        private final int patientId;
        private final String patientName;

        private PatientRow(int mrId, String mrCode, int patientId, String patientName) {
            this.mrId = mrId;
            this.mrCode = mrCode;
            this.patientId = patientId;
            this.patientName = patientName;
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
