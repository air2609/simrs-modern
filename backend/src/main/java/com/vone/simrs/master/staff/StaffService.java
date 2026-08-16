package com.vone.simrs.master.staff;

import com.vone.simrs.master.doctor.CoaOptionResponse;
import com.vone.simrs.master.doctor.UnitOptionResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0031 (MASTER STAFF).
 * Mengikuti logika legacy {@code StaffController} + {@code MsStaffDAO}
 * pada tabel ms_staff dan ms_staff_in_unit. Berbeda dengan MASTER DOKTER
 * (SCM0030) yang memakai tabel ms_doctor, screen ini murni memakai
 * ms_staff dengan n_staff_role = STAFF (1) dan mendukung banyak unit
 * (sub divisi) per staff.
 */
@Service
public class StaffService {

    private static final short ROLE_STAFF = 1;

    private final JdbcTemplate jdbcTemplate;

    public StaffService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar staff. Mengikuti {@code MsStaffDAO.getStaffByRole} yang
     * mengambil ms_staff dengan n_staff_role = STAFF (1). Kolom unitIds dan
     * unitNames berisi daftar unit (sub divisi) dari ms_staff_in_unit.
     */
    public List<StaffRowResponse> getStaffs(String code, String name) {
        String likeCode = "%" + normalizeLike(code) + "%";
        String likeName = "%" + normalizeLike(name) + "%";

        String sql = "select staff.n_staff_id, staff.v_staff_code, staff.v_staff_name, "
                + "staff.v_staff_addr, staff.v_staff_ph_no, staff.n_coa, coa.v_acct_no, "
                + "staff.n_staff_salary, staff.d_staff_hired_date, staff.d_staff_fired_date "
                + "from ms_staff staff "
                + "left join ms_coa coa on coa.n_coa_id = staff.n_coa "
                + "where staff.n_staff_role = ? "
                + "and upper(staff.v_staff_code) like ? "
                + "and upper(staff.v_staff_name) like ? "
                + "order by staff.v_staff_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            Integer staffId = resultSet.getInt("n_staff_id");
            return new StaffRowResponse(
                    staffId,
                    resultSet.getString("v_staff_code"),
                    resultSet.getString("v_staff_name"),
                    resultSet.getString("v_staff_addr"),
                    resultSet.getString("v_staff_ph_no"),
                    toInteger(resultSet.getObject("n_coa")),
                    resultSet.getString("v_acct_no"),
                    toDouble(resultSet.getObject("n_staff_salary")),
                    toDateString(resultSet.getObject("d_staff_hired_date")),
                    toDateString(resultSet.getObject("d_staff_fired_date")),
                    getUnitIds(staffId),
                    getUnitNames(staffId));
        }, ROLE_STAFF, likeCode, likeName);
    }

    /**
     * Daftar id unit (sub divisi) untuk seorang staff dari ms_staff_in_unit.
     */
    private List<Integer> getUnitIds(Integer staffId) {
        String sql = "select n_unit_id from ms_staff_in_unit "
                + "where n_staff_id = ? order by n_unit_id";
        return jdbcTemplate.query(sql,
                (resultSet, rowNum) -> resultSet.getInt("n_unit_id"), staffId);
    }

    /**
     * Daftar nama unit (sub divisi) untuk seorang staff dari ms_staff_in_unit
     * yang digabung dengan ms_unit.
     */
    private List<String> getUnitNames(Integer staffId) {
        String sql = "select u.v_unit_name from ms_staff_in_unit siu "
                + "join ms_unit u on u.n_unit_id = siu.n_unit_id "
                + "where siu.n_staff_id = ? order by u.v_unit_name";
        return jdbcTemplate.query(sql,
                (resultSet, rowNum) -> resultSet.getString("v_unit_name"), staffId);
    }

    /**
     * Data master untuk form staff: opsi unit (sub divisi) dan opsi COA.
     */
    public StaffMastersResponse getMasters() {
        return new StaffMastersResponse(getUnitOptions(), getCoaOptions());
    }

    /**
     * Opsi dropdown unit (sub divisi). Mengikuti {@code UnitController}
     * pada tabel ms_unit.
     */
    public List<UnitOptionResponse> getUnitOptions() {
        String sql = "select n_unit_id, v_unit_code, v_unit_name "
                + "from ms_unit order by v_unit_name";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new UnitOptionResponse(
                resultSet.getInt("n_unit_id"),
                resultSet.getString("v_unit_code"),
                resultSet.getString("v_unit_name")));
    }

    /**
     * Opsi dropdown COA. Mengikuti {@code CoaController.getCoaForSelect}
     * pada tabel ms_coa.
     */
    public List<CoaOptionResponse> getCoaOptions() {
        String sql = "select n_coa_id, v_acct_no, v_acct_name from ms_coa "
                + "order by v_acct_no limit 100";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new CoaOptionResponse(
                resultSet.getInt("n_coa_id"),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")));
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
     * Cek apakah kode staff sudah dipakai. Mengikuti kolom unik v_staff_code.
     */
    public boolean isCodeExists(String code) {
        if (code == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_staff where v_staff_code = ?",
                Integer.class,
                code.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update staff. Mengikuti {@code MsStaffDAO.save} yang
     * menyimpan ms_staff dan relasi ms_staff_in_unit untuk setiap unit
     * (sub divisi) yang dipilih.
     */
    @Transactional
    public void save(StaffSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode staff harus diisi.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nama staff harus diisi.");
        }
        if (request.getUnitIds() == null || request.getUnitIds().isEmpty()) {
            throw new IllegalArgumentException("Sub divisi harus dipilih.");
        }

        Integer staffId = request.getStaffId();
        String actor = normalizeActor(username);

        if (staffId == null) {
            // Cek duplikasi kode (kolom unik v_staff_code)
            if (isCodeExists(code)) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            staffId = nextStaffId();
            jdbcTemplate.update(
                    "insert into ms_staff (n_staff_id, v_staff_code, v_staff_name, "
                            + "v_staff_addr, v_staff_ph_no, n_staff_salary, "
                            + "d_staff_hired_date, d_staff_fired_date, n_coa, "
                            + "n_staff_role, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                    staffId,
                    code,
                    name,
                    request.getAddress(),
                    request.getPhone(),
                    request.getSalary(),
                    parseDate(request.getHiredDate()),
                    parseDate(request.getFiredDate()),
                    request.getCoaId(),
                    ROLE_STAFF,
                    actor);

            insertUnits(staffId, request.getUnitIds(), actor);
        } else {
            // Cek duplikasi kode selain staff ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_staff where v_staff_code = ? and n_staff_id <> ?",
                    Integer.class,
                    code,
                    staffId);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            jdbcTemplate.update(
                    "update ms_staff set v_staff_code = ?, v_staff_name = ?, "
                            + "v_staff_addr = ?, v_staff_ph_no = ?, n_staff_salary = ?, "
                            + "d_staff_hired_date = ?, d_staff_fired_date = ?, n_coa = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_staff_id = ?",
                    code,
                    name,
                    request.getAddress(),
                    request.getPhone(),
                    request.getSalary(),
                    parseDate(request.getHiredDate()),
                    parseDate(request.getFiredDate()),
                    request.getCoaId(),
                    actor,
                    staffId);

            // Update relasi unit staff (hapus lama, insert baru)
            jdbcTemplate.update("delete from ms_staff_in_unit where n_staff_id = ?", staffId);
            insertUnits(staffId, request.getUnitIds(), actor);
        }
    }

    /**
     * Insert relasi ms_staff_in_unit untuk setiap unit (sub divisi) yang
     * dipilih. Mengikuti {@code MsStaffDAO.save} yang menyimpan satu baris
     * ms_staff_in_unit per unit.
     */
    private void insertUnits(Integer staffId, List<Integer> unitIds, String actor) {
        for (Integer unitId : unitIds) {
            if (unitId == null) {
                continue;
            }
            jdbcTemplate.update(
                    "insert into ms_staff_in_unit (n_staff_id, n_unit_id, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, now())",
                    staffId,
                    unitId,
                    actor);
        }
    }

    /**
     * Hapus staff. Mengikuti {@code MsStaffDAO.delete} yang menghapus dari
     * ms_staff (relasi ms_staff_in_unit ikut terhapus melalui foreign key).
     */
    @Transactional
    public boolean delete(Integer staffId) {
        int affected = jdbcTemplate.update("delete from ms_staff where n_staff_id = ?", staffId);
        return affected > 0;
    }

    private Integer nextStaffId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_staff_n_staff_id_seq')",
                Integer.class);
    }

    private java.sql.Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.valueOf(value.toString());
    }

    private String toDateString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toString();
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toString();
        }
        return value.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLike(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
