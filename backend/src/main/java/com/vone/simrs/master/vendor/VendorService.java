package com.vone.simrs.master.vendor;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0043 (VENDOR MASTER / FORM MASTER SUPPLIER).
 * Mengikuti logika legacy {@code VendorManagerImpl} + {@code VendorDAO}.
 */
@Service
public class VendorService {

    private final JdbcTemplate jdbcTemplate;

    public VendorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar vendor. Mengikuti {@code VendorDAO.getAllVendor()} dan
     * {@code VendorDAO.searchVendorByCriteria()} yang memfilter berdasarkan
     * kode ATAU nama (LIKE %crit%).
     */
    public List<VendorRowResponse> getVendors(String search) {
        String sql = "select v.n_vendor_id, v.v_vendor_code, v.v_vendor_name, "
                + "v.v_vendor_address, v.v_vendor_contact_person, v.v_vendor_contact_no, "
                + "v.v_vendor_alt_contact_no, v.v_vendor_fax_no, "
                + "v.n_coa_id, c.v_acct_no, c.v_acct_name "
                + "from ms_vendor v "
                + "left join ms_coa c on c.n_coa_id = v.n_coa_id ";

        if (search != null && !search.trim().isEmpty()) {
            String value = "%" + search.trim() + "%";
            sql += "where v.v_vendor_code like ? or v.v_vendor_name like ? ";
            return jdbcTemplate.query(sql + "order by v.v_vendor_code",
                    (resultSet, rowNum) -> mapRow(resultSet), value, value);
        }

        return jdbcTemplate.query(sql + "order by v.v_vendor_code",
                (resultSet, rowNum) -> mapRow(resultSet));
    }

    /**
     * Data master untuk form: opsi COA untuk bandbox NO. COA.
     */
    public VendorMastersResponse getMasters() {
        List<CoaOptionResponse> coaOptions = jdbcTemplate.query(
                "select n_coa_id, v_acct_no, v_acct_name from ms_coa order by v_acct_no",
                (resultSet, rowNum) -> new CoaOptionResponse(
                        resultSet.getInt("n_coa_id"),
                        resultSet.getString("v_acct_no"),
                        resultSet.getString("v_acct_name")));
        return new VendorMastersResponse(coaOptions);
    }

    /**
     * Pencarian COA. Mengikuti {@code CoaDAO.getCoaByCodeAndName()}.
     * Kata kunci dicocokkan pada nomor akun ATAU nama akun sekaligus.
     */
    public List<CoaOptionResponse> searchCoa(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
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
     * Simpan / update vendor. Mengikuti {@code VendorDAO.save()}
     * 
     * (saveOrUpdate).
     */
    @Transactional
    public void save(VendorSaveRequest request, String username) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Kode vendor harus diisi.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nama vendor harus diisi.");
        }
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Alamat vendor harus diisi.");
        }
        if (request.getContactPerson() == null || request.getContactPerson().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact person harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_vendor (n_vendor_id, v_vendor_code, v_vendor_name, "
                            + "v_vendor_address, v_vendor_contact_person, v_vendor_contact_no, "
                            + "v_vendor_alt_contact_no, v_vendor_fax_no, n_coa_id, "
                            + "v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                    id,
                    request.getCode().trim().toUpperCase(Locale.ROOT),
                    request.getName().trim().toUpperCase(Locale.ROOT),
                    request.getAddress().trim().toUpperCase(Locale.ROOT),
                    request.getContactPerson().trim().toUpperCase(Locale.ROOT),
                    request.getContactNo(),
                    request.getAltContactNo(),
                    request.getFaxNo(),
                    request.getCoaId(),
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_vendor set v_vendor_code = ?, v_vendor_name = ?, "
                            + "v_vendor_address = ?, v_vendor_contact_person = ?, "
                            + "v_vendor_contact_no = ?, v_vendor_alt_contact_no = ?, "
                            + "v_vendor_fax_no = ?, n_coa_id = ?, "
                            + "v_who_change = ?, d_whn_change = now() "
                            + "where n_vendor_id = ?",
                    request.getCode().trim().toUpperCase(Locale.ROOT),
                    request.getName().trim().toUpperCase(Locale.ROOT),
                    request.getAddress().trim().toUpperCase(Locale.ROOT),
                    request.getContactPerson().trim().toUpperCase(Locale.ROOT),
                    request.getContactNo(),
                    request.getAltContactNo(),
                    request.getFaxNo(),
                    request.getCoaId(),
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus vendor. Mengikuti {@code VendorDAO.delete()}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_vendor where n_vendor_id = ?", id);
        return affected > 0;
    }

    private VendorRowResponse mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new VendorRowResponse(
                resultSet.getInt("n_vendor_id"),
                resultSet.getString("v_vendor_code"),
                resultSet.getString("v_vendor_name"),
                resultSet.getString("v_vendor_address"),
                resultSet.getString("v_vendor_contact_person"),
                resultSet.getString("v_vendor_contact_no"),
                resultSet.getString("v_vendor_alt_contact_no"),
                resultSet.getString("v_vendor_fax_no"),
                toInteger(resultSet.getObject("n_coa_id")),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name"));
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_vendor_n_vendor_id_seq')", Integer.class);
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

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
