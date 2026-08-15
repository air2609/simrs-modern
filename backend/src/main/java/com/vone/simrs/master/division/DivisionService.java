package com.vone.simrs.master.division;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0022 (DIVISION MASTER).
 * Mengikuti logika legacy {@code DivisionController} +
 * {@code MsDivisionDAO}.
 */
@Service
public class DivisionService {

    private final JdbcTemplate jdbcTemplate;

    public DivisionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar divisi. Mengikuti {@code MsDivisionDAO.getAllDivision()}.
     */
    public List<DivisionRowResponse> getDivisions() {
        String sql = "select n_division_id, v_division_code, v_division_name, "
                + "v_registration_unit, n_registration_charge "
                + "from ms_division order by v_division_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new DivisionRowResponse(
                resultSet.getInt("n_division_id"),
                resultSet.getString("v_division_code"),
                resultSet.getString("v_division_name"),
                resultSet.getString("v_registration_unit"),
                resultSet.getObject("n_registration_charge") == null
                        ? null
                        : resultSet.getInt("n_registration_charge")));
    }

    /**
     * Cek apakah kode sudah dipakai. Mengikuti kolom unik v_division_code.
     */
    public boolean isCodeExists(String code) {
        if (code == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_division where v_division_code = ?",
                Integer.class,
                code.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update divisi. Mengikuti {@code DivisionController.doSaveAdd}
     * dan {@code doSaveModify} (saveOrUpdate).
     */
    @Transactional
    public void save(DivisionSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        String registrationUnit = normalizeRegistrationUnit(request.getRegistrationUnit());
        Integer registrationCharge = request.getRegistrationCharge() == null
                ? 0
                : request.getRegistrationCharge();

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode harus diisi.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nama harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            // Cek duplikasi kode (kolom unik v_division_code)
            if (isCodeExists(code)) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_division (n_division_id, v_division_code, v_division_name, "
                            + "v_registration_unit, n_registration_charge, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, now())",
                    id,
                    code,
                    name,
                    registrationUnit,
                    registrationCharge,
                    normalizeActor(username));
        } else {
            // Cek duplikasi kode selain id ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_division where v_division_code = ? and n_division_id <> ?",
                    Integer.class,
                    code,
                    id);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            jdbcTemplate.update(
                    "update ms_division set v_division_code = ?, v_division_name = ?, "
                            + "v_registration_unit = ?, n_registration_charge = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_division_id = ?",
                    code,
                    name,
                    registrationUnit,
                    registrationCharge,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus divisi. Mengikuti {@code MsDivisionDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_division where n_division_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_division_n_division_id_seq')",
                Integer.class);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRegistrationUnit(String value) {
        if (value == null) {
            return "NO";
        }
        String trimmed = value.trim().toUpperCase(Locale.ROOT);
        return "YES".equals(trimmed) ? "YES" : "NO";
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
