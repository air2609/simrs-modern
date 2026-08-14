package com.vone.simrs.master.treatment;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0026 (TREATMENT MASTER).
 * Mengikuti logika legacy {@code TreatmentManagerImpl} +
 * {@code MsTreatmentFeeDAO} + {@code MsTreatmentDAO}.
 */
@Service
public class TreatmentService {

    private final JdbcTemplate jdbcTemplate;

    public TreatmentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar treatment (join ms_treatment_fee + ms_treatment + group + class).
     * Mengikuti {@code MsTreatmentFeeDAO.getAllTreatmentFee()}.
     */
    public List<TreatmentRowResponse> getTreatments() {
        String sql = "select tf.n_treatment_fee_id, t.n_treatment_id, t.v_treatment_code, "
                + "t.v_treatment_name, tf.n_tgroup_id, tg.v_tgroup_name, "
                + "tf.n_tclass_id, tc.v_tclass_desc, "
                + "tf.n_rs_fee, tf.n_doctor_fee, tf.n_medic_fee, tf.n_trtfee_fee, "
                + "tf.n_coa, coa.v_acct_no, coa.v_acct_name "
                + "from ms_treatment_fee tf "
                + "join ms_treatment t on t.n_treatment_id = tf.n_treatment_id "
                + "left join ms_treatment_group tg on tg.n_tgroup_id = tf.n_tgroup_id "
                + "left join ms_treatment_class tc on tc.n_tclass_id = tf.n_tclass_id "
                + "left join ms_coa coa on coa.n_coa_id = tf.n_coa "
                + "order by t.v_treatment_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new TreatmentRowResponse(
                resultSet.getInt("n_treatment_fee_id"),
                resultSet.getInt("n_treatment_id"),
                resultSet.getString("v_treatment_code"),
                resultSet.getString("v_treatment_name"),
                toInteger(resultSet.getObject("n_tgroup_id")),
                resultSet.getString("v_tgroup_name"),
                toInteger(resultSet.getObject("n_tclass_id")),
                resultSet.getString("v_tclass_desc"),
                toDouble(resultSet.getObject("n_rs_fee")),
                toDouble(resultSet.getObject("n_doctor_fee")),
                toDouble(resultSet.getObject("n_medic_fee")),
                toDouble(resultSet.getObject("n_trtfee_fee")),
                toInteger(resultSet.getObject("n_coa")),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")));
    }

    /**
     * Pencarian treatment berdasarkan kode, nama, atau kelas tarif.
     * Mengikuti {@code MsTreatmentDAO.searchTreatement()} pada legacy.
     */
    public List<TreatmentRowResponse> searchTreatments(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return getTreatments();
        }
        String like = "%" + normalized + "%";
        String sql = "select tf.n_treatment_fee_id, t.n_treatment_id, t.v_treatment_code, "
                + "t.v_treatment_name, tf.n_tgroup_id, tg.v_tgroup_name, "
                + "tf.n_tclass_id, tc.v_tclass_desc, "
                + "tf.n_rs_fee, tf.n_doctor_fee, tf.n_medic_fee, tf.n_trtfee_fee, "
                + "tf.n_coa, coa.v_acct_no, coa.v_acct_name "

                + "from ms_treatment_fee tf "
                + "join ms_treatment t on t.n_treatment_id = tf.n_treatment_id "
                + "left join ms_treatment_group tg on tg.n_tgroup_id = tf.n_tgroup_id "
                + "left join ms_treatment_class tc on tc.n_tclass_id = tf.n_tclass_id "
                + "left join ms_coa coa on coa.n_coa_id = tf.n_coa "
                + "where upper(t.v_treatment_code) like ? "

                + "or upper(t.v_treatment_name) like ? "
                + "or upper(tc.v_tclass_desc) like ? "
                + "order by t.v_treatment_code limit 100";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new TreatmentRowResponse(
                resultSet.getInt("n_treatment_fee_id"),
                resultSet.getInt("n_treatment_id"),
                resultSet.getString("v_treatment_code"),
                resultSet.getString("v_treatment_name"),
                toInteger(resultSet.getObject("n_tgroup_id")),
                resultSet.getString("v_tgroup_name"),
                toInteger(resultSet.getObject("n_tclass_id")),
                resultSet.getString("v_tclass_desc"),
                toDouble(resultSet.getObject("n_rs_fee")),
                toDouble(resultSet.getObject("n_doctor_fee")),
                toDouble(resultSet.getObject("n_medic_fee")),
                toDouble(resultSet.getObject("n_trtfee_fee")),
                toInteger(resultSet.getObject("n_coa")),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")), like, like, like);

    }

    /**
     * Opsi dropdown group tindakan.
     */
    public List<TreatmentGroupOptionResponse> getTreatmentGroupOptions() {

        String sql = "select n_tgroup_id, v_tgroup_code, v_tgroup_name "
                + "from ms_treatment_group order by v_tgroup_code";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new TreatmentGroupOptionResponse(
                resultSet.getInt("n_tgroup_id"),
                resultSet.getString("v_tgroup_code"),
                resultSet.getString("v_tgroup_name")));
    }

    /**
     * Opsi dropdown kelas tarif.
     */
    public List<TreatmentClassOptionResponse> getTreatmentClassOptions() {
        String sql = "select n_tclass_id, v_tclass_code, v_tclass_desc "
                + "from ms_treatment_class order by v_tclass_code";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new TreatmentClassOptionResponse(
                resultSet.getInt("n_tclass_id"),
                resultSet.getString("v_tclass_code"),
                resultSet.getString("v_tclass_desc")));
    }

    /**
     * Pencarian COA. Mengikuti {@code CoaDAO.getCoaByCodeAndName()}.
     * Jika keyword diawali '%%' maka cari berdasarkan nama, selain itu
     * berdasarkan nomor akun.
     */
    public List<CoaOptionResponse> searchCoa(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        String like = "%" + normalized.toUpperCase(Locale.ROOT) + "%";
        String sql;
        Object param;
        if (normalized.startsWith("%%")) {
            sql = "select n_coa_id, v_acct_no, v_acct_name from ms_coa "
                    + "where upper(v_acct_name) like ? limit 100";
            param = "%" + normalized.substring(2).toUpperCase(Locale.ROOT) + "%";
        } else {
            sql = "select n_coa_id, v_acct_no, v_acct_name from ms_coa "
                    + "where upper(v_acct_no) like ? limit 100";
            param = like;
        }
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new CoaOptionResponse(
                resultSet.getInt("n_coa_id"),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")), param);
    }

    /**
     * Simpan / update treatment. Mengikuti
     * {@code TreatmentController.doSaveAdd} dan {@code doSaveModify}
     * (saveOrUpdate pada MsTreatmentFee).
     */
    @Transactional
    public void save(TreatmentSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode harus diisi.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nama tindakan harus diisi.");
        }
        if (request.getTreatmentGroupId() == null) {
            throw new IllegalArgumentException("Group tindakan harus dipilih.");
        }
        if (request.getTreatmentClassId() == null) {
            throw new IllegalArgumentException("Kelas tarif harus dipilih.");
        }

        double hospitalFee = valueOrZero(request.getHospitalFee());
        double doctorFee = valueOrZero(request.getDoctorFee());
        double medicFee = valueOrZero(request.getMedicFee());
        double totalFee = valueOrZero(request.getTotalFee());

        Integer treatmentFeeId = request.getTreatmentFeeId();
        if (treatmentFeeId == null) {
            // Insert baru: buat ms_treatment lalu ms_treatment_fee
            Integer treatmentId = request.getTreatmentId();
            if (treatmentId == null) {
                treatmentId = nextTreatmentId();
                jdbcTemplate.update(
                        "insert into ms_treatment (n_treatment_id, v_treatment_code, v_treatment_name, "
                                + "v_who_create, d_whn_create) values (?, ?, ?, ?, now())",
                        treatmentId,
                        code,
                        name,
                        normalizeActor(username));
            } else {
                jdbcTemplate.update(
                        "update ms_treatment set v_treatment_code = ?, v_treatment_name = ?, "
                                + "v_who_change = ?, d_whn_change = now() where n_treatment_id = ?",
                        code,
                        name,
                        normalizeActor(username),
                        treatmentId);
            }

            Integer newFeeId = nextTreatmentFeeId();
            jdbcTemplate.update(
                    "insert into ms_treatment_fee (n_treatment_fee_id, n_treatment_id, n_tgroup_id, "
                            + "n_tclass_id, n_rs_fee, n_doctor_fee, n_medic_fee, n_trtfee_fee, "
                            + "n_coa, v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())",

                    newFeeId,
                    treatmentId,
                    request.getTreatmentGroupId(),
                    request.getTreatmentClassId(),
                    hospitalFee,
                    doctorFee,
                    medicFee,
                    totalFee,
                    request.getCoaId(),
                    normalizeActor(username));
        } else {
            // Update: update ms_treatment_fee (dan ms_treatment bila ada)
            if (request.getTreatmentId() != null) {
                jdbcTemplate.update(
                        "update ms_treatment set v_treatment_code = ?, v_treatment_name = ?, "
                                + "v_who_change = ?, d_whn_change = now() where n_treatment_id = ?",
                        code,
                        name,
                        normalizeActor(username),
                        request.getTreatmentId());
            }
            jdbcTemplate.update(
                    "update ms_treatment_fee set n_tgroup_id = ?, n_tclass_id = ?, "
                            + "n_rs_fee = ?, n_doctor_fee = ?, n_medic_fee = ?, n_trtfee_fee = ?, "
                            + "n_coa = ?, v_who_change = ?, d_whn_change = now() "
                            + "where n_treatment_fee_id = ?",

                    request.getTreatmentGroupId(),
                    request.getTreatmentClassId(),
                    hospitalFee,
                    doctorFee,
                    medicFee,
                    totalFee,
                    request.getCoaId(),
                    normalizeActor(username),
                    treatmentFeeId);
        }
    }

    /**
     * Hapus treatment fee (dan treatment terkait). Mengikuti
     * {@code MsTreatmentFeeDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer treatmentFeeId) {
        Integer treatmentId = jdbcTemplate.query(
                "select n_treatment_id from ms_treatment_fee where n_treatment_fee_id = ?",
                resultSet -> resultSet.next() ? resultSet.getInt("n_treatment_id") : null,
                treatmentFeeId);

        int affected = jdbcTemplate.update(
                "delete from ms_treatment_fee where n_treatment_fee_id = ?", treatmentFeeId);
        if (affected > 0 && treatmentId != null) {
            jdbcTemplate.update("delete from ms_treatment where n_treatment_id = ?", treatmentId);
        }
        return affected > 0;
    }

    private Integer nextTreatmentId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_treatment_n_treatment_id_seq')",
                Integer.class);
    }

    private Integer nextTreatmentFeeId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_treatment_fee_n_treatment_fee_id_seq')",
                Integer.class);
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

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
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
}
