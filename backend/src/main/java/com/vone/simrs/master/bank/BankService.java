package com.vone.simrs.master.bank;

import com.vone.simrs.master.treatment.CoaOptionResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0033 (BANK MASTER).
 * Mengikuti logika legacy {@code BankManagerImpl} + {@code MsBankDAO}.
 */
@Service
public class BankService {

    private final JdbcTemplate jdbcTemplate;

    public BankService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar bank. Mengikuti {@code MsBankDAO.findByExample(new MsBank())}
     * yang mengembalikan seluruh data bank.
     */
    public List<BankRowResponse> getBanks() {
        String sql = "select b.n_bank_id, b.v_bank_name, b.v_bank_acc_no, b.v_bank_addr, "
                + "b.v_bank_contact_no, b.v_bank_2nd_ctc_no, b.n_coa_id, "
                + "coa.v_acct_no, coa.v_acct_name "
                + "from ms_bank b "
                + "left join ms_coa coa on coa.n_coa_id = b.n_coa_id "
                + "order by b.v_bank_name";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new BankRowResponse(
                resultSet.getInt("n_bank_id"),
                resultSet.getString("v_bank_name"),
                resultSet.getString("v_bank_acc_no"),
                resultSet.getString("v_bank_addr"),
                resultSet.getString("v_bank_contact_no"),
                resultSet.getString("v_bank_2nd_ctc_no"),
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
     * Simpan / update bank. Mengikuti {@code BankController.doSaveAdd} dan
     * {@code doSaveModify}.
     */
    @Transactional
    public void save(BankSaveRequest request, String username) {
        String bankName = normalize(request.getBankName());
        String bankAccNo = normalize(request.getBankAccNo());

        if (bankName == null || bankName.isEmpty()) {
            throw new IllegalArgumentException("Nama bank harus diisi.");
        }
        if (bankAccNo == null || bankAccNo.isEmpty()) {
            throw new IllegalArgumentException("No. Account harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_bank (n_bank_id, v_bank_name, v_bank_acc_no, v_bank_addr, "
                            + "v_bank_contact_no, v_bank_2nd_ctc_no, n_coa_id, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, now())",
                    id,
                    bankName,
                    bankAccNo,
                    normalize(request.getBankAddr()),
                    normalize(request.getBankContactNo()),
                    normalize(request.getBank2ndCtcNo()),
                    request.getCoaId(),
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_bank set v_bank_name = ?, v_bank_acc_no = ?, v_bank_addr = ?, "
                            + "v_bank_contact_no = ?, v_bank_2nd_ctc_no = ?, n_coa_id = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_bank_id = ?",
                    bankName,
                    bankAccNo,
                    normalize(request.getBankAddr()),
                    normalize(request.getBankContactNo()),
                    normalize(request.getBank2ndCtcNo()),
                    request.getCoaId(),
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus bank. Mengikuti {@code MsBankDAO.delete}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_bank where n_bank_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_bank_n_bank_id_seq')",
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
