package com.vone.simrs.master.treatmentclass;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0021 (KELAS TARIF / TREATMENT CLASS MASTER).
 * Mengikuti logika legacy {@code TreatmentClassManagerImpl} +
 * {@code MsTreatmentClassDAO}.
 */
@Service
public class TreatmentClassService {

    private final JdbcTemplate jdbcTemplate;

    public TreatmentClassService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar kelas tarif. Mengikuti
     * {@code MsTreatmentClassDAO.getAllTreatmentClass()}
     * yang mengurutkan berdasarkan kode.
     */
    public List<TreatmentClassRowResponse> getTreatmentClasses() {
        String sql = "select n_tclass_id, v_tclass_code, v_tclass_desc "
                + "from ms_treatment_class order by v_tclass_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new TreatmentClassRowResponse(
                resultSet.getInt("n_tclass_id"),
                resultSet.getString("v_tclass_code"),
                resultSet.getString("v_tclass_desc")));
    }

    /**
     * Cek apakah kode sudah dipakai. Mengikuti
     * {@code MsTreatmentClassDAO.getByCode}.
     */
    public boolean isCodeExists(String code) {
        if (code == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_treatment_class where v_tclass_code = ?",
                Integer.class,
                code.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update kelas tarif. Mengikuti
     * {@code TreatmentClassController.doSaveAdd}
     * dan {@code doSaveModify} (saveOrUpdate).
     */
    @Transactional
    public void save(TreatmentClassSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String description = normalize(request.getDescription());

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode harus diisi.");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Nama harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            // Cek duplikasi kode (legacy: getTClassByCode != null -> code exist)
            if (isCodeExists(code)) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_treatment_class (n_tclass_id, v_tclass_code, v_tclass_desc, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, now())",
                    id,
                    code,
                    description,
                    normalizeActor(username));
        } else {
            // Cek duplikasi kode selain id ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_treatment_class where v_tclass_code = ? and n_tclass_id <> ?",
                    Integer.class,
                    code,
                    id);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            jdbcTemplate.update(
                    "update ms_treatment_class set v_tclass_code = ?, v_tclass_desc = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_tclass_id = ?",
                    code,
                    description,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus kelas tarif. Mengikuti {@code MsTreatmentClassDAO.deleteById}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_treatment_class where n_tclass_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_treatment_class_n_tclass_id_seq')",
                Integer.class);
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
