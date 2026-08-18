package com.vone.simrs.ward;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0004 (FORM TRANSAKSI BED / bedTransaction.zul).
 *
 * <p>
 * Migrasi dari legacy {@code BedTransactionController} +
 * {@code BedTransactionManagerImpl} + {@code BedTransactionDAO} +
 * {@code NoteDAO.save()} (journal bed).
 */
@Service
public class BedTransactionService {

    private static final int REG_ACTIVE = 1;
    private static final short BELUM_LUNAS = 0;
    private static final short NOTE_VALIDATED = 2;
    private static final String BED_KOSONG = "0";
    private static final String CLOSE_NOTE = "K";
    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter NOTE_DATE = DateTimeFormatter.ofPattern("yyMM");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public BedTransactionService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * History bed pasien ranap (tree HISTORY BED PASIEN). Migrasi dari
     * {@code BedTransactionDAO.getBocsBaseOnRegistration()} +
     * {@code BedTransactionManagerImpl.getBedsOccupancy()}.
     */
    public List<BedOccupancyResponse> getBedHistory(Integer registrationId) {
        if (registrationId == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        List<OccupancyRow> occupancies = jdbcTemplate.query(
                "select boc.n_bed_primary_id, bed.v_bed_desc, bed.n_bed_price, "
                        + "boc.d_check_in_time, boc.d_check_out_time, "
                        + "room.n_hall_id, bed.n_tclass_id "
                        + "from tb_bed_occupancy boc "
                        + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                        + "join ms_room room on room.n_room_id = bed.n_room_id "
                        + "where boc.n_reg_primary_id = ? order by boc.d_whn_create",
                (resultSet, rowNum) -> new OccupancyRow(
                        resultSet.getInt("n_bed_primary_id"),
                        resultSet.getString("v_bed_desc"),
                        resultSet.getDouble("n_bed_price"),
                        resultSet.getTimestamp("d_check_in_time"),
                        resultSet.getTimestamp("d_check_out_time"),
                        getNullableInteger(resultSet, "n_hall_id"),
                        getNullableInteger(resultSet, "n_tclass_id")),
                registrationId);

        List<BedOccupancyResponse> result = new ArrayList<>();
        for (OccupancyRow occupancy : occupancies) {
            LocalDate checkIn = occupancy.checkIn.toLocalDateTime().toLocalDate();
            LocalDate checkOut = occupancy.checkOut == null ? null
                    : occupancy.checkOut.toLocalDateTime().toLocalDate();
            LocalDate end = checkOut != null ? checkOut : LocalDate.now();

            List<LocalDate> dates = new ArrayList<>();
            for (LocalDate d = checkIn; !d.isAfter(end); d = d.plusDays(1)) {
                dates.add(d);
            }

            // nota yang sudah dibuat untuk bed ini (untuk mengisi kolom NO. NOTA)
            Map<String, String> noteByDate = findBedTrxNotes(occupancy.bedId, checkIn, end);

            List<BedDayResponse> days = new ArrayList<>();
            for (LocalDate date : dates) {
                String noteNo = noteByDate.getOrDefault(date.toString(), "-");
                days.add(new BedDayResponse(
                        date.format(DATE_DISPLAY),
                        "1 HARI",
                        occupancy.bedPrice,
                        occupancy.bedPrice,
                        noteNo));
            }
            result.add(new BedOccupancyResponse(
                    occupancy.bedId, occupancy.bedDesc,
                    checkIn.format(DATE_DISPLAY),
                    checkOut == null ? "" : checkOut.format(DATE_DISPLAY),
                    days));
        }
        return result;
    }

    /**
     * Nota bed trx yang sudah ada per tanggal (untuk kolom NO. NOTA pada tree).
     */
    private Map<String, String> findBedTrxNotes(Integer bedId, LocalDate from, LocalDate to) {
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query(
                "select trx.d_date_from, trx.d_date_to, nota.v_note_no "
                        + "from tb_bed_trx trx "
                        + "join tb_examination nota on nota.n_exam_id = trx.n_note_id "
                        + "where trx.n_bed_id = ? and trx.d_date_to >= ? and trx.d_date_from <= ?",
                (resultSet, rowNum) -> {
                    LocalDate dateFrom = resultSet.getTimestamp("d_date_from")
                            .toLocalDateTime().toLocalDate();
                    LocalDate dateTo = resultSet.getTimestamp("d_date_to")
                            .toLocalDateTime().toLocalDate();
                    String noteNo = resultSet.getString("v_note_no");
                    for (LocalDate d = dateFrom; !d.isAfter(dateTo); d = d.plusDays(1)) {
                        result.put(d.toString(), noteNo);
                    }
                    return null;
                },
                bedId, Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.atTime(23, 59, 59)));
        return result;
    }

    /**
     * Buat nota bed untuk tanggal-tanggal terpilih. Migrasi dari legacy
     * {@code BedTransactionController.createNote()} + {@code BedTransactionDAO.save()}
     * + {@code NoteDAO.save()} (nota langsung berstatus VALIDASI + journal bed).
     */
    @Transactional
    public BedNoteCreateResultResponse createNote(BedNoteCreateRequest request, String username) {
        if (request.getRows() == null || request.getRows().isEmpty()) {
            throw new IllegalArgumentException("pilih.transaksi.bed.yang.akan.dibuat.notanya");
        }
        if (request.getRegistrationId() == null || request.getUnitId() == null) {
            throw new IllegalArgumentException("PILIH PASIEN & LOKASI TERLEBIH DAHULU!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // urutkan baris per tanggal
        List<BedNoteRowRequest> rows = new ArrayList<>(request.getRows());
        rows.sort(Comparator.comparing(r -> r.getDate()));
        LocalDate firstDate = parseDate(rows.get(0).getDate());
        LocalDate lastDate = parseDate(rows.get(rows.size() - 1).getDate());
        if (firstDate == null || lastDate == null) {
            throw new IllegalArgumentException("Tanggal tidak valid!");
        }

        double total = 0;
        for (BedNoteRowRequest row : rows) {
            total += row.getHarga() == null ? 0 : row.getHarga();
        }
        int jumlahJam = rows.size();

        BedInfo bed = findBed(rows.get(0).getBedId());
        RegInfo reg = findRegistration(request.getRegistrationId());
        UnitInfo unit = findUnit(request.getUnitId());

        String noteNo = generateRanapNoteNumber(now, unit.code);
        jdbcTemplate.update(
                "insert into tb_examination (n_exam_id, v_note_no, n_exam_status, n_payment_status, "
                        + "n_unit_id, n_patient_id, n_reg_id, n_total_amount, "
                        + "d_whn_create, v_who_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                getNextSequence("tb_examination_n_exam_id_seq"), noteNo,
                (int) NOTE_VALIDATED, (int) BELUM_LUNAS,
                request.getUnitId(), reg.patientId, request.getRegistrationId(), total,
                now, username);

        Integer noteId = jdbcTemplate.queryForObject(
                "select max(n_exam_id) from tb_examination where v_note_no = ?", Integer.class,
                noteNo);
        jdbcTemplate.update(
                "insert into tb_bed_trx (n_bed_id, n_note_id, d_date_from, d_date_to, "
                        + "n_total_hour, n_fee, v_disc_type, n_disc_amount, n_amount_after_disc, "
                        + "n_patient_id, v_who_create, d_who_create) "
                        + "values (?, ?, ?, ?, ?, ?, null, 0, ?, ?, ?, ?)",
                rows.get(0).getBedId(), noteId,
                Timestamp.valueOf(firstDate.atStartOfDay()),
                Timestamp.valueOf(lastDate.atTime(23, 59, 59)),
                (short) jumlahJam, total, total, reg.patientId, username, now);

        Integer bedTrxId = jdbcTemplate.queryForObject(
                "select max(n_btrx_id) from tb_bed_trx where n_note_id = ?", Integer.class, noteId);

        for (BedNoteRowRequest row : rows) {
            LocalDate date = parseDate(row.getDate());
            jdbcTemplate.update(
                    "insert into tb_bor (bed_id, tclass_id, hall_id, bed_date, bed_trx_id) "
                            + "values (?, ?, ?, ?, ?)",
                    rows.get(0).getBedId(), bed.tclassId, bed.hallId,
                    java.sql.Date.valueOf(date), bedTrxId);
        }

        // jika hari ini terpilih -> tutup occupancy + bed kosong
        boolean isClosed = rows.stream().anyMatch(r -> {
            LocalDate d = parseDate(r.getDate());
            return d != null && d.equals(LocalDate.now());
        });
        if (isClosed) {
            jdbcTemplate.update(
                    "update tb_bed_occupancy set d_check_out_time = ?, v_out_note = ?, "
                            + "v_who_change = ?, d_whn_change = ? "
                            + "where n_reg_primary_id = ? and n_bed_primary_id = ? "
                            + "and d_check_out_time is null",
                    now, CLOSE_NOTE, username, now,
                    request.getRegistrationId(), rows.get(0).getBedId());
            jdbcTemplate.update(
                    "update ms_bed set v_bed_status = ? where n_bed_id = ?",
                    BED_KOSONG, rows.get(0).getBedId());
        }

        createBedJournal(noteId, noteNo, bed, total, jumlahJam, now, username);
        return new BedNoteCreateResultResponse(true, "Nota bed berhasil dibuat.", noteNo);
    }

    /**
     * Journal bed saat nota dibuat. Migrasi dari {@code NoteDAO.save()}:
     * DR AR / CR coa bed.
     */
    private void createBedJournal(Integer noteId, String noteNo, BedInfo bed, double amount,
            int totalHour, Timestamp now, String username) {
        Integer coaArId = findCoaIdByGimKey("COA_INPATIENT_AR");
        if (coaArId == null) {
            throw new IllegalStateException("COA AR belum dikonfigurasi.");
        }
        if (bed.coaId == null) {
            throw new IllegalStateException("trx.bed.coa.null");
        }
        String batchId = "AR" + String.format("%015d", getNextSequence("sq_journal_trx"));
        String memo = "BEDCODE:" + bed.bedCode + ";TOTALHOUR:" + totalHour
                + ";FEE:" + amount + ";DISCOUNT:0";
        insertJournal(batchId, noteNo, memo, amount, 0, now, username, coaArId);
        insertJournal(batchId, noteNo, memo, 0, amount, now, username, bed.coaId);
    }

    private void insertJournal(String batchId, String voucherNo, String desc, double debit,
            double credit, Timestamp now, String username, Integer coaId) {
        Integer journalId = getNextSequence("tb_journal_trx_n_journal_id_seq");
        jdbcTemplate.update(
                "insert into tb_journal_trx (n_journal_id, v_journal_batch_id, v_voucher_no, "
                        + "v_desc, n_debit, n_credit, d_whn_create, v_who_create, d_apl_date, n_coa_id) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                journalId, batchId, voucherNo, desc, debit, credit, now, username, now, coaId);
    }

    private Integer findCoaIdByGimKey(String gimKey) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coa.n_coa_id from ms_gim gim "
                            + "join ms_coa coa on coa.v_acct_no = gim.v_value where gim.v_key = ?",
                    Integer.class, gimKey);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ helpers

    private BedInfo findBed(Integer bedId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select bed.n_bed_id, bed.v_bed_code, bed.v_bed_desc, bed.n_bed_price, "
                            + "bed.n_coa, bed.n_tclass_id, room.n_hall_id "
                            + "from ms_bed bed "
                            + "join ms_room room on room.n_room_id = bed.n_room_id "
                            + "where bed.n_bed_id = ?",
                    (resultSet, rowNum) -> new BedInfo(
                            resultSet.getInt("n_bed_id"),
                            resultSet.getString("v_bed_code"),
                            resultSet.getDouble("n_bed_price"),
                            getNullableInteger(resultSet, "n_coa"),
                            getNullableInteger(resultSet, "n_tclass_id"),
                            getNullableInteger(resultSet, "n_hall_id")),
                    bedId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Bed tidak ditemukan.");
        }
    }

    private RegInfo findRegistration(Integer regId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select reg.n_reg_id, mr.n_patient_id from tb_registration reg "
                            + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                            + "where reg.n_reg_id = ?",
                    (resultSet, rowNum) -> new RegInfo(
                            resultSet.getInt("n_reg_id"),
                            getNullableInteger(resultSet, "n_patient_id")),
                    regId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Registrasi tidak ditemukan.");
        }
    }

    private UnitInfo findUnit(Integer unitId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_unit_id, v_unit_code from ms_unit where n_unit_id = ?",
                    (resultSet, rowNum) -> new UnitInfo(
                            resultSet.getInt("n_unit_id"),
                            resultSet.getString("v_unit_code")),
                    unitId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Unit tidak ditemukan.");
        }
    }

    private String generateRanapNoteNumber(Timestamp now, String unitCode) {
        Integer sequence = getNextSequence("nota_ranap_seq");
        return "I-" + unitCode + "-"
                + now.toLocalDateTime().format(NOTE_DATE) + "-"
                + String.format("%06d", sequence);
    }

    private Integer getNextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    // ------------------------------------------------------------------ rows

    private static final class OccupancyRow {
        private final int bedId;
        private final String bedDesc;
        private final double bedPrice;
        private final Timestamp checkIn;
        private final Timestamp checkOut;
        private final Integer hallId;
        private final Integer tclassId;

        private OccupancyRow(int bedId, String bedDesc, double bedPrice, Timestamp checkIn,
                Timestamp checkOut, Integer hallId, Integer tclassId) {
            this.bedId = bedId;
            this.bedDesc = bedDesc;
            this.bedPrice = bedPrice;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.hallId = hallId;
            this.tclassId = tclassId;
        }
    }

    private static final class BedInfo {
        private final int bedId;
        private final String bedCode;
        private final double bedPrice;
        private final Integer coaId;
        private final Integer tclassId;
        private final Integer hallId;

        private BedInfo(int bedId, String bedCode, double bedPrice, Integer coaId,
                Integer tclassId, Integer hallId) {
            this.bedId = bedId;
            this.bedCode = bedCode;
            this.bedPrice = bedPrice;
            this.coaId = coaId;
            this.tclassId = tclassId;
            this.hallId = hallId;
        }
    }

    private static final class RegInfo {
        private final int regId;
        private final Integer patientId;

        private RegInfo(int regId, Integer patientId) {
            this.regId = regId;
            this.patientId = patientId;
        }
    }

    private static final class UnitInfo {
        private final int unitId;
        private final String code;

        private UnitInfo(int unitId, String code) {
            this.unitId = unitId;
            this.code = code;
        }
    }
}
