package com.vone.simrs.purchasing;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0194 (FORM PERSETUJUAN &amp; PEMBATALAN ORDER
 * PEMBELIAN / poApproval.zul).
 *
 * <p>
 * Migrasi dari legacy {@code POApproval} + {@code POManagerImpl}:
 * <ul>
 * <li>{@code POApproval.doSearch()} +
 * {@code POManagerImpl.doSearchApproval()} → {@link #searchPo(String, boolean)}</li>
 * <li>{@code POApproval.redraw()} +
 * {@code POManagerImpl.redraw(POApproval, ...)} → {@link #getApprovalDetail(String)}</li>
 * <li>{@code POApproval.doApprove()} +
 * {@code POManagerImpl.doApprove(POApproval, ...)} → {@link #approve(String, String)}</li>
 * </ul>
 */
@Service
public class PurchaseOrderApprovalService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_APPROVED = "APPROVED";

    private final JdbcTemplate jdbcTemplate;

    public PurchaseOrderApprovalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Pencarian OP untuk bandbox NO. OP. Migrasi dari legacy
     * {@code POManagerImpl.doSearchApproval()}: jika checkbox OP VALIDATED
     * dicentang maka ditampilkan OP berstatus APPROVED, jika tidak maka OP
     * berstatus OPEN.
     */
    public List<PurchaseOrderPoOptionResponse> searchPo(String poCode, boolean validated) {
        String status = validated ? STATUS_APPROVED : STATUS_OPEN;
        return jdbcTemplate.query(
                "select po.v_po_code, coalesce(v.v_vendor_name, '-') as v_vendor_name "
                        + "from tb_purchase_order po "
                        + "left join ms_vendor v on v.n_vendor_id = po.n_vendor_id "
                        + "where upper(po.v_po_code) like ? and po.v_po_status = ? "
                        + "order by po.d_whn_create desc limit 100",
                (resultSet, rowNum) -> new PurchaseOrderPoOptionResponse(
                        resultSet.getString("v_po_code"),
                        resultSet.getString("v_vendor_name"),
                        ""),
                like(poCode), status);
    }

    /**
     * Header + detail OP untuk layar persetujuan. Migrasi dari legacy
     * {@code POManagerImpl.redraw(POApproval, ...)} yang mengisi DIBUAT OLEH,
     * DISETUJUI OLEH, status, dan daftar item (KODE, KETERANGAN, HRG SATUAN,
     * JUMLAH ORD., SATUAN, BONUS, DISKON, SUBTOTAL).
     */
    public PurchaseOrderApprovalDetailResponse getApprovalDetail(String poCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select po.n_po_id, po.v_po_code, po.v_po_status, "
                            + "coalesce(iss.v_staff_code, '') || '-' || coalesce(iss.v_staff_name, '') as issuer_name, "
                            + "case when po.n_approver_id is null then '' "
                            + "  else coalesce(appr.v_staff_code, '') || '-' || coalesce(appr.v_staff_name, '') end as approver_name "
                            + "from tb_purchase_order po "
                            + "left join ms_staff iss on iss.n_staff_id = po.n_issuer_id "
                            + "left join ms_staff appr on appr.n_staff_id = po.n_approver_id "
                            + "where po.v_po_code = ?",
                    (resultSet, rowNum) -> new PurchaseOrderApprovalDetailResponse(
                            resultSet.getString("v_po_code"),
                            resultSet.getString("v_po_status"),
                            resultSet.getString("issuer_name"),
                            resultSet.getString("approver_name"),
                            loadItems(resultSet.getInt("n_po_id"))),
                    poCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }
    }

    /**
     * Aksi DISETUJUI: ubah status OP menjadi APPROVED dan isi approver dengan
     * staff user yang sedang login. Migrasi dari legacy
     * {@code POManagerImpl.doApprove(POApproval, ...)}.
     */
    @Transactional
    public PurchaseOrderApprovalResultResponse approve(String poCode, String username) {
        if (poCode == null || poCode.trim().isEmpty()) {
            throw new IllegalArgumentException("NO. OP WAJIB DIISI.");
        }

        Integer poId = findPoId(poCode.trim());
        if (poId == null) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }

        String status = jdbcTemplate.queryForObject(
                "select v_po_status from tb_purchase_order where n_po_id = ?",
                String.class, poId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("STATUS OP SUDAH TIDAK OPEN.");
        }

        Integer staffId = findStaffId(username);
        if (staffId == null) {
            throw new IllegalArgumentException("STAFF USER TIDAK DITEMUKAN.");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_purchase_order set v_po_status = ?, n_approver_id = ?, "
                        + "v_who_change = ?, d_whn_change = ? where n_po_id = ?",
                STATUS_APPROVED, staffId, normalize(username), now, poId);

        String approvedByName = staffCodeName(staffId);
        return new PurchaseOrderApprovalResultResponse(
                poCode.trim(), STATUS_APPROVED, approvedByName);
    }

    private List<PurchaseOrderApprovalDetailResponse.Item> loadItems(Integer poId) {
        String sql = "select d.n_item_id, it.v_item_code, it.v_item_name, "
                + "coalesce(m.v_mitem_end_quantify, '-') as v_mitem_end_quantify, "
                + "d.n_po_det_cost, d.n_po_det_qty_ordered, "
                + "coalesce(d.n_bonus, 0) as n_bonus, coalesce(d.n_discount, 0) as n_discount, "
                + "coalesce(d.v_discount_type, 'RP') as v_discount_type, d.n_subtotal "
                + "from tb_purchase_order_detail d "
                + "join ms_item it on it.n_item_id = d.n_item_id "
                + "left join ms_item_measurement m on m.n_mitem_id = d.n_measurement_id "
                + "where d.n_po_id = ? order by d.n_po_det_id";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new PurchaseOrderApprovalDetailResponse.Item(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getDouble("n_po_det_cost"),
                resultSet.getInt("n_po_det_qty_ordered"),
                resultSet.getString("v_mitem_end_quantify"),
                resultSet.getInt("n_bonus"),
                resultSet.getDouble("n_discount"),
                resultSet.getString("v_discount_type"),
                resultSet.getDouble("n_subtotal")),
                poId);
    }

    private Integer findPoId(String poCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_po_id from tb_purchase_order where v_po_code = ?",
                    Integer.class, poCode);
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

    private String staffCodeName(Integer staffId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coalesce(v_staff_code, '') || '-' || coalesce(v_staff_name, '') "
                            + "from ms_staff where n_staff_id = ?",
                    String.class, staffId);
        } catch (EmptyResultDataAccessException exception) {
            return "";
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String like(String value) {
        return "%" + normalize(value) + "%";
    }
}
