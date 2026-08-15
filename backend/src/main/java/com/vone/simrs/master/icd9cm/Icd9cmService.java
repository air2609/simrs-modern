package com.vone.simrs.master.icd9cm;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0028 (ICD-9-CM MASTER).
 * Mengikuti logika legacy {@code Icd9Controller} + {@code MsIcd9cmDAO}
 * pada tabel ms_icd_9cm.
 */
@Service
public class Icd9cmService {

    private final JdbcTemplate jdbcTemplate;

    public Icd9cmService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar semua ICD-9-CM. Mengikuti {@code MsIcd9cmDAO.getIcd9s()}.
     */
    public List<Icd9cmRowResponse> getIcd9cms() {
        String sql = "select n_icd9cm_id, v_icd9cm_code, v_icd9cm_name "
                + "from ms_icd_9cm order by v_icd9cm_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new Icd9cmRowResponse(
                resultSet.getInt("n_icd9cm_id"),
                resultSet.getString("v_icd9cm_code"),
                resultSet.getString("v_icd9cm_name")));
    }

    /**
     * Pencarian ICD-9-CM berdasarkan kode dan/atau nama tindakan.
     * Mengikuti {@code MsIcd9cmDAO.findByExample} / {@code searchIcd9}
     * yang memakai LIKE pada kedua field.
     */
    public List<Icd9cmRowResponse> searchIcd9cms(String code, String name) {
        String likeCode = "%" + normalize(code) + "%";
        String likeName = "%" + normalize(name) + "%";
        String sql = "select n_icd9cm_id, v_icd9cm_code, v_icd9cm_name from ms_icd_9cm "
                + "where upper(v_icd9cm_code) like ? and upper(v_icd9cm_name) like ? "
                + "order by v_icd9cm_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new Icd9cmRowResponse(
                resultSet.getInt("n_icd9cm_id"),
                resultSet.getString("v_icd9cm_code"),
                resultSet.getString("v_icd9cm_name")), likeCode, likeName);
    }

    /**
     * Cek apakah kode sudah dipakai. Mengikuti kolom unik v_icd9cm_code.
     */
    public boolean isCodeExists(String code) {
        if (code == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_icd_9cm where v_icd9cm_code = ?",
                Integer.class,
                code.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update ICD-9-CM. Mengikuti {@code Icd9Controller.doSaveAdd}
     * dan {@code doSaveModify} (saveOrUpdate).
     */
    @Transactional
    public void save(Icd9cmSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode ICD-9-CM harus diisi.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nama tindakan harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            // Cek duplikasi kode (kolom unik v_icd9cm_code)
            if (isCodeExists(code)) {
                throw new IllegalArgumentException("Kode ICD-9-CM sudah terdaftar.");
            }
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_icd_9cm (n_icd9cm_id, v_icd9cm_code, v_icd9cm_name, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, now())",
                    id,
                    code,
                    name,
                    normalizeActor(username));
        } else {
            // Cek duplikasi kode selain id ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_icd_9cm where v_icd9cm_code = ? and n_icd9cm_id <> ?",
                    Integer.class,
                    code,
                    id);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Kode ICD-9-CM sudah terdaftar.");
            }
            jdbcTemplate.update(
                    "update ms_icd_9cm set v_icd9cm_code = ?, v_icd9cm_name = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_icd9cm_id = ?",
                    code,
                    name,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus ICD-9-CM. Mengikuti {@code MsIcd9cmDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_icd_9cm where n_icd9cm_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_icd_9cm_n_icd9cm_id_seq')",
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
