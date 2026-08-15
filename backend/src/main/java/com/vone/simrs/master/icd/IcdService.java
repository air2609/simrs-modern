package com.vone.simrs.master.icd;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0027 (ICD MASTER).
 * Mengikuti logika legacy {@code IcdController} + {@code MsIcdDAO}
 * pada tabel ms_icd.
 */
@Service
public class IcdService {

    private final JdbcTemplate jdbcTemplate;

    public IcdService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar semua ICD. Mengikuti {@code MsIcdDAO.getIcds()}.
     */
    public List<IcdRowResponse> getIcds() {
        String sql = "select n_icd_id, v_icd_code, v_icd_name "
                + "from ms_icd order by v_icd_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new IcdRowResponse(
                resultSet.getInt("n_icd_id"),
                resultSet.getString("v_icd_code"),
                resultSet.getString("v_icd_name")));
    }

    /**
     * Pencarian ICD berdasarkan kode dan/atau nama penyakit.
     * Mengikuti {@code MsIcdDAO.findByExample} / {@code searchIcd}
     * yang memakai LIKE pada kedua field.
     */
    public List<IcdRowResponse> searchIcds(String code, String name) {
        String likeCode = "%" + normalize(code) + "%";
        String likeName = "%" + normalize(name) + "%";
        String sql = "select n_icd_id, v_icd_code, v_icd_name from ms_icd "
                + "where upper(v_icd_code) like ? and upper(v_icd_name) like ? "
                + "order by v_icd_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new IcdRowResponse(
                resultSet.getInt("n_icd_id"),
                resultSet.getString("v_icd_code"),
                resultSet.getString("v_icd_name")), likeCode, likeName);
    }

    /**
     * Cek apakah kode sudah dipakai. Mengikuti kolom unik v_icd_code.
     */
    public boolean isCodeExists(String code) {
        if (code == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_icd where v_icd_code = ?",
                Integer.class,
                code.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update ICD. Mengikuti {@code IcdController.doSaveAdd}
     * dan {@code doSaveModify} (saveOrUpdate).
     */
    @Transactional
    public void save(IcdSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode ICD harus diisi.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nama penyakit harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            // Cek duplikasi kode (kolom unik v_icd_code)
            if (isCodeExists(code)) {
                throw new IllegalArgumentException("Kode ICD sudah terdaftar.");
            }
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_icd (n_icd_id, v_icd_code, v_icd_name, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, now())",
                    id,
                    code,
                    name,
                    normalizeActor(username));
        } else {
            // Cek duplikasi kode selain id ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_icd where v_icd_code = ? and n_icd_id <> ?",
                    Integer.class,
                    code,
                    id);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Kode ICD sudah terdaftar.");
            }
            jdbcTemplate.update(
                    "update ms_icd set v_icd_code = ?, v_icd_name = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_icd_id = ?",
                    code,
                    name,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus ICD. Mengikuti {@code MsIcdDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_icd where n_icd_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_icd_n_icd_id_seq')",
                Integer.class);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
