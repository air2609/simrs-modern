package com.vone.simrs.master.cardtype;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0048 (MASTER CARD TYPE / FORM TIPE KARTU BANK).
 * Mengikuti logika legacy {@code CreditCardTypeManagerImpl} +
 * {@code CreditCardTypeDAO}.
 */
@Service
public class CardTypeService {

    private final JdbcTemplate jdbcTemplate;

    public CardTypeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar tipe kartu bank. Mengikuti
     * {@code CreditCardTypeDAO.getCreditCardTypes()}
     * yang mengembalikan seluruh data ms_credit_card_type.
     */
    public List<CardTypeRowResponse> getCardTypes() {
        String sql = "select ct.n_cc_type_id, ct.n_bank_payment_type_id, "
                + "ct.n_bank_id, b.v_bank_name, "
                + "ct.n_coa_id, coa.v_acct_no, coa.v_acct_name, "
                + "ct.n_cc_type_desc "
                + "from ms_credit_card_type ct "
                + "left join ms_bank b on b.n_bank_id = ct.n_bank_id "
                + "left join ms_coa coa on coa.n_coa_id = ct.n_coa_id "
                + "order by b.v_bank_name, ct.n_cc_type_desc";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            Short paymentType = resultSet.getShort("n_bank_payment_type_id");
            return new CardTypeRowResponse(
                    resultSet.getInt("n_cc_type_id"),
                    paymentType,
                    convert2CardType(paymentType),
                    toInteger(resultSet.getObject("n_bank_id")),
                    resultSet.getString("v_bank_name"),
                    toInteger(resultSet.getObject("n_coa_id")),
                    resultSet.getString("v_acct_no"),
                    resultSet.getString("v_acct_name"),
                    resultSet.getString("n_cc_type_desc"));
        });
    }

    /**
     * Data master untuk form: opsi bank untuk dropdown NAMA BANK.
     * Mengikuti {@code BankController.getBanks()} pada tabel ms_bank.
     */
    public CardTypeMastersResponse getMasters() {
        List<BankOptionResponse> bankOptions = jdbcTemplate.query(
                "select n_bank_id, v_bank_name from ms_bank order by v_bank_name",
                (resultSet, rowNum) -> new BankOptionResponse(
                        resultSet.getInt("n_bank_id"),
                        resultSet.getString("v_bank_name")));
        return new CardTypeMastersResponse(bankOptions);
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
     * Simpan / update tipe kartu bank. Mengikuti
     * {@code CreditCardTypeController.doSaveAdd}
     * dan {@code doSaveModify} (saveOrUpdate).
     */
    @Transactional
    public void save(CardTypeSaveRequest request, String username) {
        if (request.getBankId() == null) {
            throw new IllegalArgumentException("Nama bank harus diisi.");
        }
        if (request.getCoaId() == null) {
            throw new IllegalArgumentException("No. COA harus diisi.");
        }
        if (request.getPaymentType() == null) {
            throw new IllegalArgumentException("Tipe kartu harus diisi.");
        }
        if (request.getCardName() == null || request.getCardName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nama kartu harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_credit_card_type (n_cc_type_id, n_bank_payment_type_id, "
                            + "n_bank_id, n_coa_id, n_cc_type_desc, "
                            + "v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, now())",
                    id,
                    request.getPaymentType(),
                    request.getBankId(),
                    request.getCoaId(),
                    request.getCardName().trim().toUpperCase(Locale.ROOT),
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_credit_card_type set n_bank_payment_type_id = ?, "
                            + "n_bank_id = ?, n_coa_id = ?, n_cc_type_desc = ?, "
                            + "v_who_change = ?, d_whn_change = now() "
                            + "where n_cc_type_id = ?",
                    request.getPaymentType(),
                    request.getBankId(),
                    request.getCoaId(),
                    request.getCardName().trim().toUpperCase(Locale.ROOT),
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus tipe kartu bank. Mengikuti {@code CreditCardTypeDAO.delete()}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_credit_card_type where n_cc_type_id = ?", id);
        return affected > 0;
    }

    /**
     * Konversi tipe pembayaran ke label. Mengikuti
     * {@code MedisafeUtil.convert2CardType()}.
     */
    private String convert2CardType(Short type) {
        if (type != null && type.shortValue() == 1) {
            return "KARTU KREDIT";
        }
        return "KARTU DEBIT";
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_credit_card_type_n_cc_type_id_seq')", Integer.class);
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
