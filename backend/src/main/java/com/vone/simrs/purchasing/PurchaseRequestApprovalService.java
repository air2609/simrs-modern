package com.vone.simrs.purchasing;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0192 (FORM PERSETUJUAN ORDER PERMINTAAN PEMBELIAN).
 *
 * <p>
 * Migrasi dari legacy {@code PORApproval} + {@code PORManagerImpl}:
 * <ul>
 * <li>{@code PORApproval.doSearch()} +
 * {@code PORManagerImpl.doSearchPORApproval()} → {@link #searchOpp(String)}</li>
 * <li>{@code PORApproval.redraw()} +
 * {@code PORManagerImpl.redrawPORApproval()} → {@link #getApprovalDetail(String)}</li>
 * <li>{@code PORApproval.doApprove()} +
 * {@code PORManagerImpl.doApprove()} → {@link #approve(String, String)}</li>
 * </ul>
 */
@Service
public class PurchaseRequestApprovalService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_APPROVED = "APPROVED";

    private final JdbcTemplate jdbcTemplate;

    public PurchaseRequestApprovalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Pencarian OPP dengan status OPEN untuk bandbox NO. OPP. Migrasi dari
     * legacy
     * {@code TbPurchaseRequestDAO.searchTbPurchaseRequestByCodeForApproval()}
     * yang hanya menampilkan OPP berstatus OPEN.
     */
    public List<PurchaseRequestOppOptionResponse> searchOpp(String prCode) {
        return jdbcTemplate.query(
                "select pr.v_pr_code, coalesce(unt.v_unit_name, '-') as v_unit_name "
                        + "from tb_purchase_request pr "
                        + "left join ms_unit unt on unt.n_unit_id = pr.n_unit_id "
                        + "where upper(pr.v_pr_code) like ? and pr.v_pr_status = ? "
                        + "order by pr.v_pr_code desc limit 100",
                (resultSet, rowNum) -> new PurchaseRequestOppOptionResponse(
                        resultSet.getString("v_pr_code"),
                        resultSet.getString("v_unit_name")),
                like(prCode), STATUS_OPEN);
    }

    /**
     * Header + detail OPP untuk layar persetujuan. Migrasi dari legacy
     * {@code PORManagerImpl.redrawPORApproval()} yang mengisi field DIBUAT
     * OLEH, DISETUJUI OLEH, status, daftar item (termasuk STOK tersedia), dan
     * data supplier dari {@code MsVendor}.
     */
    public PurchaseRequestApprovalDetailResponse getApprovalDetail(String prCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select pr.n_pr_id, pr.v_pr_code, pr.v_pr_status, "
                            + "pr.n_unit_id, coalesce(unt.v_unit_name, '-') as v_unit_name, "
                            + "unt.n_whouse_id, "
                            + "coalesce(iss.v_staff_code, '') || '-' || coalesce(iss.v_staff_name, '') as issuer_name, "
                            + "case when pr.n_approver_id is null then '' "
                            + "  else coalesce(appr.v_staff_code, '') || '-' || coalesce(appr.v_staff_name, '') end as approver_name, "
                            + "pr.n_supplier_id, "
                            + "coalesce(v.v_vendor_code, '-') as v_vendor_code, "
                            + "coalesce(v.v_vendor_name, '-') as v_vendor_name, "
                            + "coalesce(v.v_vendor_address, '') as v_vendor_address, "
                            + "coalesce(v.v_vendor_contact_no, '') as v_vendor_contact_no "
                            + "from tb_purchase_request pr "
                            + "left join ms_unit unt on unt.n_unit_id = pr.n_unit_id "
                            + "left join ms_staff iss on iss.n_staff_id = pr.n_issuer_id "
                            + "left join ms_staff appr on appr.n_staff_id = pr.n_approver_id "
                            + "left join ms_vendor v on v.n_vendor_id = pr.n_supplier_id "
                            + "where pr.v_pr_code = ?",
                    (resultSet, rowNum) -> new PurchaseRequestApprovalDetailResponse(
                            resultSet.getString("v_pr_code"),
                            resultSet.getString("v_pr_status"),
                            resultSet.getString("issuer_name"),
                            resultSet.getString("approver_name"),
                            toInteger(resultSet.getObject("n_unit_id")),
                            resultSet.getString("v_unit_name"),
                            toInteger(resultSet.getObject("n_whouse_id")),
                            toInteger(resultSet.getObject("n_supplier_id")),
                            resultSet.getString("v_vendor_code"),
                            resultSet.getString("v_vendor_name"),
                            resultSet.getString("v_vendor_address"),
                            resultSet.getString("v_vendor_contact_no"),
                            loadDetailItems(resultSet.getInt("n_pr_id"),
                                    toInteger(resultSet.getObject("n_whouse_id")))),
                    prCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }
    }

    /**
     * Aksi DISETUJUI: ubah status OPP menjadi APPROVED dan isi approver dengan
     * staff user yang sedang login. Migrasi dari legacy
     * {@code PORManagerImpl.doApprove()}.
     */
    @Transactional
    public PurchaseRequestApprovalResultResponse approve(String prCode, String username) {
        if (prCode == null || prCode.trim().isEmpty()) {
            throw new IllegalArgumentException("NO. OPP WAJIB DIISI.");
        }

        Integer prId = findPrId(prCode.trim());
        if (prId == null) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }

        String status = jdbcTemplate.queryForObject(
                "select v_pr_status from tb_purchase_request where n_pr_id = ?",
                String.class, prId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("STATUS OPP SUDAH TIDAK OPEN.");
        }

        Integer staffId = findStaffId(username);
        if (staffId == null) {
            throw new IllegalArgumentException("STAFF USER TIDAK DITEMUKAN.");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_purchase_request set v_pr_status = ?, n_approver_id = ?, "
                        + "v_who_change = ?, d_whn_change = ? where n_pr_id = ?",
                STATUS_APPROVED, staffId, normalize(username), now, prId);

        String approvedByName = jdbcTemplate.queryForObject(
                "select coalesce(stf.v_staff_code, '') || '-' || coalesce(stf.v_staff_name, '') "
                        + "from ms_staff stf where stf.n_staff_id = ?",
                String.class, staffId);

        return new PurchaseRequestApprovalResultResponse(
                prCode.trim(), STATUS_APPROVED, approvedByName);
    }

    private List<PurchaseRequestApprovalItemResponse> loadDetailItems(Integer prId, Integer warehouseId) {
        String sql = "select d.n_item_id, it.v_item_code, it.v_item_name, "
                + "coalesce(g.v_item_group_code, '-') as v_item_group_code, "
                + "coalesce(m.v_mitem_early_quantify, '-') as v_mitem_early_quantify, "
                + "d.n_pr_det_qty_requested "
                + "from tb_purchase_request_detail d "
                + "join ms_item it on it.n_item_id = d.n_item_id "
                + "left join ms_item_group g on g.n_item_group_id = it.n_item_group_id "
                + "left join ms_item_measurement m on m.n_mitem_id = d.n_mitem_id "
                + "where d.n_pr_id = ? order by d.n_pr_det_id";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new PurchaseRequestApprovalItemResponse(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_item_group_code"),
                getQtyAvail(warehouseId, resultSet.getInt("n_item_id")),
                getBufferLimit(resultSet.getInt("n_item_id")),
                getMaxOrder(resultSet.getInt("n_item_id")),
                resultSet.getString("v_mitem_early_quantify"),
                resultSet.getInt("n_pr_det_qty_requested")),
                prId);
    }

    /**
     * Stok tersedia untuk sebuah item di warehouse unit peminta. Migrasi dari
     * legacy {@code MsWarehouseDAO.getQtyAvail()}.
     */
    private Integer getQtyAvail(Integer warehouseId, Integer itemId) {
        if (warehouseId == null || itemId == null) {
            return 0;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "select coalesce(sum(inv.n_item_inv_qty), 0) from tb_item_inventory inv "
                            + "where inv.n_whouse_id = ? and inv.n_item_id = ? and inv.n_item_inv_qty > 0",
                    Integer.class, warehouseId, itemId);
        } catch (EmptyResultDataAccessException exception) {
            return 0;
        }
    }

    private Integer getBufferLimit(Integer itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coalesce(n_item_buffer_limit, 0) from ms_item where n_item_id = ?",
                    Integer.class, itemId);
        } catch (EmptyResultDataAccessException exception) {
            return 0;
        }
    }

    private Integer getMaxOrder(Integer itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coalesce(n_max_order, 0) from ms_item where n_item_id = ?",
                    Integer.class, itemId);
        } catch (EmptyResultDataAccessException exception) {
            return 0;
        }
    }

    private Integer findPrId(String prCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_pr_id from tb_purchase_request where v_pr_code = ?",
                    Integer.class, prCode);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private Integer findStaffId(String username) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_staff_id from ms_user where upper(v_user_name) = ?",
                    Integer.class, normalize(username));
        } catch (EmptyResultDataAccessException exception) {
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
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String like(String value) {
        return "%" + normalize(value) + "%";
    }
}
