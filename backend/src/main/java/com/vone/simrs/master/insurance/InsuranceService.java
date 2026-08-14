package com.vone.simrs.master.insurance;

import com.vone.simrs.master.treatment.CoaOptionResponse;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0034 (INSURANCE MASTER).
 * Mengikuti logika legacy {@code InsuranceManagerImpl} +
 * {@code MsInsuranceDAO}.
 */
@Service
public class InsuranceService {

    private static final short INSURANCE_ACTIVE = 1;
    private static final short INSURANCE_INACTIVE = 0;

    private final JdbcTemplate jdbcTemplate;

    public InsuranceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar asuransi. Mengikuti {@code MsInsuranceDAO.findAll()}
     * yang mengembalikan seluruh data asuransi diurutkan berdasarkan status aktif.
     */
    public List<InsuranceRowResponse> getInsurances() {
        String sql = "select i.n_insurance_id, i.v_insurance_name, i.v_insurance_addr, "
                + "i.v_insurance_ph_no, i.v_insurance_desc, i.n_active_status, "
                + "i.d_date_end_of_partnership, i.n_coa_id, "
                + "coa.v_acct_no, coa.v_acct_name "
                + "from ms_insurance i "
                + "left join ms_coa coa on coa.n_coa_id = i.n_coa_id "
                + "order by i.n_active_status desc, i.v_insurance_name";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new InsuranceRowResponse(
                resultSet.getInt("n_insurance_id"),
                resultSet.getString("v_insurance_name"),
                resultSet.getString("v_insurance_addr"),
                resultSet.getString("v_insurance_ph_no"),
                resultSet.getString("v_insurance_desc"),
                resultSet.getShort("n_active_status") == INSURANCE_ACTIVE,
                toLocalDate(resultSet.getDate("d_date_end_of_partnership")),
                toInteger(resultSet.getObject("n_coa_id")),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")));
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
     * Simpan / update asuransi. Mengikuti {@code InsuranceController.doSaveAdd}
     * dan {@code doSaveModify}.
     */
    @Transactional
    public void save(InsuranceSaveRequest request, String username) {
        String insuranceName = normalize(request.getInsuranceName());

        if (insuranceName == null || insuranceName.isEmpty()) {
            throw new IllegalArgumentException("Nama asuransi harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_insurance (n_insurance_id, v_insurance_name, v_insurance_addr, "
                            + "v_insurance_ph_no, v_insurance_desc, n_coa_id, "
                            + "d_date_end_of_partnership, n_active_status, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                    id,
                    insuranceName,
                    normalize(request.getInsuranceAddr()),
                    normalize(request.getInsurancePhNo()),
                    normalize(request.getInsuranceDesc()),
                    request.getCoaId(),
                    toSqlDate(request.getEndOfContract()),
                    request.isActive() ? INSURANCE_ACTIVE : INSURANCE_INACTIVE,
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_insurance set v_insurance_name = ?, v_insurance_addr = ?, "
                            + "v_insurance_ph_no = ?, v_insurance_desc = ?, n_coa_id = ?, "
                            + "d_date_end_of_partnership = ?, n_active_status = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_insurance_id = ?",
                    insuranceName,
                    normalize(request.getInsuranceAddr()),
                    normalize(request.getInsurancePhNo()),
                    normalize(request.getInsuranceDesc()),
                    request.getCoaId(),
                    toSqlDate(request.getEndOfContract()),
                    request.isActive() ? INSURANCE_ACTIVE : INSURANCE_INACTIVE,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus asuransi. Mengikuti {@code MsInsuranceDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_insurance where n_insurance_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_insurance_n_insurance_id_seq')",
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

    private LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
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
