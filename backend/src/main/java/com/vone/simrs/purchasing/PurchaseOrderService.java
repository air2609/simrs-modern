package com.vone.simrs.purchasing;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0193 (ORDER PEMBELIAN / OP).
 *
 * <p>
 * Migrasi dari legacy {@code POController} + {@code POManagerImpl} +
 * {@code TbPurchaseOrderDAO}:
 * <ul>
 * <li>{@code POController.init()} → {@link #getMasters(String)}</li>
 * <li>{@code POManagerImpl.doSearchPOR()} → {@link #searchOpp(String)}</li>
 * <li>{@code POManagerImpl.redraw()} → {@link #getOppDetail(String)}</li>
 * <li>{@code POManagerImpl.doSearchPO()} → {@link #searchPo(String, String)}</li>
 * <li>{@code POManagerImpl.redrawPO()} → {@link #getPoDetail(String)}</li>
 * <li>{@code POManagerImpl.doSearchSupPOController()} → {@link #searchSuppliers(String, String)}</li>
 * <li>{@code POController.doSaveAdd()} → {@link #save(PurchaseOrderSaveRequest, String)}</li>
 * <li>{@code POController.doSaveModify()} → {@link #update(PurchaseOrderUpdateRequest, String)}</li>
 * <li>{@code POController.doRevoke()} → {@link #revoke(String, String)}</li>
 * <li>{@code POController.doClosePOR()} → {@link #closeOpp(String, String)}</li>
 * <li>{@code POController.cetakPO()} → {@link #getPrintData(String, String)}</li>
 * </ul>
 */
@Service
public class PurchaseOrderService {

    private static final String SCREEN_CODE = "SC0193";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String DISCOUNT_TYPE_RP = "RP";

    private final JdbcTemplate jdbcTemplate;

    public PurchaseOrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Data master untuk form: unit LOKASI TRANSAKSI untuk screen SC0193 dan
     * daftar satuan (measurement). Migrasi dari legacy
     * {@code POController.init()}.
     */
    public PurchaseOrderMastersResponse getMasters(String username) {
        List<PurchaseRequestUnitResponse> units = jdbcTemplate.query(
                "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name, unt.n_whouse_id "
                        + "from ms_screen scr "
                        + "join ms_user usr on upper(usr.v_user_name) = ? "
                        + "join ms_staff_in_unit stfunit on stfunit.n_staff_id = usr.n_staff_id "
                        + "join ms_unit unt on unt.n_unit_id = stfunit.n_unit_id "
                        + "where scr.v_screen_code = ? and unt.n_whouse_id is not null "
                        + "order by unt.v_unit_name",
                (resultSet, rowNum) -> new PurchaseRequestUnitResponse(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        resultSet.getInt("n_whouse_id")),
                normalize(username), SCREEN_CODE);

        List<PurchaseRequestMeasurementResponse> measurements = jdbcTemplate.query(
                "select n_mitem_id, v_mitem_early_quantify, v_mitem_end_quantify "
                        + "from ms_item_measurement order by v_mitem_end_quantify",
                (resultSet, rowNum) -> new PurchaseRequestMeasurementResponse(
                        resultSet.getInt("n_mitem_id"),
                        resultSet.getString("v_mitem_early_quantify"),
                        resultSet.getString("v_mitem_end_quantify")));

        return new PurchaseOrderMastersResponse(units, measurements);
    }

    /**
     * Pencarian OPP dengan status APPROVED untuk bandbox NO. OPP. Migrasi dari
     * legacy {@code POManagerImpl.doSearchPOR()} yang memakai
     * {@code searchTbPurchaseRequestByCode(code, APPROVED)}.
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
                like(prCode), STATUS_APPROVED);
    }

    /**
     * Header + detail OPP (status APPROVED) untuk dijadikan OP. Migrasi dari
     * legacy {@code POManagerImpl.redraw()} yang menghitung ORD/S (sisa) dan
     * harga terakhir (HRG SAT).
     */
    public PurchaseOrderOppDetailResponse getOppDetail(String prCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select pr.n_pr_id, pr.v_pr_code, pr.n_supplier_id, "
                            + "coalesce(v.v_vendor_code, '-') as v_vendor_code, "
                            + "coalesce(v.v_vendor_name, '-') as v_vendor_name, "
                            + "coalesce(v.v_vendor_address, '') as v_vendor_address, "
                            + "coalesce(v.v_vendor_contact_no, '') as v_vendor_contact_no "
                            + "from tb_purchase_request pr "
                            + "left join ms_vendor v on v.n_vendor_id = pr.n_supplier_id "
                            + "where pr.v_pr_code = ?",
                    (resultSet, rowNum) -> new PurchaseOrderOppDetailResponse(
                            resultSet.getString("v_pr_code"),
                            toInteger(resultSet.getObject("n_supplier_id")),
                            resultSet.getString("v_vendor_code"),
                            resultSet.getString("v_vendor_name"),
                            resultSet.getString("v_vendor_address"),
                            resultSet.getString("v_vendor_contact_no"),
                            loadOppLines(resultSet.getInt("n_pr_id"))),
                    prCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }
    }

    /**
     * Pencarian OP untuk bandbox NO. OP (status bukan CLOSED). Migrasi dari
     * legacy {@code POManagerImpl.doSearchPO()} +
     * {@code TbPurchaseOrderDAO.searchPOByCodeSup()}.
     */
    public List<PurchaseOrderPoOptionResponse> searchPo(String poCode, String supName) {
        return jdbcTemplate.query(
                "select po.v_po_code, coalesce(v.v_vendor_name, '-') as v_vendor_name, "
                        + "po.d_whn_create "
                        + "from tb_purchase_order po "
                        + "left join ms_vendor v on v.n_vendor_id = po.n_vendor_id "
                        + "where upper(po.v_po_code) like ? and upper(coalesce(v.v_vendor_name, '')) like ? "
                        + "and po.v_po_status != ? "
                        + "order by po.d_whn_create desc limit 100",
                (resultSet, rowNum) -> new PurchaseOrderPoOptionResponse(
                        resultSet.getString("v_po_code"),
                        resultSet.getString("v_vendor_name"),
                        toDisplayDate(resultSet.getTimestamp("d_whn_create"))),
                like(poCode), like(supName), STATUS_CLOSED);
    }

    /**
     * Header + detail OP untuk diubah. Migrasi dari legacy
     * {@code POManagerImpl.redrawPO()}.
     */
    public PurchaseOrderPoDetailResponse getPoDetail(String poCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select po.n_po_id, po.v_po_code, po.v_po_status, "
                            + "coalesce(pr.v_pr_code, '') as v_pr_code, "
                            + "coalesce(iss.v_staff_code, '') || '-' || coalesce(iss.v_staff_name, '') as issuer_name, "
                            + "case when po.n_approver_id is null then '' "
                            + "  else coalesce(appr.v_staff_code, '') || '-' || coalesce(appr.v_staff_name, '') end as approver_name, "
                            + "po.n_vendor_id, "
                            + "coalesce(v.v_vendor_code, '-') as v_vendor_code, "
                            + "coalesce(v.v_vendor_name, '-') as v_vendor_name, "
                            + "coalesce(v.v_vendor_address, '') as v_vendor_address, "
                            + "coalesce(v.v_vendor_contact_no, '') as v_vendor_contact_no, "
                            + "po.d_payment_due, po.n_subtotal, po.n_discount, "
                            + "coalesce(po.v_discount_type, 'RP') as v_discount_type, po.n_total "
                            + "from tb_purchase_order po "
                            + "left join tb_purchase_request pr on pr.n_pr_id = po.n_pr_id "
                            + "left join ms_staff iss on iss.n_staff_id = po.n_issuer_id "
                            + "left join ms_staff appr on appr.n_staff_id = po.n_approver_id "
                            + "left join ms_vendor v on v.n_vendor_id = po.n_vendor_id "
                            + "where po.v_po_code = ?",
                    (resultSet, rowNum) -> new PurchaseOrderPoDetailResponse(
                            resultSet.getString("v_po_code"),
                            resultSet.getString("v_po_status"),
                            resultSet.getString("v_pr_code"),
                            resultSet.getString("issuer_name"),
                            resultSet.getString("approver_name"),
                            toInteger(resultSet.getObject("n_vendor_id")),
                            resultSet.getString("v_vendor_code"),
                            resultSet.getString("v_vendor_name"),
                            resultSet.getString("v_vendor_address"),
                            resultSet.getString("v_vendor_contact_no"),
                            toIsoDate(resultSet.getTimestamp("d_payment_due")),
                            toDouble(resultSet.getObject("n_subtotal")),
                            toDouble(resultSet.getObject("n_discount")),
                            resultSet.getString("v_discount_type"),
                            toDouble(resultSet.getObject("n_total")),
                            loadPoLines(resultSet.getInt("n_po_id"))),
                    poCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }
    }

    /**
     * Pencarian supplier/vendor pada bandbox SUPPLIER. Migrasi dari legacy
     * {@code POManagerImpl.doSearchSupPOController()} +
     * {@code VendorDAO.searchVendor()}.
     */
    public List<PurchaseRequestSupplierResponse> searchSuppliers(String code, String name) {
        return jdbcTemplate.query(
                "select n_vendor_id, v_vendor_code, v_vendor_name, "
                        + "coalesce(v_vendor_address, '') as v_vendor_address, "
                        + "coalesce(v_vendor_contact_no, '') as v_vendor_contact_no "
                        + "from ms_vendor "
                        + "where upper(v_vendor_code) like ? and upper(v_vendor_name) like ? "
                        + "order by v_vendor_name limit 100",
                (resultSet, rowNum) -> new PurchaseRequestSupplierResponse(
                        resultSet.getInt("n_vendor_id"),
                        resultSet.getString("v_vendor_code"),
                        resultSet.getString("v_vendor_name"),
                        resultSet.getString("v_vendor_address"),
                        resultSet.getString("v_vendor_contact_no")),
                like(code), like(name));
    }

    /**
     * Simpan OP baru. Migrasi dari legacy {@code POController.doSaveAdd()}.
     */
    @Transactional
    public PurchaseOrderSaveResultResponse save(PurchaseOrderSaveRequest request, String username) {
        if (request.getPrCode() == null || request.getPrCode().trim().isEmpty()) {
            throw new IllegalArgumentException("DATA PURCHASE REQUEST TIDAK VALID! MOHON DI-CEK ULANG");
        }
        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException("DATA SUPPLIER TIDAK VALID! MOHON DI-CEK ULANG");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("LIST ITEM KOSONG, TIDAK DAPAT DISIMPAN!");
        }

        Integer staffId = findStaffId(username);
        if (staffId == null) {
            throw new IllegalArgumentException("STAFF USER TIDAK DITEMUKAN.");
        }

        Integer prId = findPrId(request.getPrCode().trim());
        if (prId == null) {
            throw new IllegalArgumentException("DATA PURCHASE REQUEST TIDAK VALID! MOHON DI-CEK ULANG");
        }

        String poCode = generatePoCode(request.getUnitCode());
        Integer poId = nextVal("tb_purchase_order_n_po_id_seq");
        Timestamp now = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update(
                "insert into tb_purchase_order (n_po_id, n_issuer_id, n_pr_id, n_vendor_id, "
                        + "v_po_code, v_po_status, n_discount, v_discount_type, n_total, n_subtotal, "
                        + "d_payment_due, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                poId, staffId, prId, request.getSupplierId(), poCode, STATUS_OPEN,
                valueOrZero(request.getDiscount()), normalize(request.getDiscountType()),
                valueOrZero(request.getTotal()), valueOrZero(request.getSubtotal()),
                parseDueDate(request.getDueDate()), normalize(username), now);

        for (PurchaseOrderSaveRequest.Line line : request.getLines()) {
            insertDetail(poId, line, username, now);
        }

        return new PurchaseOrderSaveResultResponse(poCode, STATUS_OPEN,
                staffCodeName(staffId));
    }

    /**
     * Ubah OP yang masih OPEN. Migrasi dari legacy
     * {@code POController.doSaveModify()} + {@code TbPurchaseOrderDAO.update()}
     * yang menghapus detail lama lalu menyimpan ulang.
     */
    @Transactional
    public void update(PurchaseOrderUpdateRequest request, String username) {
        if (request.getPoCode() == null || request.getPoCode().trim().isEmpty()) {
            throw new IllegalArgumentException("NO. OP WAJIB DIISI.");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("LIST ITEM KOSONG, TIDAK DAPAT DISIMPAN!");
        }

        Integer poId = findPoId(request.getPoCode().trim());
        if (poId == null) {
            throw new IllegalArgumentException("Internal Error! OP No Tidak Ada!");
        }
        String status = jdbcTemplate.queryForObject(
                "select v_po_status from tb_purchase_order where n_po_id = ?",
                String.class, poId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("Tidak Dapat Melakukan Modifikasi. OP Sudah DiValidasi!");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_purchase_order set d_payment_due = ?, n_subtotal = ?, n_discount = ?, "
                        + "v_discount_type = ?, n_total = ?, v_who_change = ?, d_whn_change = ? "
                        + "where n_po_id = ?",
                parseDueDate(request.getDueDate()), valueOrZero(request.getSubtotal()),
                valueOrZero(request.getDiscount()), normalize(request.getDiscountType()),
                valueOrZero(request.getTotal()), normalize(username), now, poId);

        jdbcTemplate.update("delete from tb_purchase_order_detail where n_po_id = ?", poId);

        for (PurchaseOrderSaveRequest.Line line : request.getLines()) {
            insertDetail(poId, line, username, now);
        }
    }

    /**
     * Pembatalan order (status OPEN → REVOKED). Migrasi dari legacy
     * {@code POController.doRevoke()}.
     */
    @Transactional
    public void revoke(String poCode, String username) {
        if (poCode == null || poCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Internal Error! OP No Tidak Ada!");
        }
        Integer poId = findPoId(poCode.trim());
        if (poId == null) {
            throw new IllegalArgumentException("Internal Error! OP No Tidak Ada!");
        }
        String status = jdbcTemplate.queryForObject(
                "select v_po_status from tb_purchase_order where n_po_id = ?",
                String.class, poId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("Tidak Dapat Melakukan Modifikasi. OP Sudah DiValidasi!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_purchase_order set v_po_status = ?, v_who_change = ?, d_whn_change = ? "
                        + "where n_po_id = ?",
                STATUS_REVOKED, normalize(username), now, poId);
    }

    /**
     * TUTUP OPP: ubah status OPP menjadi CLOSED. Migrasi dari legacy
     * {@code POController.doClosePOR()}.
     */
    @Transactional
    public void closeOpp(String prCode, String username) {
        if (prCode == null || prCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Internal Error! OPP No Tidak Ada!");
        }
        Integer prId = findPrId(prCode.trim());
        if (prId == null) {
            throw new IllegalArgumentException("Internal Error! OPP No Tidak Ada!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_purchase_request set v_pr_status = ?, v_who_change = ?, d_whn_change = ? "
                        + "where n_pr_id = ?",
                STATUS_CLOSED, normalize(username), now, prId);
    }

    /**
     * Data cetak PURCHASE ORDER. Migrasi dari legacy
     * {@code POController.cetakPO()} (query + report orderPembelian.jrxml).
     */
    public PurchaseOrderPrintData getPrintData(String poCode, String username) {
        List<PurchaseOrderPrintData.Line> lines = jdbcTemplate.query(
                "select i.v_item_code || ' ' || i.v_item_name as item, "
                        + "m.v_mitem_early_quantify as satuan, "
                        + "rd.n_pr_det_qty_requested as quantity_request, "
                        + "d.n_po_det_qty_ordered as quantity_realisasi, "
                        + "d.n_po_det_cost as harga_satuan, d.n_subtotal "
                        + "from tb_purchase_order o "
                        + "join tb_purchase_request r on r.n_pr_id = o.n_pr_id "
                        + "join tb_purchase_request_detail rd on rd.n_pr_id = r.n_pr_id "
                        + "join ms_vendor v on v.n_vendor_id = o.n_vendor_id "
                        + "join tb_purchase_order_detail d on d.n_po_id = o.n_po_id "
                        + "join ms_item i on i.n_item_id = d.n_item_id "
                        + "join ms_item_measurement m on m.n_mitem_id = rd.n_mitem_id "
                        + "where o.v_po_code = ? and rd.n_item_id = i.n_item_id "
                        + "and d.n_measurement_id = m.n_mitem_id "
                        + "order by d.n_po_det_id",
                (resultSet, rowNum) -> new PurchaseOrderPrintData.Line(
                        resultSet.getString("item"),
                        resultSet.getString("satuan"),
                        resultSet.getInt("quantity_request"),
                        resultSet.getString("satuan"),
                        resultSet.getInt("quantity_realisasi"),
                        resultSet.getDouble("harga_satuan"),
                        resultSet.getDouble("n_subtotal")),
                poCode);

        String supplier;
        try {
            supplier = jdbcTemplate.queryForObject(
                    "select coalesce(v.v_vendor_name, '-') from tb_purchase_order o "
                            + "left join ms_vendor v on v.n_vendor_id = o.n_vendor_id "
                            + "where o.v_po_code = ?",
                    String.class, poCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }

        return new PurchaseOrderPrintData(poCode, supplier, normalize(username), lines);
    }

    private List<PurchaseOrderOppDetailResponse.Line> loadOppLines(Integer prId) {
        String sql = "select d.n_item_id, it.v_item_code, it.v_item_name, "
                + "coalesce(m.v_mitem_early_quantify, '-') as v_mitem_early_quantify, "
                + "coalesce(m.v_mitem_end_quantify, '-') as v_mitem_end_quantify, "
                + "d.n_mitem_id, d.n_pr_det_qty_requested "
                + "from tb_purchase_request_detail d "
                + "join ms_item it on it.n_item_id = d.n_item_id "
                + "left join ms_item_measurement m on m.n_mitem_id = d.n_mitem_id "
                + "where d.n_pr_id = ? order by d.n_pr_det_id";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            Integer itemId = resultSet.getInt("n_item_id");
            Integer qtyRequested = resultSet.getInt("n_pr_det_qty_requested");
            Integer qtyArrived = getQtyOrderArrived(prId, itemId);
            return new PurchaseOrderOppDetailResponse.Line(
                    itemId,
                    resultSet.getString("v_item_code"),
                    resultSet.getString("v_item_name"),
                    qtyRequested,
                    resultSet.getString("v_mitem_early_quantify"),
                    resultSet.getString("v_mitem_end_quantify"),
                    toInteger(resultSet.getObject("n_mitem_id")),
                    qtyRequested - qtyArrived,
                    getLastPrice(itemId, null));
        }, prId);
    }

    private List<PurchaseOrderPoDetailResponse.Line> loadPoLines(Integer poId) {
        String sql = "select d.n_item_id, it.v_item_code, it.v_item_name, "
                + "coalesce(m.v_mitem_early_quantify, '-') as v_mitem_early_quantify, "
                + "d.n_measurement_id, d.n_po_det_cost, d.n_po_det_qty_ordered, "
                + "coalesce(d.n_bonus, 0) as n_bonus, coalesce(d.n_discount, 0) as n_discount, "
                + "coalesce(d.v_discount_type, 'RP') as v_discount_type, d.n_subtotal "
                + "from tb_purchase_order_detail d "
                + "join ms_item it on it.n_item_id = d.n_item_id "
                + "left join ms_item_measurement m on m.n_mitem_id = d.n_measurement_id "
                + "where d.n_po_id = ? order by d.n_po_det_id";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new PurchaseOrderPoDetailResponse.Line(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_mitem_early_quantify"),
                toInteger(resultSet.getObject("n_measurement_id")),
                resultSet.getDouble("n_po_det_cost"),
                resultSet.getInt("n_po_det_qty_ordered"),
                resultSet.getInt("n_bonus"),
                resultSet.getDouble("n_discount"),
                resultSet.getString("v_discount_type"),
                resultSet.getDouble("n_subtotal")),
                poId);
    }

    private void insertDetail(Integer poId, PurchaseOrderSaveRequest.Line line, String username,
            Timestamp now) {
        Integer detailId = nextVal("tb_purchase_order_detail_n_po_det_id_seq");
        jdbcTemplate.update(
                "insert into tb_purchase_order_detail (n_po_det_id, n_po_id, n_item_id, "
                        + "n_po_det_qty_ordered, n_po_det_qty_received, n_po_det_qty_remark, "
                        + "n_po_det_cost, n_measurement_id, n_bonus, n_discount, v_discount_type, "
                        + "n_subtotal, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                detailId, poId, line.getItemId(),
                shortValue(line.getQtyOrdered()), (short) 0, (short) 0,
                valueOrZero(line.getCost()), line.getMeasurementId(),
                shortValue(line.getBonus()), valueOrZero(line.getDiscount()),
                normalize(line.getDiscountType()), itemSubtotal(line), normalize(username), now);
    }

    /**
     * Perhitungan SUBTOTAL per item. Migrasi dari legacy
     * {@code CommonGlobalDiscountListener.onEvent()}:
     * <ul>
     * <li>RP: harga × qty − diskon</li>
     * <li>%: harga × qty − ((diskon/100) × harga) × qty</li>
     * </ul>
     */
    private double itemSubtotal(PurchaseOrderSaveRequest.Line line) {
        double initPrice = valueOrZero(line.getCost());
        double qty = line.getQtyOrdered() == null ? 0 : line.getQtyOrdered();
        double disc = valueOrZero(line.getDiscount());
        if (DISCOUNT_TYPE_RP.equals(normalize(line.getDiscountType()))) {
            return initPrice * qty - disc;
        }
        return initPrice * qty - ((disc / 100) * initPrice) * qty;
    }

    /**
     * Jumlah yang sudah diorder untuk item pada OPP (status OPEN/APPROVED).
     * Migrasi dari legacy {@code TbPurchaseOrderDAO.getQtyOrderArrived()}.
     */
    private Integer getQtyOrderArrived(Integer prId, Integer itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coalesce(sum(pod.n_po_det_qty_ordered), 0) "
                            + "from tb_purchase_order_detail pod "
                            + "join tb_purchase_order po on po.n_po_id = pod.n_po_id "
                            + "where po.n_pr_id = ? and pod.n_item_id = ? "
                            + "and (po.v_po_status = ? or po.v_po_status = ?)",
                    Integer.class, prId, itemId, STATUS_OPEN, STATUS_APPROVED);
        } catch (EmptyResultDataAccessException exception) {
            return 0;
        }
    }

    /**
     * Harga terakhir item. Migrasi dari legacy
     * {@code TbPurchaseOrderDAO.getLastPrice()}.
     */
    private Double getLastPrice(Integer itemId, Integer vendorId) {
        try {
            if (vendorId != null) {
                return jdbcTemplate.queryForObject(
                        "select n_po_det_cost from tb_purchase_order_detail det "
                                + "join tb_purchase_order po on po.n_po_id = det.n_po_id "
                                + "where det.n_item_id = ? and po.n_vendor_id = ? "
                                + "order by det.d_whn_create desc limit 1",
                        Double.class, itemId, vendorId);
            }
            return jdbcTemplate.queryForObject(
                    "select n_po_det_cost from tb_purchase_order_detail "
                            + "where n_item_id = ? order by d_whn_create desc limit 1",
                    Double.class, itemId);
        } catch (EmptyResultDataAccessException exception) {
            return 0.0;
        }
    }

    private String generatePoCode(String unitCode) {
        Integer seq = nextVal("sq_purchase_order_code");
        Date date = new Date();
        String yyMM = new SimpleDateFormat("yyMM").format(date);
        return "OP-" + normalize(unitCode) + "-" + yyMM + seq;
    }

    private Timestamp parseDueDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Timestamp.valueOf(value.trim() + " 00:00:00");
        } catch (IllegalArgumentException exception) {
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

    private Integer findStaffId(String username) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_staff_id from ms_user where upper(v_user_name) = ?",
                    Integer.class, normalize(username));
        } catch (EmptyResultDataAccessException exception) {
            return null;
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

    private Integer findPoId(String poCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_po_id from tb_purchase_order where v_po_code = ?",
                    Integer.class, poCode);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private Integer nextVal(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private short shortValue(Integer value) {
        if (value == null) {
            return 0;
        }
        return value.shortValue();
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
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

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String toIsoDate(Timestamp value) {
        if (value == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(value);
    }

    private String toDisplayDate(Timestamp value) {
        if (value == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy").format(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String like(String value) {
        return "%" + normalize(value) + "%";
    }
}
