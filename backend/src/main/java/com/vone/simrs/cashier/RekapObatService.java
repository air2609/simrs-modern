package com.vone.simrs.cashier;

import com.vone.simrs.auth.LegacyAuthService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0208 (FORM INFORMASI REKAP OBAT / rekapObat.zul).
 *
 * <p>
 * Migrasi dari legacy {@code RekapObatController} + {@code CashierManagerImpl.getRekapObat()}
 * + {@code CashierDAO.getItemTrx(reg, type)/getRetur(reg, itemId)}.
 */
@Service
public class RekapObatService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public RekapObatService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ------------------------------------------------------------------ pasien ranap

    /** Cari pasien rawat inap (searchRanapPatient). */
    public List<com.vone.simrs.ward.WardPatientOptionResponse> searchRanapPatients(
            String mrCode, String patientName, String address) {
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
                    return new com.vone.simrs.ward.WardPatientOptionResponse(
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

    /** Detail pasien ranap: reg + kelas tarif + bed + tipe pasien. */
    public CashierPatientDetailResponse getPatientDetail(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        try {
            return jdbcTemplate.queryForObject(
                    "select reg.n_reg_id, reg.v_reg_secondary_id, reg.n_transfer_reg_id, "
                            + "mr.v_mr_code, mr.n_patient_id, "
                            + "pat.v_patient_name, pat.v_patient_main_addr, "
                            + "pt.v_tpatient_desc, tclass.v_tclass_desc, bed.v_bed_desc "
                            + "from tb_registration reg "
                            + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                            + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                            + "left join ms_patient_type pt on pt.n_patient_type_id = pat.n_patient_type_id "
                            + "left join tb_bed_occupancy boc on boc.n_reg_primary_id = reg.n_reg_id "
                            + "and boc.d_check_out_time is null "
                            + "left join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                            + "left join ms_treatment_class tclass on tclass.n_tclass_id = bed.n_tclass_id "
                            + "where reg.reg_status = 1 and reg.v_reg_secondary_id like 'I%' "
                            + "and upper(mr.v_mr_code) = ? "
                            + "order by reg.d_registration_date desc limit 1",
                    (resultSet, rowNum) -> new CashierPatientDetailResponse(
                            resultSet.getInt("n_patient_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getInt("n_reg_id"),
                            resultSet.getString("v_reg_secondary_id"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_main_addr"),
                            resultSet.getString("v_tpatient_desc"),
                            resultSet.getString("v_bed_desc"),
                            true,
                            0.0),
                    normalizeMrCode(mrCode));
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("PASIEN RAWAT INAP TIDAK DITEMUKAN!");
        }
    }

    // ------------------------------------------------------------------ rekap

    /**
     * Rekap obat pasien ranap dengan filter tipe obat. Migrasi dari
     * {@code CashierManagerImpl.getRekapObat()} + {@code CashierDAO.getItemTrx()/getRetur()}.
     */
    public RekapObatResponse getRekap(Integer registrationId, Integer type) {
        if (registrationId == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        Integer parentRegId = findParentRegId(registrationId);
        Integer effectiveType = (type == null || type == 10) ? 10 : type;

        List<RekapRow> rows = jdbcTemplate.query(
                "select item.n_item_id id, item.v_item_code kode, item.v_item_name nama, "
                        + "case when item.n_type = 1 then 'PSIKOTROPIKA' "
                        + "when item.n_type = 2 then 'NARKOTIKA' "
                        + "when item.n_type = 3 then 'GENERIK' "
                        + "when item.n_type = 4 then 'PATEN' "
                        + "when item.n_type = 5 then 'BPJS' end as tipeObat, "
                        + "sum(trx.n_qty) as qty, sum(trx.n_amount_after_disc) as total "
                        + "from tb_item_trx trx "
                        + "join ms_item item on item.n_item_id = trx.n_item_id "
                        + "join tb_examination nota on nota.n_exam_id = trx.n_note_id "
                        + "join tb_registration reg on reg.n_reg_id = nota.n_reg_id "
                        + "where nota.n_exam_status not in (0, 4) "
                        + "and reg.n_reg_id in (?, ?) "
                        + (effectiveType != 10 ? "and item.n_type = ? " : "")
                        + "group by item.n_item_id, item.v_item_code, item.v_item_name, tipeObat "
                        + "order by item.v_item_name",
                (resultSet, rowNum) -> new RekapRow(
                        resultSet.getInt("id"),
                        resultSet.getString("kode"),
                        resultSet.getString("nama"),
                        resultSet.getString("tipeObat"),
                        getNullableInteger(resultSet, "qty"),
                        toDouble(resultSet.getObject("total"))),
                buildParams(registrationId, parentRegId, effectiveType));

        List<RekapObatRowResponse> result = new ArrayList<>();
        double totTransaksi = 0;
        double totRetur = 0;
        for (RekapRow row : rows) {
            double trx = row.total == null ? 0 : row.total;
            totTransaksi += trx;
            ReturRow retur = findRetur(registrationId, row.itemId);
            int jmlRetur = 0;
            double totReturItem = 0;
            if (retur != null) {
                jmlRetur = retur.qty == null ? 0 : retur.qty;
                totReturItem = retur.total == null ? 0 : retur.total;
                totRetur += totReturItem;
            }
            result.add(new RekapObatRowResponse(
                    row.itemId, row.code, row.name, row.drugType,
                    row.qty == null ? 0 : row.qty, trx,
                    jmlRetur, totReturItem, trx - totReturItem));
        }
        return new RekapObatResponse(totTransaksi, totRetur, totTransaksi - totRetur, result);
    }

    private Object[] buildParams(Integer regId, Integer parentRegId, Integer type) {
        List<Object> params = new ArrayList<>();
        params.add(regId);
        params.add(parentRegId == null ? regId : parentRegId);
        if (type != 10) {
            params.add(type);
        }
        return params.toArray();
    }

    /** Registrasi rajal asal (parent) via n_transfer_reg_id. */
    private Integer findParentRegId(Integer regId) {
        List<Integer> rows = jdbcTemplate.query(
                "select n_transfer_reg_id from tb_registration where n_reg_id = ?",
                (resultSet, rowNum) -> getNullableInteger(resultSet, "n_transfer_reg_id"),
                regId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ReturRow findRetur(Integer regId, Integer itemId) {
        List<ReturRow> rows = jdbcTemplate.query(
                "select trx.n_item_id as item_id, sum(trx.n_qty) as qty, sum(trx.n_value) as total "
                        + "from tb_retur_pharmacy_detail_trx trx "
                        + "join tb_retur_pharmacy_trx retur on retur.n_retur_id = trx.n_retur_id "
                        + "where retur.n_reg_id = ? and trx.n_item_id = ? "
                        + "group by trx.n_item_id",
                (resultSet, rowNum) -> new ReturRow(
                        getNullableInteger(resultSet, "qty"),
                        toDouble(resultSet.getObject("total"))),
                regId, itemId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ------------------------------------------------------------------ helpers

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

    private static final class RekapRow {
        private final int itemId;
        private final String code;
        private final String name;
        private final String drugType;
        private final Integer qty;
        private final Double total;

        private RekapRow(int itemId, String code, String name, String drugType, Integer qty,
                Double total) {
            this.itemId = itemId;
            this.code = code;
            this.name = name;
            this.drugType = drugType;
            this.qty = qty;
            this.total = total;
        }
    }

    private static final class ReturRow {
        private final Integer qty;
        private final Double total;

        private ReturRow(Integer qty, Double total) {
            this.qty = qty;
            this.total = total;
        }
    }
}
