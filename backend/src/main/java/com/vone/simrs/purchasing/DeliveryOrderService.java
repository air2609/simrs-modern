package com.vone.simrs.purchasing;

import com.vone.simrs.accounting.JournalService;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0195 (BUKTI PENERIMAAN BARANG / BPP) + tab SC0195B
 * (INPUT BATCH NO.).
 *
 * <p>
 * Migrasi dari legacy {@code DOController} + {@code DOBatchController} +
 * {@code DOManagerImpl} + {@code TbDeliveryOrderDAO}:
 * <ul>
 * <li>{@code DOController.init()} → {@link #getMasters(String)}</li>
 * <li>{@code DOManagerImpl.doSearch()} → {@link #searchPo(String, String)}</li>
 * <li>{@code DOManagerImpl.redraw(DOController, ...)} → {@link #getPoDetail(String)}</li>
 * <li>{@code DOManagerImpl.doSearchDO()} → {@link #searchDo(String, String)}</li>
 * <li>{@code DOManagerImpl.redrawExistingDO()} → {@link #getDoDetail(String)}</li>
 * <li>{@code DOManagerImpl.doSaveAdd(...)} → {@link #save(DeliveryOrderSaveRequest, String)}</li>
 * <li>{@code DOManagerImpl.update(...)} → {@link #update(DeliveryOrderUpdateRequest, String)}</li>
 * <li>{@code DOController.doRevoke()} → {@link #revoke(String, String)}</li>
 * <li>{@code DOManagerImpl.redraw(DOBatchController)} → {@link #getBatchMasters(String)}</li>
 * <li>{@code DOBatchController.redrawStatus()} → {@link #getMeasurementOptions(String)}</li>
 * <li>{@code TbDeliveryOrderDAO.getBatchItemByBatchCode()} → {@link #isBatchDuplicate(String)}</li>
 * <li>{@code TbDeliveryOrderDAO.executeApproval()} → {@link #approve(DeliveryOrderApproveRequest, String)}</li>
 * </ul>
 */
@Service
public class DeliveryOrderService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_BACKORDER = "BACK-ORDER";
    private static final String DISCOUNT_TYPE_RP = "RP";
    private static final String GIM_KEY_AP = "COA_AP";
    private static final String PROFIT_NORMAL = "32.5";
    private static final String PROFIT_BPJS = "20";
    private static final int ITEM_TYPE_BPJS = 5;

    private final JdbcTemplate jdbcTemplate;
    private final JournalService journalService;

    public DeliveryOrderService(JdbcTemplate jdbcTemplate, JournalService journalService) {
        this.jdbcTemplate = jdbcTemplate;
        this.journalService = journalService;
    }

    /**
     * Data master: gudang untuk staff login + nama user (DITERIMA OLEH).
     * Migrasi dari legacy {@code WarehouseManager.getWhouseByStaffId()}.
     */
    public DeliveryOrderMastersResponse getMasters(String username) {
        Integer staffId = findStaffId(username);
        List<DeliveryOrderMastersResponse.Warehouse> warehouses = new ArrayList<DeliveryOrderMastersResponse.Warehouse>();
        if (staffId != null) {
            warehouses = jdbcTemplate.query(
                    "select distinct wh.n_whouse_id, wh.v_whouse_code, wh.v_whouse_name "
                            + "from ms_staff_in_unit siu "
                            + "join ms_unit unit on unit.n_unit_id = siu.n_unit_id "
                            + "join ms_warehouse wh on wh.n_whouse_id = unit.n_whouse_id "
                            + "where siu.n_staff_id = ? order by wh.v_whouse_name",
                    (resultSet, rowNum) -> new DeliveryOrderMastersResponse.Warehouse(
                            resultSet.getInt("n_whouse_id"),
                            resultSet.getString("v_whouse_code"),
                            resultSet.getString("v_whouse_name")),
                    staffId);
        }
        return new DeliveryOrderMastersResponse(warehouses, username);
    }

    /**
     * Pencarian OP (status APPROVED / BACK-ORDER) untuk bandbox NO. OP.
     * Migrasi dari legacy {@code POManagerImpl.doSearchPOR()}? Tidak —
     * {@code DOManagerImpl.doSearch()} + {@code searchPOActiveByCodeSup()}.
     */
    public List<PurchaseOrderPoOptionResponse> searchPo(String poCode, String supName) {
        return jdbcTemplate.query(
                "select po.v_po_code, coalesce(v.v_vendor_name, '-') as v_vendor_name, "
                        + "po.d_whn_create "
                        + "from tb_purchase_order po "
                        + "left join ms_vendor v on v.n_vendor_id = po.n_vendor_id "
                        + "where upper(po.v_po_code) like ? and upper(coalesce(v.v_vendor_name, '')) like ? "
                        + "and (po.v_po_status = ? or po.v_po_status = ?) "
                        + "order by po.d_whn_create desc limit 100",
                (resultSet, rowNum) -> new PurchaseOrderPoOptionResponse(
                        resultSet.getString("v_po_code"),
                        resultSet.getString("v_vendor_name"),
                        toDisplayDate(resultSet.getTimestamp("d_whn_create"))),
                like(poCode), like(supName), STATUS_APPROVED, STATUS_BACKORDER);
    }

    /**
     * Header + detail OP untuk dijadikan BPP. Migrasi dari legacy
     * {@code DOManagerImpl.redraw(DOController, ...)}.
     */
    public DeliveryOrderPoDetailResponse getPoDetail(String poCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select po.n_po_id, po.v_po_code, po.v_po_status, po.n_vendor_id, "
                            + "coalesce(v.v_vendor_code, '-') as v_vendor_code, "
                            + "coalesce(v.v_vendor_name, '-') as v_vendor_name, "
                            + "coalesce(v.v_vendor_address, '') as v_vendor_address, "
                            + "coalesce(v.v_vendor_contact_no, '') as v_vendor_contact_no, "
                            + "coalesce(po.n_discount, 0) as n_discount, "
                            + "coalesce(po.v_discount_type, 'RP') as v_discount_type "
                            + "from tb_purchase_order po "
                            + "left join ms_vendor v on v.n_vendor_id = po.n_vendor_id "
                            + "where po.v_po_code = ?",
                    (resultSet, rowNum) -> new DeliveryOrderPoDetailResponse(
                            resultSet.getString("v_po_code"),
                            resultSet.getString("v_po_status"),
                            toInteger(resultSet.getObject("n_vendor_id")),
                            resultSet.getString("v_vendor_code"),
                            resultSet.getString("v_vendor_name"),
                            resultSet.getString("v_vendor_address"),
                            resultSet.getString("v_vendor_contact_no"),
                            resultSet.getDouble("n_discount"),
                            resultSet.getString("v_discount_type"),
                            loadPoLines(resultSet.getInt("n_po_id"))),
                    poCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }
    }

    /**
     * Pencarian BPP (status OPEN) untuk bandbox NO. BPP. Migrasi dari legacy
     * {@code DOManagerImpl.doSearchDO()}.
     */
    public List<DeliveryOrderDoOptionResponse> searchDo(String doCode, String whouseCode) {
        return jdbcTemplate.query(
                "select d.v_do_code, coalesce(wh.v_whouse_name, '-') as v_whouse_name, d.d_whn_create "
                        + "from tb_delivery_order d "
                        + "left join ms_warehouse wh on wh.n_whouse_id = d.n_whouse_id "
                        + "where upper(d.v_do_code) like ? and upper(coalesce(wh.v_whouse_code, '')) like ? "
                        + "and d.v_do_status = ? "
                        + "order by d.d_whn_create desc limit 100",
                (resultSet, rowNum) -> new DeliveryOrderDoOptionResponse(
                        resultSet.getString("v_do_code"),
                        resultSet.getString("v_whouse_name"),
                        toDisplayDate(resultSet.getTimestamp("d_whn_create"))),
                like(doCode), like(whouseCode), STATUS_OPEN);
    }

    /**
     * Header + detail BPP yang sudah ada. Migrasi dari legacy
     * {@code DOManagerImpl.redrawExistingDO()}.
     */
    public DeliveryOrderDoDetailResponse getDoDetail(String doCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select d.n_do_id, d.v_do_code, d.v_do_status, "
                            + "coalesce(po.v_po_code, '') as v_po_code, "
                            + "d.n_whouse_id, coalesce(wh.v_whouse_code, '') as v_whouse_code, "
                            + "coalesce(wh.v_whouse_name, '') as v_whouse_name, "
                            + "d.d_rec_date, coalesce(d.v_who_create, '') as v_who_create, "
                            + "case when d.n_approver_id is null then '' "
                            + "  else coalesce(appr.v_staff_code, '') || '-' || coalesce(appr.v_staff_name, '') end as approver_name, "
                            + "coalesce(v.v_vendor_code, '-') as v_vendor_code, "
                            + "coalesce(v.v_vendor_name, '-') as v_vendor_name, "
                            + "coalesce(v.v_vendor_address, '') as v_vendor_address, "
                            + "coalesce(v.v_vendor_contact_no, '') as v_vendor_contact_no, "
                            + "coalesce(po.n_discount, 0) as n_discount, "
                            + "coalesce(po.v_discount_type, 'RP') as v_discount_type, "
                            + "coalesce(d.n_do_tax, 0) as n_do_tax, "
                            + "coalesce(d.v_tax_type, '%') as v_tax_type, "
                            + "coalesce(d.n_total, 0) as n_total, "
                            + "coalesce(d.n_total_after_disc, 0) as n_total_after_disc, "
                            + "coalesce(d.n_total_after_ppn, 0) as n_total_after_ppn "
                            + "from tb_delivery_order d "
                            + "left join ms_warehouse wh on wh.n_whouse_id = d.n_whouse_id "
                            + "left join tb_purchase_order po on po.n_po_id = d.n_po_id "
                            + "left join ms_vendor v on v.n_vendor_id = po.n_vendor_id "
                            + "left join ms_staff appr on appr.n_staff_id = d.n_approver_id "
                            + "where d.v_do_code = ?",
                    (resultSet, rowNum) -> new DeliveryOrderDoDetailResponse(
                            resultSet.getString("v_do_code"),
                            resultSet.getString("v_do_status"),
                            resultSet.getString("v_po_code"),
                            toInteger(resultSet.getObject("n_whouse_id")),
                            resultSet.getString("v_whouse_code"),
                            resultSet.getString("v_whouse_name"),
                            toIsoDate(resultSet.getTimestamp("d_rec_date")),
                            resultSet.getString("v_who_create"),
                            resultSet.getString("approver_name"),
                            resultSet.getString("v_vendor_code"),
                            resultSet.getString("v_vendor_name"),
                            resultSet.getString("v_vendor_address"),
                            resultSet.getString("v_vendor_contact_no"),
                            resultSet.getDouble("n_discount"),
                            resultSet.getString("v_discount_type"),
                            resultSet.getDouble("n_do_tax"),
                            resultSet.getString("v_tax_type"),
                            resultSet.getDouble("n_total"),
                            resultSet.getDouble("n_total_after_disc"),
                            resultSet.getDouble("n_total_after_ppn"),
                            loadDoLines(resultSet.getInt("n_do_id"))),
                    doCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }
    }

    /**
     * Simpan BPP baru. Migrasi dari legacy
     * {@code DOManagerImpl.doSaveAdd(DOController, TbPurchaseOrder, TbDeliveryOrder)}.
     */
    @Transactional
    public DeliveryOrderResultResponse save(DeliveryOrderSaveRequest request, String username) {
        if (request.getDoCode() == null || request.getDoCode().trim().isEmpty()) {
            throw new IllegalArgumentException("NO. BPP WAJIB DIISI.");
        }
        if (request.getPoCode() == null || request.getPoCode().trim().isEmpty()) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }
        if (request.getWarehouseId() == null) {
            throw new IllegalArgumentException("LOKASI GUDANG WAJIB DIISI.");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("LIST ITEM KOSONG.");
        }
        if (findDoId(request.getDoCode().trim()) != null) {
            throw new IllegalArgumentException("BPP TIDAK VALID! SUDAH TERDAPAT NOMOR YANG SAMA DI DATABASE!");
        }

        Integer staffId = findStaffId(username);
        if (staffId == null) {
            throw new IllegalArgumentException("STAFF USER TIDAK DITEMUKAN.");
        }
        Integer poId = findPoId(request.getPoCode().trim());
        if (poId == null) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }

        Integer doId = nextVal("tb_delivery_order_n_do_id_seq");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "insert into tb_delivery_order (n_do_id, n_issuer_id, n_po_id, n_whouse_id, "
                        + "v_do_code, v_do_status, n_do_tax, v_tax_type, d_rec_date, n_total, "
                        + "n_total_after_disc, n_total_after_ppn, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                doId, staffId, poId, request.getWarehouseId(), request.getDoCode().trim(),
                STATUS_OPEN, valueOrZero(request.getPpn()), normalize(request.getPpnType()),
                parseDate(request.getRecDate()), valueOrZero(request.getTotal()),
                totalAfterDiscount(request.getTotal(), request.getDiscount(), request.getDiscountType()),
                valueOrZero(request.getGtotal()), normalize(username), now);

        for (DeliveryOrderSaveRequest.Line line : request.getLines()) {
            insertDetail(doId, line, username, now);
        }

        return new DeliveryOrderResultResponse(request.getDoCode().trim(), STATUS_OPEN);
    }

    /**
     * Ubah BPP yang masih OPEN. Migrasi dari legacy
     * {@code DOManagerImpl.update(...)} (hapus detail lama lalu simpan ulang).
     */
    @Transactional
    public void update(DeliveryOrderUpdateRequest request, String username) {
        if (request.getDoCode() == null || request.getDoCode().trim().isEmpty()) {
            throw new IllegalArgumentException("BPP TIDAK TERDAPAT DI DATABASE!");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("LIST ITEM KOSONG.");
        }
        Integer doId = findDoId(request.getDoCode().trim());
        if (doId == null) {
            throw new IllegalArgumentException("BPP TIDAK TERDAPAT DI DATABASE!");
        }
        String status = jdbcTemplate.queryForObject(
                "select v_do_status from tb_delivery_order where n_do_id = ?",
                String.class, doId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("STATUS BPP TIDAK VALID UNTUK DIUBAH!");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_delivery_order set n_whouse_id = ?, n_do_tax = ?, v_tax_type = ?, "
                        + "d_rec_date = ?, n_total = ?, n_total_after_disc = ?, n_total_after_ppn = ?, "
                        + "v_who_change = ?, d_whn_change = ? where n_do_id = ?",
                request.getWarehouseId(), valueOrZero(request.getPpn()),
                normalize(request.getPpnType()), parseDate(request.getRecDate()),
                valueOrZero(request.getTotal()), totalAfterDiscount(request.getTotal(),
                        request.getDiscount(), request.getDiscountType()),
                valueOrZero(request.getGtotal()), normalize(username), now, doId);

        jdbcTemplate.update("delete from tb_delivery_order_detail where n_do_id = ?", doId);
        for (DeliveryOrderSaveRequest.Line line : request.getLines()) {
            insertDetail(doId, line, username, now);
        }
    }

    /**
     * PEMBATALAN BPP: hapus BPP (status ditampilkan REVOKED). Migrasi dari
     * legacy {@code DOController.doRevoke()} + {@code delete(...)}.
     */
    @Transactional
    public void revoke(String doCode, String username) {
        if (doCode == null || doCode.trim().isEmpty()) {
            throw new IllegalArgumentException("BPP TIDAK TERDAPAT DI DATABASE!");
        }
        Integer doId = findDoId(doCode.trim());
        if (doId == null) {
            throw new IllegalArgumentException("BPP TIDAK TERDAPAT DI DATABASE!");
        }
        String status = jdbcTemplate.queryForObject(
                "select v_do_status from tb_delivery_order where n_do_id = ?",
                String.class, doId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("STATUS BPP TIDAK VALID UNTUK DIBATALKAN!");
        }
        jdbcTemplate.update("delete from tb_delivery_order where n_do_id = ?", doId);
    }

    /**
     * Daftar item BPP untuk tab INPUT BATCH NO. Migrasi dari legacy
     * {@code DOManagerImpl.redraw(DOBatchController)}.
     */
    public DeliveryOrderBatchMastersResponse getBatchMasters(String doCode) {
        Integer doId = findDoId(doCode);
        if (doId == null) {
            throw new IllegalArgumentException("BPP TIDAK TERDAPAT DI DATABASE!");
        }
        List<DeliveryOrderBatchMastersResponse.BatchItem> items = jdbcTemplate.query(
                "select dod.n_item_id, it.v_item_code, it.v_item_name, "
                        + "(dod.n_do_det_qty + coalesce(dod.n_do_bonus_qty, 0)) as init_qty, "
                        + "coalesce(m.v_mitem_end_quantify, '-') as init_m "
                        + "from tb_delivery_order_detail dod "
                        + "join ms_item it on it.n_item_id = dod.n_item_id "
                        + "join tb_purchase_order_detail pod on pod.n_po_det_id = dod.n_po_det_id "
                        + "left join ms_item_measurement m on m.n_mitem_id = pod.n_measurement_id "
                        + "where dod.n_do_id = ? order by dod.n_do_det_id",
                (resultSet, rowNum) -> new DeliveryOrderBatchMastersResponse.BatchItem(
                        resultSet.getInt("n_item_id"),
                        resultSet.getString("v_item_code"),
                        resultSet.getString("v_item_name"),
                        resultSet.getInt("init_qty"),
                        resultSet.getString("init_m")),
                doId);
        return new DeliveryOrderBatchMastersResponse(doCode, items);
    }

    /**
     * Opsi SATUAN AKHIR (konversi satuan + multiplier). Migrasi dari legacy
     * {@code getMsItemMeasurementListByCode()}.
     */
    public List<DeliveryOrderMeasurementOptionResponse> getMeasurementOptions(String code) {
        return jdbcTemplate.query(
                "select v_mitem_end_quantify, n_mitem_end_qty from ms_item_measurement "
                        + "where v_mitem_early_quantify = ? order by n_mitem_end_qty",
                (resultSet, rowNum) -> new DeliveryOrderMeasurementOptionResponse(
                        resultSet.getString("v_mitem_end_quantify"),
                        resultSet.getInt("n_mitem_end_qty")),
                code);
    }

    /**
     * Cek duplikat NO. BATCH di database. Migrasi dari legacy
     * {@code TbDeliveryOrderDAO.getBatchItemByBatchCode()}.
     */
    public boolean isBatchDuplicate(String batchNo) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from tb_batch_item where v_batch_no = ?",
                    Integer.class, batchNo);
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException exception) {
            return false;
        }
    }

    /**
     * Validasi + eksekusi approval BPP. Migrasi dari legacy
     * {@code TbDeliveryOrderDAO.executeApproval()}: update status DO + detail,
     * jurnal (AP credit + inventory debit), hutang dagang, batch item +
     * inventory, update OP detail terima, status OP, dan harga jual item.
     */
    @Transactional
    public DeliveryOrderResultResponse approve(DeliveryOrderApproveRequest request, String username) {
        if (request.getDoCode() == null || request.getDoCode().trim().isEmpty()) {
            throw new IllegalArgumentException("BPP TIDAK TERDAPAT DI DATABASE!");
        }
        Integer doId = findDoId(request.getDoCode().trim());
        if (doId == null) {
            throw new IllegalArgumentException("BPP TIDAK TERDAPAT DI DATABASE!");
        }
        String status = jdbcTemplate.queryForObject(
                "select v_do_status from tb_delivery_order where n_do_id = ?",
                String.class, doId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("STATUS BPP TIDAK VALID UNTUK DIVALIDASI!");
        }

        // Data BPP + OP + vendor + gudang
        DoHeader header = loadDoHeader(doId);
        if (header == null) {
            throw new IllegalArgumentException("BPP TIDAK TERDAPAT DI DATABASE!");
        }
        List<DoLine> lines = loadDoLinesForApproval(doId);
        int totalItem = 0;
        if (DISCOUNT_TYPE_RP.equals(header.poDiscountType)) {
            for (DoLine line : lines) {
                totalItem += line.qtyArrived + line.bonusArrived;
            }
        }

        // Validasi kelengkapan batch per item
        Map<Integer, Integer> batchQtyByItem = new LinkedHashMap<Integer, Integer>();
        for (DeliveryOrderApproveRequest.BatchEntry entry : request.getEntries() == null
                ? new ArrayList<DeliveryOrderApproveRequest.BatchEntry>()
                : request.getEntries()) {
            Integer qty = entry.getQty() == null ? 0 : entry.getQty();
            Integer existing = batchQtyByItem.get(entry.getItemId());
            batchQtyByItem.put(entry.getItemId(), (existing == null ? 0 : existing) + qty);
        }
        for (DoLine line : lines) {
            Integer registered = batchQtyByItem.get(line.itemId);
            if (registered == null || registered.intValue() != line.qtyArrived + line.bonusArrived) {
                throw new IllegalArgumentException(
                        "DATA BATCH BELUM DIMASUKKAN SEMUA ATAU TIDAK SAMA DENGAN DATA ORDER/BONUS DITERIMA. MOHON DIPERIKSA LAGI!");
            }
        }

        Integer staffId = findStaffId(username);
        if (staffId == null) {
            throw new IllegalArgumentException("STAFF USER TIDAK DITEMUKAN.");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // 1. Status BPP APPROVED + approver
        jdbcTemplate.update(
                "update tb_delivery_order set v_do_status = ?, n_approver_id = ?, "
                        + "v_who_change = ?, d_whn_change = ? where n_do_id = ?",
                STATUS_APPROVED, staffId, normalize(username), now, doId);

        String memoAp = "VENDOR:" + header.vendorCode + "TOTAL INVOICE:" + header.totalAfterPpn;
        double apAmount = header.totalAfterPpn;

        // 2. Jurnal: AP credit + inventory debit + update detail (v_end_m, n_multiple_m)
        String batchId = journalService.buildJournalBatchId();
        Integer apCoaId = header.vendorCoaId != null ? header.vendorCoaId
                : journalService.findCoaIdByGimKey(GIM_KEY_AP);
        if (apCoaId == null) {
            throw new IllegalArgumentException("COA AP BELUM DIATUR (GIM " + GIM_KEY_AP + ").");
        }
        journalService.insertJournalEntry(batchId, header.doCode, memoAp, 0, apAmount, now,
                normalize(username), apCoaId);

        double totalDebit = 0;
        for (DoLine line : lines) {
            Integer warehouseCoaId = header.warehouseCoaId;
            if (warehouseCoaId == null) {
                throw new IllegalArgumentException("COA GUDANG BELUM DIATUR (n_coa_id ms_warehouse).");
            }
            double subtotal = calculatePrice(line.subtotal, header.poDiscount,
                    header.poDiscountType, header.tax, header.taxType,
                    line.qtyArrived + line.bonusArrived, totalItem);
            totalDebit += subtotal;
            String memo = "VENDOR:" + header.vendorCode + ";INV:" + line.itemCode
                    + ";QTY:" + line.qtyArrived;
            journalService.insertJournalEntry(batchId, header.doCode, memo, subtotal, 0, now,
                    normalize(username), warehouseCoaId);
        }
        if (Math.abs(totalDebit - apAmount) > 0.01) {
            throw new IllegalArgumentException("JURNAL TIDAK BALANCE (COA PERLU DIPERIKSA).");
        }

        // 3. Hutang dagang
        Integer apId = nextVal("tb_account_payable_n_ap_id_seq");
        jdbcTemplate.update(
                "insert into tb_account_payable (n_ap_id, n_vendor_id, n_total_remaining, "
                        + "d_due_date, v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?)",
                apId, header.vendorId, apAmount, header.poPaymentDue, normalize(username), now);

        // 4. Batch item + inventory + COGS + v_end_m/n_multiple_m
        Map<Integer, Double> cogsByItem = new LinkedHashMap<Integer, Double>();
        for (DeliveryOrderApproveRequest.BatchEntry entry : request.getEntries() == null
                ? new ArrayList<DeliveryOrderApproveRequest.BatchEntry>()
                : request.getEntries()) {
            DoLine line = findLine(lines, entry.getItemId());
            if (line == null) {
                continue;
            }
            int multiplier = entry.getMultiplier() == null ? 1 : entry.getMultiplier();
            int qtyTotal = line.qtyArrived + line.bonusArrived;
            double pricePerUnit = calculatePrice(line.subtotal, header.poDiscount,
                    header.poDiscountType, header.tax, header.taxType, qtyTotal, totalItem);
            double priceEach = qtyTotal == 0 ? 0 : pricePerUnit / (qtyTotal * multiplier);
            priceEach = round2(priceEach);

            jdbcTemplate.update(
                    "update tb_delivery_order_detail set v_end_m = ?, n_multiple_m = ?, "
                            + "v_who_change = ?, d_whn_change = ? where n_do_det_id = ?",
                    entry.getFinalM(), multiplier, normalize(username), now, line.doDetId);

            Integer batchIdPk = nextVal("tb_batch_item_n_batch_id_seq");
            jdbcTemplate.update(
                    "insert into tb_batch_item (n_batch_id, n_item_id, v_batch_no, "
                            + "d_batch_exp_date, n_batch_item_qty, v_who_create, d_whn_create, "
                            + "n_do_detail_id, n_cogs_price) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    batchIdPk, entry.getItemId(), entry.getBatchNo(), parseDate(entry.getExpDate()),
                    shortValue(entry.getQty()), normalize(username), now, line.doDetId, priceEach);

            Integer invId = nextVal("tb_item_inventory_n_item_inventory_id_seq");
            jdbcTemplate.update(
                    "insert into tb_item_inventory (n_item_inventory_id, n_whouse_id, n_batch_id, "
                            + "n_item_inv_qty, v_who_create, d_whn_create, n_item_id) "
                            + "values (?, ?, ?, ?, ?, ?, ?)",
                    invId, header.warehouseId, batchIdPk,
                    entry.getQty() == null ? 0 : entry.getQty() * multiplier,
                    normalize(username), now, entry.getItemId());

            cogsByItem.put(entry.getItemId(), priceEach);
        }

        // 5. Update OP detail: qty diterima + bonus diterima
        for (DoLine line : lines) {
            jdbcTemplate.update(
                    "update tb_purchase_order_detail set n_po_det_qty_received = "
                            + "coalesce(n_po_det_qty_received, 0) + ?, "
                            + "n_bonus_recieved = coalesce(n_bonus_recieved, 0) + ?, "
                            + "v_who_change = ?, d_whn_change = ? where n_po_det_id = ?",
                    shortValue(line.qtyArrived), shortValue(line.bonusArrived),
                    normalize(username), now, line.poDetId);
        }

        // 6. Status OP: CLOSED bila semua terima, selain itu BACK-ORDER
        updatePoStatus(header.poId, username, now);

        // 7. Harga jual ulang (COGS × persentase laba per kelas tarif)
        updateSellingPrices(cogsByItem, username, now);

        return new DeliveryOrderResultResponse(header.doCode, STATUS_APPROVED);
    }

    // ===== helpers =====

    private List<DeliveryOrderPoDetailResponse.Line> loadPoLines(Integer poId) {
        String sql = "select d.n_po_det_id, d.n_item_id, it.v_item_code, it.v_item_name, "
                + "coalesce(m.v_mitem_end_quantify, '-') as satuan, "
                + "d.n_po_det_cost, d.n_po_det_qty_ordered, coalesce(d.n_bonus, 0) as n_bonus, "
                + "coalesce(d.n_po_det_qty_received, 0) as qty_received, "
                + "coalesce(d.n_bonus_recieved, 0) as bonus_received, "
                + "coalesce(d.n_discount, 0) as n_discount, "
                + "coalesce(d.v_discount_type, 'RP') as v_discount_type "
                + "from tb_purchase_order_detail d "
                + "join ms_item it on it.n_item_id = d.n_item_id "
                + "left join ms_item_measurement m on m.n_mitem_id = d.n_measurement_id "
                + "where d.n_po_id = ? order by d.n_po_det_id";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            int qtyOrdered = resultSet.getInt("n_po_det_qty_ordered");
            int bonus = resultSet.getInt("n_bonus");
            return new DeliveryOrderPoDetailResponse.Line(
                    resultSet.getInt("n_po_det_id"),
                    resultSet.getInt("n_item_id"),
                    resultSet.getString("v_item_code"),
                    resultSet.getString("v_item_name"),
                    qtyOrdered,
                    bonus,
                    resultSet.getString("satuan"),
                    resultSet.getDouble("n_po_det_cost"),
                    qtyOrdered - resultSet.getInt("qty_received"),
                    bonus - resultSet.getInt("bonus_received"),
                    resultSet.getDouble("n_discount"),
                    resultSet.getString("v_discount_type"));
        }, poId);
    }

    private List<DeliveryOrderDoDetailResponse.Line> loadDoLines(Integer doId) {
        String sql = "select dod.n_do_det_id, dod.n_po_det_id, dod.n_item_id, "
                + "it.v_item_code, it.v_item_name, coalesce(m.v_mitem_end_quantify, '-') as satuan, "
                + "coalesce(pod.n_po_det_cost, 0) as cost, "
                + "coalesce(pod.n_po_det_qty_ordered, 0) as qty_ordered, "
                + "coalesce(pod.n_bonus, 0) as n_bonus, "
                + "coalesce(pod.n_po_det_qty_received, 0) as qty_received, "
                + "coalesce(pod.n_bonus_recieved, 0) as bonus_received, "
                + "coalesce(dod.n_do_det_qty, 0) as do_qty, "
                + "coalesce(dod.n_do_bonus_qty, 0) as do_bonus, "
                + "coalesce(dod.n_subtotal, 0) as subtotal "
                + "from tb_delivery_order_detail dod "
                + "join ms_item it on it.n_item_id = dod.n_item_id "
                + "join tb_purchase_order_detail pod on pod.n_po_det_id = dod.n_po_det_id "
                + "left join ms_item_measurement m on m.n_mitem_id = pod.n_measurement_id "
                + "where dod.n_do_id = ? order by dod.n_do_det_id";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new DeliveryOrderDoDetailResponse.Line(
                resultSet.getInt("n_do_det_id"),
                resultSet.getInt("n_po_det_id"),
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getInt("qty_ordered"),
                resultSet.getInt("n_bonus"),
                resultSet.getString("satuan"),
                resultSet.getDouble("cost"),
                resultSet.getInt("qty_ordered") - resultSet.getInt("qty_received"),
                resultSet.getInt("n_bonus") - resultSet.getInt("bonus_received"),
                resultSet.getInt("do_qty"),
                resultSet.getInt("do_bonus"),
                resultSet.getDouble("subtotal")),
                doId);
    }

    private void insertDetail(Integer doId, DeliveryOrderSaveRequest.Line line, String username,
            Timestamp now) {
        Integer detailId = nextVal("tb_delivery_order_detail_n_do_det_id_seq");
        jdbcTemplate.update(
                "insert into tb_delivery_order_detail (n_do_det_id, n_do_id, n_item_id, "
                        + "n_do_det_qty, v_who_create, d_whn_create, n_po_det_id, n_subtotal, "
                        + "n_do_bonus_qty) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                detailId, doId, line.getItemId(), shortValue(line.getQtyArrived()),
                normalize(username), now, line.getPoDetId(), valueOrZero(line.getSubtotal()),
                shortValue(line.getBonusArrived()));
    }

    private double totalAfterDiscount(double total, Double discount, String discountType) {
        double disc = valueOrZero(discount);
        if (DISCOUNT_TYPE_RP.equals(normalize(discountType))) {
            return total - disc;
        }
        return total - (total * disc / 100);
    }

    /**
     * Perhitungan harga dengan diskon + pajak. Migrasi dari legacy
     * {@code PurchaseController.calculatePrice()}.
     */
    private double calculatePrice(double initPrice, double discount, String discountType,
            double tax, String taxType, int qtyLocal, int qtyGlobal) {
        double result = initPrice;
        double disc = 0;
        double ppn = 0;
        if (discount > 0) {
            if (DISCOUNT_TYPE_RP.equals(normalize(discountType))) {
                disc = qtyGlobal == 0 ? 0 : (double) qtyLocal / qtyGlobal * discount;
            } else {
                disc = initPrice * discount / 100;
            }
            result -= disc;
        }
        if (tax > 0) {
            if (DISCOUNT_TYPE_RP.equals(normalize(taxType))) {
                ppn = tax;
            } else {
                ppn = result * tax / 100;
            }
            result += ppn;
        }
        return result;
    }

    private void updatePoStatus(Integer poId, String username, Timestamp now) {
        List<Boolean> rows = jdbcTemplate.query(
                "select n_po_det_qty_ordered - coalesce(n_po_det_qty_received, 0) as rem_qty, "
                        + "coalesce(n_bonus, 0) - coalesce(n_bonus_recieved, 0) as rem_bonus "
                        + "from tb_purchase_order_detail where n_po_id = ?",
                (resultSet, rowNum) -> resultSet.getInt("rem_qty") == 0
                        && resultSet.getInt("rem_bonus") == 0,
                poId);
        boolean allZero = true;
        for (Boolean zero : rows) {
            if (!zero) {
                allZero = false;
                break;
            }
        }
        String poStatus = allZero ? STATUS_CLOSED : STATUS_BACKORDER;
        jdbcTemplate.update(
                "update tb_purchase_order set v_po_status = ?, v_who_change = ?, d_whn_change = ? "
                        + "where n_po_id = ?",
                poStatus, normalize(username), now, poId);
    }

    /**
     * Regenerasi harga jual item (COGS × persen laba per kelas tarif). Migrasi
     * dari bagian akhir legacy {@code TbDeliveryOrderDAO.executeApproval()}.
     */
    private void updateSellingPrices(Map<Integer, Double> cogsByItem, String username, Timestamp now) {
        for (Map.Entry<Integer, Double> entry : cogsByItem.entrySet()) {
            Integer itemId = entry.getKey();
            double cogs = entry.getValue();
            double margin = profitMargin(itemId);

            jdbcTemplate.update("delete from ms_item_selling_price where n_item_id = ?", itemId);

            List<Integer> treatmentClasses = jdbcTemplate.query(
                    "select n_tclass_id from ms_treatment_class",
                    (resultSet, rowNum) -> resultSet.getInt("n_tclass_id"));
            for (Integer tclassId : treatmentClasses) {
                Integer sellingId = nextVal("ms_item_selling_price_n_item_selling_price_id_seq");
                double sellingPrice = round2(cogs + (margin / 100) * cogs);
                jdbcTemplate.update(
                        "insert into ms_item_selling_price (n_item_selling_price_id, n_item_id, "
                                + "n_tclass_id, n_selling_price, v_who_create, d_whn_create) "
                                + "values (?, ?, ?, ?, ?, ?)",
                        sellingId, itemId, tclassId, sellingPrice, normalize(username), now);
            }

            jdbcTemplate.update(
                    "update tb_batch_item set n_cogs_price = ?, v_who_change = ?, d_whn_change = ? "
                            + "where n_item_id = ?",
                    cogs, normalize(username), now, itemId);
        }
    }

    private double profitMargin(Integer itemId) {
        try {
            Integer itemType = jdbcTemplate.queryForObject(
                    "select n_type from ms_item where n_item_id = ?", Integer.class, itemId);
            if (itemType != null && itemType.intValue() == ITEM_TYPE_BPJS) {
                return Double.parseDouble(PROFIT_BPJS);
            }
        } catch (EmptyResultDataAccessException | NumberFormatException ignored) {
            // fallthrough
        }
        return Double.parseDouble(PROFIT_NORMAL);
    }

    private DoHeader loadDoHeader(Integer doId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select d.n_do_id, d.v_do_code, d.n_whouse_id, d.n_po_id, "
                            + "d.n_total_after_ppn, coalesce(d.n_do_tax, 0) as n_do_tax, "
                            + "coalesce(d.v_tax_type, '%') as v_tax_type, "
                            + "coalesce(po.n_discount, 0) as n_discount, "
                            + "coalesce(po.v_discount_type, 'RP') as v_discount_type, "
                            + "po.n_vendor_id, coalesce(v.v_vendor_code, '') as v_vendor_code, "
                            + "v.n_coa_id as vendor_coa_id, "
                            + "wh.n_coa_id as whouse_coa_id, "
                            + "po.d_payment_due "
                            + "from tb_delivery_order d "
                            + "join tb_purchase_order po on po.n_po_id = d.n_po_id "
                            + "join ms_vendor v on v.n_vendor_id = po.n_vendor_id "
                            + "join ms_warehouse wh on wh.n_whouse_id = d.n_whouse_id "
                            + "where d.n_do_id = ?",
                    (resultSet, rowNum) -> new DoHeader(
                            resultSet.getString("v_do_code"),
                            resultSet.getInt("n_whouse_id"),
                            resultSet.getInt("n_po_id"),
                            resultSet.getDouble("n_total_after_ppn"),
                            resultSet.getDouble("n_do_tax"),
                            resultSet.getString("v_tax_type"),
                            resultSet.getDouble("n_discount"),
                            resultSet.getString("v_discount_type"),
                            resultSet.getInt("n_vendor_id"),
                            resultSet.getString("v_vendor_code"),
                            toInteger(resultSet.getObject("vendor_coa_id")),
                            toInteger(resultSet.getObject("whouse_coa_id")),
                            resultSet.getTimestamp("d_payment_due")),
                    doId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private List<DoLine> loadDoLinesForApproval(Integer doId) {
        String sql = "select dod.n_do_det_id, dod.n_po_det_id, dod.n_item_id, "
                + "it.v_item_code, coalesce(dod.n_do_det_qty, 0) as do_qty, "
                + "coalesce(dod.n_do_bonus_qty, 0) as do_bonus, "
                + "coalesce(dod.n_subtotal, 0) as subtotal "
                + "from tb_delivery_order_detail dod "
                + "join ms_item it on it.n_item_id = dod.n_item_id "
                + "where dod.n_do_id = ? order by dod.n_do_det_id";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new DoLine(
                resultSet.getInt("n_do_det_id"),
                resultSet.getInt("n_po_det_id"),
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getInt("do_qty"),
                resultSet.getInt("do_bonus"),
                resultSet.getDouble("subtotal")),
                doId);
    }

    private DoLine findLine(List<DoLine> lines, Integer itemId) {
        for (DoLine line : lines) {
            if (line.itemId.equals(itemId)) {
                return line;
            }
        }
        return null;
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

    private Integer findPoId(String poCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_po_id from tb_purchase_order where v_po_code = ?",
                    Integer.class, poCode);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private Integer findDoId(String doCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_do_id from tb_delivery_order where v_do_code = ?",
                    Integer.class, doCode);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private Integer nextVal(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private Timestamp parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Timestamp.valueOf(value.trim() + " 00:00:00");
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private short shortValue(Integer value) {
        return value == null ? 0 : value.shortValue();
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
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

    private String toIsoDate(Timestamp value) {
        return value == null ? null : new SimpleDateFormat("yyyy-MM-dd").format(value);
    }

    private String toDisplayDate(Timestamp value) {
        return value == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String like(String value) {
        return "%" + normalize(value) + "%";
    }

    private static class DoHeader {

        final String doCode;
        final Integer warehouseId;
        final Integer poId;
        final double totalAfterPpn;
        final double tax;
        final String taxType;
        final double poDiscount;
        final String poDiscountType;
        final Integer vendorId;
        final String vendorCode;
        final Integer vendorCoaId;
        final Integer warehouseCoaId;
        final Timestamp poPaymentDue;

        DoHeader(String doCode, Integer warehouseId, Integer poId, double totalAfterPpn,
                double tax, String taxType, double poDiscount, String poDiscountType,
                Integer vendorId, String vendorCode, Integer vendorCoaId, Integer warehouseCoaId,
                Timestamp poPaymentDue) {
            this.doCode = doCode;
            this.warehouseId = warehouseId;
            this.poId = poId;
            this.totalAfterPpn = totalAfterPpn;
            this.tax = tax;
            this.taxType = taxType;
            this.poDiscount = poDiscount;
            this.poDiscountType = poDiscountType;
            this.vendorId = vendorId;
            this.vendorCode = vendorCode;
            this.vendorCoaId = vendorCoaId;
            this.warehouseCoaId = warehouseCoaId;
            this.poPaymentDue = poPaymentDue;
        }
    }

    private static class DoLine {

        final Integer doDetId;
        final Integer poDetId;
        final Integer itemId;
        final String itemCode;
        final int qtyArrived;
        final int bonusArrived;
        final double subtotal;

        DoLine(Integer doDetId, Integer poDetId, Integer itemId, String itemCode, int qtyArrived,
                int bonusArrived, double subtotal) {
            this.doDetId = doDetId;
            this.poDetId = poDetId;
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.qtyArrived = qtyArrived;
            this.bonusArrived = bonusArrived;
            this.subtotal = subtotal;
        }
    }
}
