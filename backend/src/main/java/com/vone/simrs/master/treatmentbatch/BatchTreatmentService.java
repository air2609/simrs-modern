package com.vone.simrs.master.treatmentbatch;

import com.vone.simrs.master.treatmentbatch.BatchTreatmentSaveRequest.BatchTreatmentItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0056 (UPDATE MASTER TINDAKAN / batch).
 * Mengikuti logika legacy {@code TreatmentManagerImpl.getAllTreatment()}
 * + {@code updateTreatmentFee()} dan {@code MsTreatmentDAO}.
 */
@Service
public class BatchTreatmentService {

    private final JdbcTemplate jdbcTemplate;

    public BatchTreatmentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar treatment untuk batch update.
     * Mengikuti {@code MsTreatmentDAO.getAllTreatmentFee()}:
     * fee.n_coa is not null and trt.v_treatment_name not in ('-','0').
     */
    public List<BatchTreatmentRowResponse> getTreatments() {
        String sql = "select tf.n_treatment_fee_id, t.n_treatment_id, t.v_treatment_code, "
                + "t.v_treatment_name, tc.v_tclass_desc, "
                + "tf.n_rs_fee, tf.n_doctor_fee, tf.n_medic_fee, tf.n_trtfee_fee, "
                + "coa.v_acct_no "
                + "from ms_treatment_fee tf "
                + "join ms_treatment t on t.n_treatment_id = tf.n_treatment_id "
                + "left join ms_treatment_class tc on tc.n_tclass_id = tf.n_tclass_id "
                + "left join ms_coa coa on coa.n_coa_id = tf.n_coa "
                + "where tf.n_coa is not null "
                + "and t.v_treatment_name not in ('-','0') "
                + "order by t.v_treatment_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new BatchTreatmentRowResponse(
                resultSet.getInt("n_treatment_fee_id"),
                resultSet.getInt("n_treatment_id"),
                resultSet.getString("v_treatment_code"),
                resultSet.getString("v_treatment_name"),
                resultSet.getString("v_tclass_desc"),
                toDouble(resultSet.getObject("n_rs_fee")),
                toDouble(resultSet.getObject("n_doctor_fee")),
                toDouble(resultSet.getObject("n_medic_fee")),
                toDouble(resultSet.getObject("n_trtfee_fee")),
                resultSet.getString("v_acct_no")));
    }

    /**
     * Simpan batch update treatment fee.
     * Mengikuti {@code TreatmentManagerImpl.updateTreatmentFee()}:
     * untuk tiap baris cari fee berdasarkan kode + kelas tarif; bila tidak ada
     * buat baru (dengan group default 9); set jasa RS/dokter/medik, hitung
     * total = medic + doctor + rs; validasi COA; lalu update semua.
     */
    @Transactional
    public BatchTreatmentSaveResult save(BatchTreatmentSaveRequest request, String username) {
        List<BatchTreatmentItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada data untuk disimpan.");
        }

        List<String> invalidCoa = new ArrayList<>();
        String actor = normalizeActor(username);

        for (BatchTreatmentItem item : items) {
            String code = normalize(item.getCode());
            String className = normalize(item.getTreatmentClassDesc());
            if (code == null || code.isEmpty()) {
                throw new IllegalArgumentException("Kode tindakan tidak boleh kosong.");
            }
            if (className == null || className.isEmpty()) {
                throw new IllegalArgumentException("Kelas tarif tidak boleh kosong.");
            }

            double hospitalFee = valueOrZero(item.getHospitalFee());
            double doctorFee = valueOrZero(item.getDoctorFee());
            double medicFee = valueOrZero(item.getMedicFee());
            double totalFee = hospitalFee + doctorFee + medicFee;

            // Cari fee berdasarkan kode + kelas tarif
            Integer feeId = findTreatmentFeeId(code, className);

            if (feeId == null) {
                // Cari treatment berdasarkan kode
                Integer treatmentId = findTreatmentIdByCode(code);
                if (treatmentId == null) {
                    // Buat treatment baru dengan group default 9
                    treatmentId = nextTreatmentId();
                    jdbcTemplate.update(
                            "insert into ms_treatment (n_treatment_id, v_treatment_code, "
                                    + "v_treatment_name, n_tgroup_id, v_who_create, d_whn_create) "
                                    + "values (?, ?, ?, 9, ?, now())",
                            treatmentId, code, normalize(item.getName()), actor);
                }
                // Buat fee baru
                Integer newFeeId = nextTreatmentFeeId();
                Integer classId = findClassIdByDesc(className);
                Integer coaId = findCoaIdByNo(item.getCoaNo());
                if (coaId == null) {
                    invalidCoa.add(item.getCoaNo());
                    continue;
                }
                jdbcTemplate.update(
                        "insert into ms_treatment_fee (n_treatment_fee_id, n_treatment_id, "
                                + "n_tclass_id, n_rs_fee, n_doctor_fee, n_medic_fee, n_trtfee_fee, "
                                + "n_coa, v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                        newFeeId, treatmentId, classId, hospitalFee, doctorFee, medicFee,
                        totalFee, coaId, actor);
            } else {
                // Update fee yang sudah ada
                Integer coaId = findCoaIdByNo(item.getCoaNo());
                if (coaId == null) {
                    invalidCoa.add(item.getCoaNo());
                    continue;
                }
                jdbcTemplate.update(
                        "update ms_treatment_fee set n_rs_fee = ?, n_doctor_fee = ?, "
                                + "n_medic_fee = ?, n_trtfee_fee = ?, n_coa = ?, "
                                + "v_who_change = ?, d_whn_change = now() where n_treatment_fee_id = ?",
                        hospitalFee, doctorFee, medicFee, totalFee, coaId, actor, feeId);
            }
        }

        if (!invalidCoa.isEmpty()) {
            throw new IllegalArgumentException("Invalid Kode COA : " + String.join(", ", invalidCoa));
        }

        return new BatchTreatmentSaveResult(true, "Sukses Mengupdate Data");
    }

    private Integer findTreatmentFeeId(String code, String className) {
        return jdbcTemplate.query(
                "select tf.n_treatment_fee_id from ms_treatment_fee tf "
                        + "join ms_treatment t on t.n_treatment_id = tf.n_treatment_id "
                        + "join ms_treatment_class cl on cl.n_tclass_id = tf.n_tclass_id "
                        + "where t.v_treatment_code = ? and cl.v_tclass_desc = ?",
                resultSet -> resultSet.next() ? resultSet.getInt("n_treatment_fee_id") : null,
                code, className);
    }

    private Integer findTreatmentIdByCode(String code) {
        return jdbcTemplate.query(
                "select n_treatment_id from ms_treatment where v_treatment_code = ?",
                resultSet -> resultSet.next() ? resultSet.getInt("n_treatment_id") : null,
                code);
    }

    private Integer findClassIdByDesc(String className) {
        return jdbcTemplate.query(
                "select n_tclass_id from ms_treatment_class where v_tclass_desc = ?",
                resultSet -> resultSet.next() ? resultSet.getInt("n_tclass_id") : null,
                className);
    }

    private Integer findCoaIdByNo(String coaNo) {
        if (coaNo == null || coaNo.trim().isEmpty()) {
            return null;
        }
        return jdbcTemplate.query(
                "select n_coa_id from ms_coa where v_acct_no = ?",
                resultSet -> resultSet.next() ? resultSet.getInt("n_coa_id") : null,
                coaNo.trim());
    }

    private Integer nextTreatmentId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_treatment_n_treatment_id_seq')", Integer.class);
    }

    private Integer nextTreatmentFeeId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_treatment_fee_n_treatment_fee_id_seq')", Integer.class);
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.valueOf(value.toString());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Hasil simpan batch.
     */
    public static class BatchTreatmentSaveResult {
        private final boolean success;
        private final String message;

        public BatchTreatmentSaveResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
