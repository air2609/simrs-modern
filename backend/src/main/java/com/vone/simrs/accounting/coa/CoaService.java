package com.vone.simrs.accounting.coa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0046 (CHART OF ACCOUNT).
 * Mengikuti logika legacy {@code COAManagerImpl} + {@code CoaDAO} +
 * {@code CoaController}.
 */
@Service
public class CoaService {

    private static final int COA_ALL = 0;
    private static final int COA_ACTIVE = 1;
    private static final int COA_INACTIVE = 2;
    private static final String COA_ACTIVE_STR = "ACTIVE";
    private static final String COA_INACTIVE_STR = "INACTIVE";

    private final JdbcTemplate jdbcTemplate;

    public CoaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar COA dalam bentuk tree parent-child.
     * Mengikuti {@code COAManagerImpl.redrawCoaController()}.
     */
    public List<CoaRowResponse> getCoaTree(Integer status, Integer typeId) {
        int statusSearch = status == null ? COA_ALL : status;
        List<CoaRowResponse> headers = queryCoa(statusSearch, typeId);
        Map<Integer, List<CoaRowResponse>> childrenByParent = new LinkedHashMap<>();
        for (CoaRowResponse header : headers) {
            childrenByParent.put(header.getCoaId(), queryChildren(header.getCoaId(), statusSearch, typeId));
        }
        List<CoaRowResponse> result = new ArrayList<>();
        for (CoaRowResponse header : headers) {
            result.add(new CoaRowResponse(
                    header.getCoaId(),
                    header.getSupCoaId(),
                    header.getTypeId(),
                    header.getTypeName(),
                    header.getAcctNo(),
                    header.getAcctName(),
                    header.getDesc(),
                    header.getBalance(),
                    header.getNaturalBalance(),
                    header.getStatus(),
                    header.getStatusLabel(),
                    childrenByParent.getOrDefault(header.getCoaId(), Collections.emptyList())));
        }
        return result;
    }

    /**
     * Opsi tipe account. Mengikuti {@code CoaDAO.getCoaType()}.
     */
    public List<CoaTypeOptionResponse> getCoaTypes() {
        String sql = "select n_ct_id, v_ct_name, n_ct_natural_balance "
                + "from ms_coa_type order by n_ct_id";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new CoaTypeOptionResponse(
                resultSet.getInt("n_ct_id"),
                resultSet.getString("v_ct_name"),
                toInteger(resultSet.getObject("n_ct_natural_balance"))));
    }

    /**
     * Opsi COA parent (tanpa child) untuk dropdown "Sub Account Of".
     * Mengikuti {@code CoaDAO.getCoaBaseOnTypeNoChild()}.
     */
    public List<CoaRowResponse> getCoaParentOptions(Integer typeId) {
        int type = typeId == null ? COA_ALL : typeId;
        String sql;
        if (type == COA_ALL) {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id is null "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet));
        } else {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_type = ? and c.n_sup_coa_id is null "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), type);
        }
    }

    /**
     * Simpan / update COA. Mengikuti {@code CoaController.doSave} dan
     * {@code doSaveModify}.
     */
    @Transactional
    public void save(CoaSaveRequest request, String username) {
        String acctNo = normalize(request.getAcctNo());
        String acctName = normalize(request.getAcctName());

        if (acctNo == null || acctNo.isEmpty()) {
            throw new IllegalArgumentException("Nomor akun harus diisi.");
        }
        if (acctName == null || acctName.isEmpty()) {
            throw new IllegalArgumentException("Nama akun harus diisi.");
        }
        if (request.getTypeId() == null) {
            throw new IllegalArgumentException("Tipe akun harus dipilih.");
        }

        int status = Boolean.TRUE.equals(request.getActive()) ? COA_ACTIVE : COA_INACTIVE;
        double balance = request.getBalance() == null ? 0.0 : request.getBalance();
        Integer supCoaId = request.getSupCoaId();

        Integer coaId = request.getCoaId();
        if (coaId == null) {
            // Insert baru
            Integer newId = nextCoaId();
            jdbcTemplate.update(
                    "insert into ms_coa (n_coa_id, n_sup_coa_id, n_type, v_acct_no, "
                            + "v_acct_name, v_desc, n_balance, n_status, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                    newId,
                    supCoaId,
                    request.getTypeId(),
                    acctNo,
                    acctName,
                    acctName,
                    balance,
                    status,
                    normalizeActor(username));
        } else {
            // Cek memilih dirinya sendiri sebagai superior
            if (supCoaId != null && supCoaId.intValue() == coaId.intValue()) {
                throw new IllegalArgumentException("Tidak dapat memilih akun yang sama sebagai sub account.");
            }
            jdbcTemplate.update(
                    "update ms_coa set n_sup_coa_id = ?, n_type = ?, v_acct_no = ?, "
                            + "v_acct_name = ?, v_desc = ?, n_balance = ?, n_status = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_coa_id = ?",
                    supCoaId,
                    request.getTypeId(),
                    acctNo,
                    acctName,
                    acctName,
                    balance,
                    status,
                    normalizeActor(username),
                    coaId);
        }
    }

    /**
     * Hapus COA. Mengikuti {@code CoaController.doDelete}.
     */
    @Transactional
    public boolean delete(Integer coaId) {
        if (coaId == null) {
            return false;
        }
        int affected = jdbcTemplate.update("delete from ms_coa where n_coa_id = ?", coaId);
        return affected > 0;
    }

    private List<CoaRowResponse> queryCoa(int status, Integer typeId) {
        boolean hasType = typeId != null && typeId != COA_ALL;
        String sql;
        if (status == COA_ALL && !hasType) {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id is null "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet));
        } else if (status == COA_ALL && hasType) {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id is null and c.n_type = ? "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), typeId);
        } else if (status != COA_ALL && !hasType) {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id is null and c.n_status = ? "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), status);
        } else {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id is null and c.n_status = ? and c.n_type = ? "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), status, typeId);
        }
    }

    private List<CoaRowResponse> queryChildren(Integer parentId, int status, Integer typeId) {
        boolean hasType = typeId != null && typeId != COA_ALL;
        String sql;
        if (status == COA_ALL && !hasType) {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id = ? "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), parentId);
        } else if (status == COA_ALL && hasType) {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id = ? and c.n_type = ? "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), parentId, typeId);
        } else if (status != COA_ALL && !hasType) {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id = ? and c.n_status = ? "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), parentId, status);
        } else {
            sql = "select c.n_coa_id, c.n_sup_coa_id, c.n_type, ct.v_ct_name, "
                    + "c.v_acct_no, c.v_acct_name, c.v_desc, c.n_balance, "
                    + "ct.n_ct_natural_balance, c.n_status "
                    + "from ms_coa c "
                    + "left join ms_coa_type ct on ct.n_ct_id = c.n_type "
                    + "where c.n_sup_coa_id = ? and c.n_status = ? and c.n_type = ? "
                    + "order by c.v_acct_no";
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), parentId, status, typeId);
        }
    }

    private CoaRowResponse mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        Integer status = toInteger(resultSet.getObject("n_status"));
        String statusLabel = null;
        if (status != null) {
            if (status == COA_ACTIVE) {
                statusLabel = COA_ACTIVE_STR;
            } else if (status == COA_INACTIVE) {
                statusLabel = COA_INACTIVE_STR;
            }
        }
        return new CoaRowResponse(
                resultSet.getInt("n_coa_id"),
                toInteger(resultSet.getObject("n_sup_coa_id")),
                toInteger(resultSet.getObject("n_type")),
                resultSet.getString("v_ct_name"),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name"),
                resultSet.getString("v_desc"),
                toDouble(resultSet.getObject("n_balance")),
                toInteger(resultSet.getObject("n_ct_natural_balance")),
                status,
                statusLabel,
                Collections.emptyList());
    }

    private Integer nextCoaId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_coa_n_coa_id_seq')",
                Integer.class);
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
