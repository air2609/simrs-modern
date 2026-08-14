package com.vone.simrs.master.warehouse;

import com.vone.simrs.master.treatment.CoaOptionResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0035 (WAREHOUSE MASTER).
 * Mengikuti logika legacy {@code WarehouseManagerImpl} +
 * {@code MsWarehouseDAO}.
 */
@Service
public class WarehouseService {

    private final JdbcTemplate jdbcTemplate;

    public WarehouseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar gudang. Mengikuti {@code MsWarehouseDAO.findAll()}
     * yang mengembalikan seluruh data gudang diurutkan berdasarkan kode.
     */
    public List<WarehouseRowResponse> getWarehouses() {
        String sql = "select w.n_whouse_id, w.v_whouse_code, w.v_whouse_name, w.v_whouse_loc, "
                + "w.n_superior_whouse_id, sup.v_whouse_name as superior_name, "
                + "w.n_coa_id, coa.v_acct_no, coa.v_acct_name "
                + "from ms_warehouse w "
                + "left join ms_warehouse sup on sup.n_whouse_id = w.n_superior_whouse_id "
                + "left join ms_coa coa on coa.n_coa_id = w.n_coa_id "
                + "order by w.v_whouse_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new WarehouseRowResponse(
                resultSet.getInt("n_whouse_id"),
                resultSet.getString("v_whouse_code"),
                resultSet.getString("v_whouse_name"),
                resultSet.getString("v_whouse_loc"),
                toInteger(resultSet.getObject("n_superior_whouse_id")),
                resultSet.getString("superior_name"),
                toInteger(resultSet.getObject("n_coa_id")),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")));
    }

    /**
     * Daftar gudang untuk dropdown "GUDANG UTAMA" (superior).
     * Mengikuti {@code WarehouseController.getWarehouseList()}.
     */
    public List<WarehouseOptionResponse> getWarehouseOptions() {
        String sql = "select n_whouse_id, v_whouse_code, v_whouse_name "
                + "from ms_warehouse order by v_whouse_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new WarehouseOptionResponse(
                resultSet.getInt("n_whouse_id"),
                resultSet.getString("v_whouse_code"),
                resultSet.getString("v_whouse_name")));
    }

    /**
     * Pencarian COA. Mengikuti {@code CoaDAO.getCoaByCodeAndName()}
     * pada tabel ms_coa.
     */
    public List<CoaOptionResponse> searchCoa(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        String like = "%" + normalized.toUpperCase(Locale.ROOT) + "%";
        String sql;
        Object param;
        if (normalized.startsWith("%%")) {
            sql = "select n_coa_id, v_acct_no, v_acct_name from ms_coa "
                    + "where upper(v_acct_name) like ? limit 100";
            param = like;
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
     * Simpan / update gudang. Mengikuti {@code WarehouseController.doSaveAdd}
     * dan {@code doSaveModify}.
     */
    @Transactional
    public void save(WarehouseSaveRequest request, String username) {
        String whouseCode = normalize(request.getWhouseCode());
        String whouseName = normalize(request.getWhouseName());

        if (whouseCode == null || whouseCode.isEmpty()) {
            throw new IllegalArgumentException("Kode gudang harus diisi.");
        }
        if (whouseName == null || whouseName.isEmpty()) {
            throw new IllegalArgumentException("Nama gudang harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_warehouse (n_whouse_id, v_whouse_code, v_whouse_name, "
                            + "v_whouse_loc, n_superior_whouse_id, n_coa_id, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, now())",
                    id,
                    whouseCode,
                    whouseName,
                    normalize(request.getWhouseLoc()),
                    request.getSuperiorId(),
                    request.getCoaId(),
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_warehouse set v_whouse_code = ?, v_whouse_name = ?, "
                            + "v_whouse_loc = ?, n_superior_whouse_id = ?, n_coa_id = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_whouse_id = ?",
                    whouseCode,
                    whouseName,
                    normalize(request.getWhouseLoc()),
                    request.getSuperiorId(),
                    request.getCoaId(),
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus gudang. Mengikuti {@code MsWarehouseDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_warehouse where n_whouse_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_warehouse_n_whouse_id_seq')",
                Integer.class);
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
