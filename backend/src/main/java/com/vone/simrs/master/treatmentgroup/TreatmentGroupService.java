package com.vone.simrs.master.treatmentgroup;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0023 (TREATMENT GROUP MASTER).
 * Mengikuti logika legacy {@code TreatmentGroupManagerImpl} +
 * {@code MsTreatmentGroupDAO}.
 */
@Service
public class TreatmentGroupService {

    private final JdbcTemplate jdbcTemplate;

    public TreatmentGroupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar treatment group. Mengikuti
     * {@code MsTreatmentGroupDAO.getAllTreatmentGroup()}
     * yang mengurutkan berdasarkan kode.
     */
    public List<TreatmentGroupRowResponse> getTreatmentGroups() {
        String sql = "select n_tgroup_id, v_tgroup_code, v_tgroup_name "
                + "from ms_treatment_group order by v_tgroup_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new TreatmentGroupRowResponse(
                resultSet.getInt("n_tgroup_id"),
                resultSet.getString("v_tgroup_code"),
                resultSet.getString("v_tgroup_name")));
    }

    /**
     * Cek apakah kode sudah dipakai. Mengikuti kolom unik v_tgroup_code.
     */
    public boolean isCodeExists(String code) {
        if (code == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_treatment_group where v_tgroup_code = ?",
                Integer.class,
                code.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update treatment group. Mengikuti
     * {@code TreatmentGroupController.doSaveAdd} dan {@code doSaveModify}
     * (saveOrUpdate).
     */
    @Transactional
    public void save(TreatmentGroupSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode harus diisi.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nama harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            // Cek duplikasi kode (kolom unik v_tgroup_code)
            if (isCodeExists(code)) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_treatment_group (n_tgroup_id, v_tgroup_code, v_tgroup_name, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, now())",
                    id,
                    code,
                    name,
                    normalizeActor(username));
        } else {
            // Cek duplikasi kode selain id ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_treatment_group where v_tgroup_code = ? and n_tgroup_id <> ?",
                    Integer.class,
                    code,
                    id);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            jdbcTemplate.update(
                    "update ms_treatment_group set v_tgroup_code = ?, v_tgroup_name = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_tgroup_id = ?",
                    code,
                    name,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus treatment group. Mengikuti {@code MsTreatmentGroupDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_treatment_group where n_tgroup_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_treatment_group_n_tgroup_id_seq')",
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
