package com.vone.simrs.master.unit;

import com.vone.simrs.master.treatment.CoaOptionResponse;
import com.vone.simrs.master.warehouse.WarehouseOptionResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0024 (UNIT MASTER).
 * Mengikuti logika legacy {@code UnitController} + {@code MsUnitDAO}
 * pada tabel ms_unit.
 */
@Service
public class UnitService {

    private final JdbcTemplate jdbcTemplate;

    public UnitService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar unit. Mengikuti {@code MsUnitDAO.getAllUnit()}.
     */
    public List<UnitRowResponse> getUnits() {
        String sql = "select u.n_unit_id, u.v_unit_code, u.v_unit_name, "
                + "u.n_division_id, d.v_division_name, u.unit_type, "
                + "u.n_whouse_id, w.v_whouse_name, "
                + "u.n_coa_id, coa.v_acct_no "
                + "from ms_unit u "
                + "left join ms_division d on d.n_division_id = u.n_division_id "
                + "left join ms_warehouse w on w.n_whouse_id = u.n_whouse_id "
                + "left join ms_coa coa on coa.n_coa_id = u.n_coa_id "
                + "order by u.v_unit_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new UnitRowResponse(
                resultSet.getInt("n_unit_id"),
                resultSet.getString("v_unit_code"),
                resultSet.getString("v_unit_name"),
                toInteger(resultSet.getObject("n_division_id")),
                resultSet.getString("v_division_name"),
                toInteger(resultSet.getObject("unit_type")),
                toInteger(resultSet.getObject("n_whouse_id")),
                resultSet.getString("v_whouse_name"),
                toInteger(resultSet.getObject("n_coa_id")),
                resultSet.getString("v_acct_no")));
    }

    /**
     * Opsi dropdown divisi. Mengikuti {@code DivisionController.getDivisionList()}.
     */
    public List<DivisionOptionResponse> getDivisionOptions() {
        String sql = "select n_division_id, v_division_code, v_division_name "
                + "from ms_division order by v_division_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new DivisionOptionResponse(
                resultSet.getInt("n_division_id"),
                resultSet.getString("v_division_code"),
                resultSet.getString("v_division_name")));
    }

    /**
     * Opsi dropdown gudang unit. Mengikuti
     * {@code WarehouseController.getWarehouseList()}.
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
     * Pencarian COA untuk bandbox NO. COA. Mencari berdasarkan account code
     * (v_acct_no) ATAU account name (v_acct_name), keduanya memakai LIKE.
     */
    public List<CoaOptionResponse> searchCoa(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        String like = "%" + normalized.toUpperCase(Locale.ROOT) + "%";
        String sql = "select n_coa_id, v_acct_no, v_acct_name from ms_coa "
                + "where upper(v_acct_no) like ? or upper(v_acct_name) like ? "
                + "order by v_acct_no limit 100";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new CoaOptionResponse(
                resultSet.getInt("n_coa_id"),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")), like, like);
    }

    /**
     * Cek apakah kode sudah dipakai. Mengikuti kolom unik v_unit_code.
     */
    public boolean isCodeExists(String code) {
        if (code == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_unit where v_unit_code = ?",
                Integer.class,
                code.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update unit. Mengikuti {@code UnitController.doSaveAdd}
     * dan {@code doSaveModify} (saveOrUpdate).
     */
    @Transactional
    public void save(UnitSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode harus diisi.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nama unit harus diisi.");
        }
        if (request.getDivisionId() == null) {
            throw new IllegalArgumentException("Divisi harus dipilih.");
        }

        Integer unitType = request.getUnitType() == null ? 1 : request.getUnitType();

        Integer id = request.getId();
        if (id == null) {
            // Cek duplikasi kode (kolom unik v_unit_code)
            if (isCodeExists(code)) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_unit (n_unit_id, v_unit_code, v_unit_name, "
                            + "n_division_id, unit_type, n_whouse_id, n_coa_id, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, now())",
                    id,
                    code,
                    name,
                    request.getDivisionId(),
                    unitType,
                    request.getWarehouseId(),
                    request.getCoaId(),
                    normalizeActor(username));
        } else {
            // Cek duplikasi kode selain id ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_unit where v_unit_code = ? and n_unit_id <> ?",
                    Integer.class,
                    code,
                    id);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            jdbcTemplate.update(
                    "update ms_unit set v_unit_code = ?, v_unit_name = ?, "
                            + "n_division_id = ?, unit_type = ?, n_whouse_id = ?, n_coa_id = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_unit_id = ?",
                    code,
                    name,
                    request.getDivisionId(),
                    unitType,
                    request.getWarehouseId(),
                    request.getCoaId(),
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus unit. Mengikuti {@code MsUnitDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_unit where n_unit_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_unit_n_unit_id_seq')",
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
