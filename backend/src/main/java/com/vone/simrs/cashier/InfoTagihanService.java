package com.vone.simrs.cashier;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0023 (FORM INFORMASI TAGIHAN PASIEN / print.zul).
 *
 * <p>
 * Migrasi dari legacy {@code PrintController} + {@code CashierManagerImpl.cariNotaClick()}
 * + {@code CashierDAO.getTbxamination()/getReturNotes()/getItemReturnable()}.
 */
@Service
public class InfoTagihanService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public InfoTagihanService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Daftar transaksi pasien per rentang tanggal + total. Migrasi dari
     * {@code CashierManagerImpl.cariNotaClick()}.
     */
    public InfoTagihanResponse getTransactions(Integer patientId, Integer registrationId,
            String from, String to) {
        if (patientId == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        Timestamp start = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp end = Timestamp.valueOf(toDate.atTime(23, 59, 59));

        List<InfoTagihanRowResponse> rows = new ArrayList<>();
        rows.addAll(getTreatmentRows(patientId, start, end));
        rows.addAll(getItemRows(patientId, start, end));
        rows.addAll(getMiscRows(patientId, start, end));
        rows.addAll(getBedRows(patientId, start, end));

        double totNota = 0;
        double lunas = 0;
        for (InfoTagihanRowResponse row : rows) {
            totNota += row.getJumlah() == null ? 0 : row.getJumlah();
            if ("LUNAS".equals(row.getStatus())) {
                lunas += row.getJumlah() == null ? 0 : row.getJumlah();
            }
        }

        boolean ranap = isRanap(registrationId);
        double retur = 0;
        double deposit = 0;
        if (ranap) {
            retur = computeRetur(patientId, registrationId);
            deposit = findDepositBalance(registrationId);
        }
        double sisa = totNota - (retur + lunas);
        return new InfoTagihanResponse(totNota, deposit, retur, lunas, sisa, rows);
    }

    // ------------------------------------------------------------------ baris

    private List<InfoTagihanRowResponse> getTreatmentRows(Integer patientId, Timestamp start,
            Timestamp end) {
        return jdbcTemplate.query(
                "select n.d_whn_create, treat.v_treatment_name, n.v_note_no, "
                        + "trx.v_who_create, n.n_exam_status, bill.v_pbill_code, "
                        + "trx.n_amount_after_disc "
                        + "from tb_treatment_trx trx "
                        + "join tb_examination n on n.n_exam_id = trx.n_note_id "
                        + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = trx.n_treatment_fee_id "
                        + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                        + "left join tb_patient_bill bill on bill.n_pbill_id = n.n_pbill_id "
                        + "where n.n_patient_id = ? and n.n_exam_status > 0 "
                        + "and n.d_whn_create between ? and ? order by n.d_whn_create, n.n_exam_id",
                (resultSet, rowNum) -> mapRow(resultSet,
                        resultSet.getString("v_treatment_name")),
                patientId, start, end);
    }

    private List<InfoTagihanRowResponse> getItemRows(Integer patientId, Timestamp start,
            Timestamp end) {
        return jdbcTemplate.query(
                "select n.d_whn_create, item.v_item_name, n.v_note_no, "
                        + "trx.v_who_create, n.n_exam_status, bill.v_pbill_code, "
                        + "trx.n_amount_after_disc, trx.n_qty "
                        + "from tb_item_trx trx "
                        + "join tb_examination n on n.n_exam_id = trx.n_note_id "
                        + "join ms_item item on item.n_item_id = trx.n_item_id "
                        + "left join tb_patient_bill bill on bill.n_pbill_id = n.n_pbill_id "
                        + "where n.n_patient_id = ? and n.n_exam_status > 0 and trx.n_qty > 0 "
                        + "and n.d_whn_create between ? and ? order by n.d_whn_create, n.n_exam_id",
                (resultSet, rowNum) -> {
                    InfoTagihanRowResponse row = mapRow(resultSet,
                            resultSet.getString("v_item_name"));
                    return new InfoTagihanRowResponse(
                            row.getTanggal(),
                            row.getKeterangan() + " QTY "
                                    + (resultSet.getObject("n_qty") == null ? "0"
                                            : String.valueOf(resultSet.getObject("n_qty"))),
                            row.getNoteNo(), row.getStaff(), row.getStatus(), row.getKwitansi(),
                            row.getJumlah());
                },
                patientId, start, end);
    }

    private List<InfoTagihanRowResponse> getMiscRows(Integer patientId, Timestamp start,
            Timestamp end) {
        return jdbcTemplate.query(
                "select n.d_whn_create, misc.v_misc_name, n.v_note_no, "
                        + "misc.v_who_create, n.n_exam_status, bill.v_pbill_code, "
                        + "misc.n_amount_after_disc "
                        + "from tb_misc_trx misc "
                        + "join tb_examination n on n.n_exam_id = misc.n_note_id "
                        + "left join tb_patient_bill bill on bill.n_pbill_id = n.n_pbill_id "
                        + "where n.n_patient_id = ? and n.n_exam_status > 0 "
                        + "and n.d_whn_create between ? and ? order by n.d_whn_create, n.n_exam_id",
                (resultSet, rowNum) -> mapRow(resultSet,
                        resultSet.getString("v_misc_name")),
                patientId, start, end);
    }

    private List<InfoTagihanRowResponse> getBedRows(Integer patientId, Timestamp start,
            Timestamp end) {
        return jdbcTemplate.query(
                "select n.d_whn_create, bed.v_bed_desc, n.v_note_no, "
                        + "trx.v_who_create, n.n_exam_status, bill.v_pbill_code, "
                        + "trx.n_amount_after_disc, trx.n_total_hour "
                        + "from tb_bed_trx trx "
                        + "join tb_examination n on n.n_exam_id = trx.n_note_id "
                        + "join ms_bed bed on bed.n_bed_id = trx.n_bed_id "
                        + "left join tb_patient_bill bill on bill.n_pbill_id = n.n_pbill_id "
                        + "where n.n_patient_id = ? and n.n_exam_status > 0 "
                        + "and n.d_whn_create between ? and ? order by n.d_whn_create, n.n_exam_id",
                (resultSet, rowNum) -> {
                    InfoTagihanRowResponse row = mapRow(resultSet,
                            resultSet.getString("v_bed_desc"));
                    String hours = resultSet.getObject("n_total_hour") == null ? "0"
                            : String.valueOf(resultSet.getObject("n_total_hour"));
                    return new InfoTagihanRowResponse(
                            row.getTanggal(),
                            row.getKeterangan() + " - " + hours + " HARI",
                            row.getNoteNo(), row.getStaff(), row.getStatus(), row.getKwitansi(),
                            row.getJumlah());
                },
                patientId, start, end);
    }

    private InfoTagihanRowResponse mapRow(java.sql.ResultSet resultSet, String keterangan)
            throws java.sql.SQLException {
        String status;
        String kwitansi;
        if (resultSet.getString("v_pbill_code") != null) {
            status = "LUNAS";
            kwitansi = resultSet.getString("v_pbill_code");
        } else if (getNullableInteger(resultSet, "n_exam_status") != null
                && getNullableInteger(resultSet, "n_exam_status") == 1) {
            status = "BARU";
            kwitansi = "-";
        } else if (getNullableInteger(resultSet, "n_exam_status") != null
                && getNullableInteger(resultSet, "n_exam_status") == 2) {
            status = "VALIDASI";
            kwitansi = "-";
        } else {
            status = "";
            kwitansi = "-";
        }
        return new InfoTagihanRowResponse(
                toDisplayDate(resultSet.getTimestamp("d_whn_create")),
                keterangan,
                resultSet.getString("v_note_no"),
                resultSet.getString("v_who_create"),
                status, kwitansi,
                toDouble(resultSet.getObject("n_amount_after_disc")));
    }

    // ------------------------------------------------------------------ retur & deposit

    private boolean isRanap(Integer registrationId) {
        if (registrationId == null) {
            return false;
        }
        List<String> rows = jdbcTemplate.query(
                "select v_reg_secondary_id from tb_registration where n_reg_id = ?",
                (resultSet, rowNum) -> resultSet.getString("v_reg_secondary_id"),
                registrationId);
        return !rows.isEmpty() && rows.get(0) != null && rows.get(0).startsWith("I");
    }

    /** Retur: nota retur tervalidasi, atau estimasi item returnable. */
    private double computeRetur(Integer patientId, Integer registrationId) {
        Double returNotes = jdbcTemplate.query(
                "select coalesce(sum(n_trx_value), 0) from tb_retur_pharmacy_trx "
                        + "where n_reg_id = ? and n_status = 2",
                (resultSet, rowNum) -> resultSet.getDouble(1),
                registrationId).stream().findFirst().orElse(0.0);
        if (returNotes > 0) {
            return returNotes;
        }
        // estimasi item returnable (getItemReturnable)
        List<Double> values = jdbcTemplate.query(
                "select coalesce(sum((inv.n_qty - inv.n_qty_out) * sell.n_selling_price), 0) "
                        + "from tb_patient_inventory inv "
                        + "join ms_item item on item.n_item_id = inv.n_item_id "
                        + "join ms_item_selling_price sell on sell.n_item_id = item.n_item_id "
                        + "where inv.n_pat_id = ? and item.v_item_returnable = 'Y'",
                (resultSet, rowNum) -> resultSet.getDouble(1),
                patientId);
        return values.isEmpty() ? 0.0 : values.get(0);
    }

    private Double findDepositBalance(Integer regId) {
        List<Double> rows = jdbcTemplate.query(
                "select n_balance from tb_patient_deposit where n_reg_id = ? "
                        + "order by d_whn_create desc limit 1",
                (resultSet, rowNum) -> toDouble(resultSet.getObject("n_balance")),
                regId);
        return rows.isEmpty() ? 0.0 : rows.get(0);
    }

    // ------------------------------------------------------------------ helpers

    private String toDisplayDate(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toLocalDate().format(DATE_DISPLAY);
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
}
