package com.vone.simrs.accounting;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0050 (FORM ACCT DEFAULT / acctDefaultDataInput.zul).
 *
 * <p>
 * Migrasi dari legacy {@code AcctDefaultDataInput}:
 * <ul>
 * <li>{@code AcctDefaultDataInput.init()} →
 * {@link #getMasters()}</li>
 * <li>{@code AcctDefaultDataInput.doSave()} →
 * {@link #save(AcctDefaultSaveRequest, String)}</li>
 * </ul>
 *
 * <p>
 * Nilai default disimpan pada tabel {@code ms_gim} (kunci COA_INPATIENT_AR,
 * COA_OUTPATIENT_AR, COA_AP, COA_PATIENT_AP, COA_PPH21, COA_MISC_TRX,
 * COA_STAFF_AP). Catatan: legacy {@code doSave()} lupa menyimpan PPH21 dan
 * MISC_TRX; pada migrasi ini ketujuh field disimpan seluruhnya.
 */
@Service
public class AcctDefaultService {

    private static final String KEY_INPATIENT_AR = "COA_INPATIENT_AR";
    private static final String KEY_OUTPATIENT_AR = "COA_OUTPATIENT_AR";
    private static final String KEY_AP = "COA_AP";
    private static final String KEY_PATIENT_AP = "COA_PATIENT_AP";
    private static final String KEY_PPH21 = "COA_PPH21";
    private static final String KEY_MISC_TRX = "COA_MISC_TRX";
    private static final String KEY_STAFF_AP = "COA_STAFF_AP";

    private final JdbcTemplate jdbcTemplate;

    public AcctDefaultService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Data master: daftar opsi COA (COA_ALL) + nilai default GIM yang aktif.
     * Migrasi dari legacy {@code CoaController.getCoaForSelect()} +
     * {@code getGimByCode()} untuk ketujuh kunci.
     */
    public AcctDefaultMastersResponse getMasters() {
        List<AcctDefaultMastersResponse.CoaOption> options = jdbcTemplate.query(
                "select n_coa_id, v_acct_no, v_acct_name from ms_coa order by v_acct_no",
                (resultSet, rowNum) -> new AcctDefaultMastersResponse.CoaOption(
                        resultSet.getInt("n_coa_id"),
                        resultSet.getString("v_acct_no"),
                        resultSet.getString("v_acct_name")));

        return new AcctDefaultMastersResponse(
                options,
                gimValue(KEY_INPATIENT_AR),
                gimValue(KEY_OUTPATIENT_AR),
                gimValue(KEY_AP),
                gimValue(KEY_PATIENT_AP),
                gimValue(KEY_PPH21),
                gimValue(KEY_MISC_TRX),
                gimValue(KEY_STAFF_AP));
    }

    /**
     * Simpan nilai default COA ke ms_gim. Migrasi dari legacy
     * {@code AcctDefaultDataInput.doSave()}. Field kosong diabaikan (tidak
     * mengubah GIM yang ada).
     */
    @Transactional
    public void save(AcctDefaultSaveRequest request, String username) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String actor = normalize(username);
        upsertGim(KEY_INPATIENT_AR, request.getInAr(), actor, now);
        upsertGim(KEY_OUTPATIENT_AR, request.getOutAr(), actor, now);
        upsertGim(KEY_AP, request.getAp(), actor, now);
        upsertGim(KEY_PATIENT_AP, request.getApPatient(), actor, now);
        upsertGim(KEY_PPH21, request.getPph21(), actor, now);
        upsertGim(KEY_MISC_TRX, request.getMiscTrx(), actor, now);
        upsertGim(KEY_STAFF_AP, request.getApStaff(), actor, now);
    }

    private void upsertGim(String key, String acctNo, String actor, Timestamp now) {
        if (acctNo == null || acctNo.trim().isEmpty()) {
            return;
        }
        jdbcTemplate.update(
                "insert into ms_gim (v_key, v_value, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?) "
                        + "on conflict (v_key) do update set v_value = excluded.v_value, "
                        + "v_who_change = excluded.v_who_create, d_whn_change = ?",
                key, acctNo.trim().toUpperCase(Locale.ROOT), actor, now, now);
    }

    private String gimValue(String key) {
        try {
            return jdbcTemplate.queryForObject(
                    "select v_value from ms_gim where v_key = ?",
                    String.class, key);
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? "SYSTEM" : value.trim().toUpperCase(Locale.ROOT);
    }
}
