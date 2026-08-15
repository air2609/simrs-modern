package com.vone.simrs.master.gim;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0047 (GENERAL INFORMATION MASTER).
 * Mengikuti logika legacy {@code GimManagerImpl} + {@code MsGimDAO}.
 */
@Service
public class GimService {

    private final JdbcTemplate jdbcTemplate;

    public GimService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar general information. Mengikuti
     * {@code MsGimDAO.findByExample(new MsGim())}
     * yang mengembalikan seluruh data ms_gim.
     */
    public List<GimRowResponse> getGims() {
        String sql = "select n_gim_id, v_key, v_value from ms_gim order by v_key";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new GimRowResponse(
                resultSet.getInt("n_gim_id"),
                resultSet.getString("v_key"),
                resultSet.getString("v_value")));
    }

    /**
     * Simpan / update general information. Mengikuti
     * {@code GimController.doSaveAdd}
     * dan {@code doSaveModify}.
     */
    @Transactional
    public void save(GimSaveRequest request, String username) {
        String key = normalize(request.getKey());
        String value = normalize(request.getValue());

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("KEY harus diisi.");
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("VALUE harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_gim (n_gim_id, v_key, v_value, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, now())",
                    id,
                    key,
                    value,
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_gim set v_key = ?, v_value = ?, v_who_change = ?, d_whn_change = now() "
                            + "where n_gim_id = ?",
                    key,
                    value,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus general information. Mengikuti {@code MsGimDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_gim where n_gim_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_gim_n_gim_id_seq')",
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
