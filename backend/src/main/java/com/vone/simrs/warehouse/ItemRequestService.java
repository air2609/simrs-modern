package com.vone.simrs.warehouse;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0174 (PERMINTAAN O-BM / itemRequest.zul).
 *
 * <p>
 * Migrasi dari legacy {@code ItemRequestController} +
 * {@code ItemRequestApproveController} + {@code HistoryRequestController} +
 * {@code WarehouseManagerImpl} + {@code MsWarehouseDAO} + {@code ItemDAO}.
 */
@Service
public class ItemRequestService {

    private static final int REQUEST_NEW = 0;
    private static final int REQUEST_ALL_SENT = 2;
    private static final int MUTATION_PENDING = 1;
    private static final int MUTATION_APPROVED = 2;
    private static final DateTimeFormatter REQUEST_CODE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public ItemRequestService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ------------------------------------------------------------------ masters

    /**
     * Gudang sumber = gudang dari unit tempat user bertugas; gudang tujuan =
     * semua gudang. Migrasi dari {@code WarehouseManagerImpl.initItemRequest()}.
     */
    public ItemRequestMastersResponse getMasters(String username) {
        List<ItemRequestWarehouseResponse> sources = jdbcTemplate.query(
                "select distinct w.n_whouse_id, w.v_whouse_code, w.v_whouse_name "
                        + "from ms_staff_in_unit siu "
                        + "join ms_unit u on u.n_unit_id = siu.n_unit_id "
                        + "join ms_warehouse w on w.n_whouse_id = u.n_whouse_id "
                        + "where siu.n_staff_id = "
                        + "(select n_staff_id from ms_user where upper(v_user_name) = ?) "
                        + "order by w.v_whouse_name",
                (resultSet, rowNum) -> new ItemRequestWarehouseResponse(
                        resultSet.getInt("n_whouse_id"),
                        resultSet.getString("v_whouse_code"),
                        resultSet.getString("v_whouse_name")),
                normalizeUsername(username));
        List<ItemRequestWarehouseResponse> targets = jdbcTemplate.query(
                "select n_whouse_id, v_whouse_code, v_whouse_name from ms_warehouse "
                        + "order by v_whouse_name",
                (resultSet, rowNum) -> new ItemRequestWarehouseResponse(
                        resultSet.getInt("n_whouse_id"),
                        resultSet.getString("v_whouse_code"),
                        resultSet.getString("v_whouse_name")));
        return new ItemRequestMastersResponse(sources, targets);
    }

    /**
     * Cari item di gudang tujuan. Migrasi dari legacy
     * {@code ItemDAO.searchItemByWarehouese()} + {@code WarehouseDAO.getQtyAvail()}.
     */
    public List<ItemRequestItemResponse> searchItems(Integer warehouseId, String code, String name) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("PILIH GUDANG TUJUAN TERLEBIH DAHULU!");
        }
        if (!hasText(code) && !hasText(name)) {
            throw new IllegalArgumentException("Salah satu field (kode/nama) harus diisi!");
        }
        return jdbcTemplate.query(
                "select inv.n_item_id, item.v_item_code, item.v_item_name, "
                        + "sat.v_mitem_end_quantify, sum(inv.n_item_inv_qty) as stock "
                        + "from ms_item item "
                        + "join tb_item_inventory inv on inv.n_item_id = item.n_item_id "
                        + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                        + "where inv.n_whouse_id = ? and inv.n_item_inv_qty > 0 "
                        + "and upper(item.v_item_code) like ? and upper(item.v_item_name) like ? "
                        + "group by inv.n_item_id, item.v_item_code, item.v_item_name, "
                        + "sat.v_mitem_end_quantify "
                        + "order by inv.n_item_id",
                (resultSet, rowNum) -> new ItemRequestItemResponse(
                        resultSet.getInt("n_item_id"),
                        resultSet.getString("v_item_code"),
                        resultSet.getString("v_item_name"),
                        resultSet.getString("v_mitem_end_quantify"),
                        getNullableInteger(resultSet, "stock")),
                warehouseId,
                like(normalizeOptionalUpper(code)),
                like(normalizeOptionalUpper(name)));
    }

    // ------------------------------------------------------------------ kirim permintaan

    /**
     * Kirim permintaan O-BM. Migrasi dari legacy
     * {@code ItemRequestController.kirimClick()} + {@code ItemDAO.saveItemRequest()}.
     */
    @Transactional
    public ItemRequestSaveResultResponse saveRequest(ItemRequestSaveRequest request, String username) {
        if (request.getSourceWarehouseId() == null || request.getTargetWarehouseId() == null) {
            throw new IllegalArgumentException("LOKASI TRANSAKSI / GUDANG TUJUAN HARUS DIISI!");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("ISILAH ITEM PERMINTAAN DULU!!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String requestCode = generateRequestCode(now);

        for (ItemRequestLineRequest line : request.getLines()) {
            if (line.getItemId() == null) {
                continue;
            }
            int qty = line.getQty() == null ? 0 : line.getQty();
            if (qty <= 0) {
                throw new IllegalArgumentException("PERMINTAAN TDK BOLEH NEGATIF/NOL!");
            }
            Integer stock = getQtyAvail(request.getTargetWarehouseId(), line.getItemId());
            if (stock == null || stock < qty) {
                throw new IllegalArgumentException("PERMINTAAN TDK BOLEH MELEBIHI STOK!");
            }
            jdbcTemplate.update(
                    "insert into tb_item_request (n_source_whouse_id, n_target_whouse_id, "
                            + "n_item_id, n_qty_req, n_qty_sent, n_status, v_who_create, "
                            + "d_whn_create, v_request_code) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    request.getSourceWarehouseId(), request.getTargetWarehouseId(),
                    line.getItemId(), qty, 0, REQUEST_NEW, username, now, requestCode);
        }
        return new ItemRequestSaveResultResponse(true, "PERMINTAAN TELAH DIKIRIM", requestCode);
    }

    private Integer getQtyAvail(Integer warehouseId, Integer itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coalesce(sum(n_item_inv_qty), 0) from tb_item_inventory "
                            + "where n_whouse_id = ? and n_item_id = ? and n_item_inv_qty > 0",
                    Integer.class, warehouseId, itemId);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ list permintaan (tab 2 kiri)

    /**
     * Daftar permintaan yang belum terkirim penuh (n_qty_sent < n_qty_req)
     * dengan gudang sumber = gudang user. Migrasi dari
     * {@code WarehouseDAO.getItemRequestBySource()} +
     * {@code WarehouseManagerImpl.getRequestItem()}.
     */
    public List<ItemRequestGroupResponse> getPendingRequests(Integer sourceWarehouseId) {
        if (sourceWarehouseId == null) {
            throw new IllegalArgumentException("LOKASI TRANSAKSI HARUS DIISI!");
        }
        List<RequestRow> rows = jdbcTemplate.query(
                "select req.n_ir_id, req.v_request_code, req.n_item_id, req.n_qty_req, "
                        + "req.n_qty_sent, req.n_status, req.d_whn_create, "
                        + "src.v_whouse_name as source_name, tgt.v_whouse_name as target_name, "
                        + "item.v_item_code, item.v_item_name, sat.v_mitem_end_quantify "
                        + "from tb_item_request req "
                        + "join ms_warehouse src on src.n_whouse_id = req.n_source_whouse_id "
                        + "join ms_warehouse tgt on tgt.n_whouse_id = req.n_target_whouse_id "
                        + "join ms_item item on item.n_item_id = req.n_item_id "
                        + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                        + "where req.n_source_whouse_id = ? and req.n_qty_sent < req.n_qty_req "
                        + "order by req.v_request_code, req.n_ir_id",
                (resultSet, rowNum) -> mapRequestRow(resultSet),
                sourceWarehouseId);
        return groupRequests(rows);
    }

    private RequestRow mapRequestRow(ResultSet resultSet) throws SQLException {
        int qtyReq = getNullableInteger(resultSet, "n_qty_req");
        int qtySent = getNullableInteger(resultSet, "n_qty_sent") == null ? 0
                : getNullableInteger(resultSet, "n_qty_sent");
        return new RequestRow(
                resultSet.getInt("n_ir_id"),
                resultSet.getString("v_request_code"),
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_mitem_end_quantify"),
                qtyReq, qtySent,
                getNullableInteger(resultSet, "n_status"),
                resultSet.getString("source_name"),
                resultSet.getString("target_name"),
                resultSet.getTimestamp("d_whn_create"));
    }

    private List<ItemRequestGroupResponse> groupRequests(List<RequestRow> rows) {
        Map<String, List<ItemRequestRowResponse>> grouped = new LinkedHashMap<>();
        Map<String, RequestRow> headerByCode = new LinkedHashMap<>();
        for (RequestRow row : rows) {
            grouped.computeIfAbsent(row.requestCode, key -> new ArrayList<>())
                    .add(new ItemRequestRowResponse(
                            row.irId, row.itemId, row.code, row.name, row.unit,
                            row.qtyReq, row.qtySent, row.qtyReq - row.qtySent));
            headerByCode.putIfAbsent(row.requestCode, row);
        }
        List<ItemRequestGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<String, List<ItemRequestRowResponse>> entry : grouped.entrySet()) {
            RequestRow header = headerByCode.get(entry.getKey());
            groups.add(new ItemRequestGroupResponse(
                    entry.getKey(),
                    header.sourceName, header.targetName,
                    header.status, requestStatusLabel(header.status),
                    toIsoDateTime(header.createdAt),
                    entry.getValue()));
        }
        return groups;
    }

    // ------------------------------------------------------------------ persetujuan (tab 2 kanan)

    /**
     * Item mutation yang menunggu persetujuan. Migrasi dari
     * {@code WarehouseDAO.getItemRequestApprove()} +
     * {@code WarehouseManagerImpl.getSentItem()}.
     */
    public List<ItemApprovalGroupResponse> getApprovals(Integer sourceWarehouseId) {
        if (sourceWarehouseId == null) {
            throw new IllegalArgumentException("LOKASI TRANSAKSI HARUS DIISI!");
        }
        List<ApprovalRow> rows = jdbcTemplate.query(
                "select mut.n_mitem_id, mut.n_ir_id, mut.n_batch_id, mut.n_mitem_qty, "
                        + "req.v_request_code, req.n_item_id, item.v_item_code, "
                        + "item.v_item_name, sat.v_mitem_end_quantify, "
                        + "src.v_whouse_name as source_name, tgt.v_whouse_name as target_name "
                        + "from tb_item_mutation mut "
                        + "join tb_item_request req on req.n_ir_id = mut.n_ir_id "
                        + "join ms_item item on item.n_item_id = req.n_item_id "
                        + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                        + "join ms_warehouse src on src.n_whouse_id = req.n_source_whouse_id "
                        + "join ms_warehouse tgt on tgt.n_whouse_id = req.n_target_whouse_id "
                        + "where req.n_source_whouse_id = ? and mut.v_status = ? "
                        + "order by req.v_request_code, mut.n_mitem_id",
                (resultSet, rowNum) -> new ApprovalRow(
                        resultSet.getInt("n_mitem_id"),
                        resultSet.getInt("n_ir_id"),
                        resultSet.getInt("n_batch_id"),
                        resultSet.getString("v_request_code"),
                        resultSet.getInt("n_item_id"),
                        resultSet.getString("v_item_code"),
                        resultSet.getString("v_item_name"),
                        resultSet.getString("v_mitem_end_quantify"),
                        getNullableInteger(resultSet, "n_mitem_qty"),
                        resultSet.getString("source_name"),
                        resultSet.getString("target_name")),
                sourceWarehouseId, MUTATION_PENDING);

        Map<String, List<ItemApprovalRowResponse>> grouped = new LinkedHashMap<>();
        Map<String, ApprovalRow> headerByCode = new LinkedHashMap<>();
        for (ApprovalRow row : rows) {
            grouped.computeIfAbsent(row.requestCode, key -> new ArrayList<>())
                    .add(new ItemApprovalRowResponse(
                            row.mutationId, row.irId, row.batchId, row.itemId,
                            row.code, row.name, row.unit, row.qty));
            headerByCode.putIfAbsent(row.requestCode, row);
        }
        List<ItemApprovalGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<String, List<ItemApprovalRowResponse>> entry : grouped.entrySet()) {
            ApprovalRow header = headerByCode.get(entry.getKey());
            groups.add(new ItemApprovalGroupResponse(
                    entry.getKey(), header.sourceName, header.targetName, entry.getValue()));
        }
        return groups;
    }

    /**
     * Setujui penerimaan O-BM: pindahkan stok dari gudang tujuan ke gudang
     * sumber. Migrasi dari {@code WarehouseDAO.saveItemApprove()}.
     */
    @Transactional
    public ItemRequestActionResultResponse approveMutations(List<Integer> mutationIds, String username) {
        if (mutationIds == null || mutationIds.isEmpty()) {
            throw new IllegalArgumentException("PILIH ITEM PERSETUJUAN TERLEBIH DAHULU!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (Integer mutationId : mutationIds) {
            ApproveContext ctx = findApproveContext(mutationId);
            if (ctx == null) {
                continue;
            }
            // stok di gudang tujuan dikurangi
            Integer targetInventoryId = findInventoryId(ctx.itemId, ctx.targetWhouseId, ctx.batchId);
            if (targetInventoryId == null) {
                throw new IllegalStateException("Stok gudang tujuan tidak ditemukan untuk item #"
                        + ctx.itemId);
            }
            jdbcTemplate.update(
                    "update tb_item_inventory set n_item_inv_qty = n_item_inv_qty - ?, "
                            + "d_whn_change = ?, v_who_change = ? where n_item_inventory_id = ?",
                    ctx.qty, now, username, targetInventoryId);

            // stok di gudang sumber ditambah (buat record baru bila belum ada)
            Integer sourceInventoryId = findInventoryId(ctx.itemId, ctx.sourceWhouseId, ctx.batchId);
            if (sourceInventoryId == null) {
                jdbcTemplate.update(
                        "insert into tb_item_inventory (n_whouse_id, n_batch_id, n_item_inv_qty, "
                                + "n_item_id, v_who_create, d_whn_create) "
                                + "values (?, ?, ?, ?, ?, ?)",
                        ctx.sourceWhouseId, ctx.batchId, ctx.qty, ctx.itemId, username, now);
            } else {
                jdbcTemplate.update(
                        "update tb_item_inventory set n_item_inv_qty = n_item_inv_qty + ?, "
                                + "d_whn_change = ?, v_who_change = ? where n_item_inventory_id = ?",
                        ctx.qty, now, username, sourceInventoryId);
            }

            // status mutation -> APPROVED
            jdbcTemplate.update(
                    "update tb_item_mutation set v_status = ?, v_who_change = ?, d_whn_change = ? "
                            + "where n_mitem_id = ?",
                    MUTATION_APPROVED, username, now, mutationId);

            // jika qty terkirim sudah penuh, status request -> ALL SENT
            jdbcTemplate.update(
                    "update tb_item_request set n_status = ? "
                            + "where n_ir_id = ? and n_qty_sent = n_qty_req",
                    REQUEST_ALL_SENT, ctx.irId);
        }
        return new ItemRequestActionResultResponse(true, "PERSETUJUAN BERHASIL DISIMPAN");
    }

    private ApproveContext findApproveContext(Integer mutationId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select mut.n_mitem_qty, mut.n_batch_id, req.n_ir_id, req.n_item_id, "
                            + "req.n_source_whouse_id, req.n_target_whouse_id "
                            + "from tb_item_mutation mut "
                            + "join tb_item_request req on req.n_ir_id = mut.n_ir_id "
                            + "where mut.n_mitem_id = ?",
                    (resultSet, rowNum) -> new ApproveContext(
                            mutationId,
                            resultSet.getInt("n_ir_id"),
                            resultSet.getInt("n_batch_id"),
                            resultSet.getInt("n_item_id"),
                            resultSet.getInt("n_source_whouse_id"),
                            resultSet.getInt("n_target_whouse_id"),
                            getNullableInteger(resultSet, "n_mitem_qty")),
                    mutationId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Integer findInventoryId(Integer itemId, Integer warehouseId, Integer batchId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_item_inventory_id from tb_item_inventory "
                            + "where n_item_id = ? and n_whouse_id = ? and n_batch_id = ?",
                    Integer.class, itemId, warehouseId, batchId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Batalkan item mutation yang belum disetujui. Migrasi dari
     * {@code WarehouseDAO.cancelItemApprove()}.
     */
    @Transactional
    public ItemRequestActionResultResponse cancelMutations(List<Integer> mutationIds) {
        if (mutationIds == null || mutationIds.isEmpty()) {
            throw new IllegalArgumentException("PILIH ITEM TERLEBIH DAHULU!");
        }
        for (Integer mutationId : mutationIds) {
            jdbcTemplate.update("delete from tb_item_mutation where n_mitem_id = ?", mutationId);
        }
        return new ItemRequestActionResultResponse(true, "PEMBATALAN BERHASIL");
    }

    /**
     * Batalkan permintaan O-BM. Migrasi dari {@code WarehouseDAO.cancelItemRequest()}
     * (menghapus baris tb_item_request; mutation terkait ikut terhapus via CASCADE).
     */
    @Transactional
    public ItemRequestActionResultResponse cancelRequests(List<Integer> irIds) {
        if (irIds == null || irIds.isEmpty()) {
            throw new IllegalArgumentException("PILIH ITEM TERLEBIH DAHULU!");
        }
        for (Integer irId : irIds) {
            jdbcTemplate.update("delete from tb_item_request where n_ir_id = ?", irId);
        }
        return new ItemRequestActionResultResponse(true, "PEMBATALAN BERHASIL");
    }

    // ------------------------------------------------------------------ history

    /**
     * History permintaan per rentang tanggal. Migrasi dari
     * {@code WarehouseDAO.getItemRequestAll()} +
     * {@code WarehouseManagerImpl.getSentItem(HistoryRequestController)}.
     */
    public List<ItemRequestGroupResponse> getHistory(Integer sourceWarehouseId, String from, String to) {
        if (sourceWarehouseId == null) {
            throw new IllegalArgumentException("LOKASI TRANSAKSI HARUS DIISI!");
        }
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        Timestamp start = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp end = Timestamp.valueOf(toDate.atTime(23, 59, 59));
        List<RequestRow> rows = jdbcTemplate.query(
                "select req.n_ir_id, req.v_request_code, req.n_item_id, req.n_qty_req, "
                        + "req.n_qty_sent, req.n_status, req.d_whn_create, "
                        + "src.v_whouse_name as source_name, tgt.v_whouse_name as target_name, "
                        + "item.v_item_code, item.v_item_name, sat.v_mitem_end_quantify "
                        + "from tb_item_request req "
                        + "join ms_warehouse src on src.n_whouse_id = req.n_source_whouse_id "
                        + "join ms_warehouse tgt on tgt.n_whouse_id = req.n_target_whouse_id "
                        + "join ms_item item on item.n_item_id = req.n_item_id "
                        + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                        + "where req.n_source_whouse_id = ? "
                        + "and req.d_whn_create between ? and ? "
                        + "order by req.v_request_code, req.n_ir_id",
                (resultSet, rowNum) -> mapRequestRow(resultSet),
                sourceWarehouseId, start, end);
        return groupRequests(rows);
    }

    // ------------------------------------------------------------------ helpers

    private String generateRequestCode(Timestamp now) {
        Integer sequence = getNextSequence("nota_request_seq");
        return now.toLocalDateTime().format(REQUEST_CODE_FORMAT) + "-"
                + String.format("%03d", sequence);
    }

    private Integer getNextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private String requestStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        if (status == REQUEST_ALL_SENT) {
            return "SELESAI";
        }
        return "BARU";
    }

    private LocalDate parseDate(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String like(String value) {
        return "%" + (value != null ? value : "") + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String toIsoDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString();
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    // ------------------------------------------------------------------ rows

    private static final class RequestRow {
        private final int irId;
        private final String requestCode;
        private final int itemId;
        private final String code;
        private final String name;
        private final String unit;
        private final int qtyReq;
        private final int qtySent;
        private final Integer status;
        private final String sourceName;
        private final String targetName;
        private final Timestamp createdAt;

        private RequestRow(int irId, String requestCode, int itemId, String code, String name,
                String unit, int qtyReq, int qtySent, Integer status, String sourceName,
                String targetName, Timestamp createdAt) {
            this.irId = irId;
            this.requestCode = requestCode;
            this.itemId = itemId;
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.qtyReq = qtyReq;
            this.qtySent = qtySent;
            this.status = status;
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.createdAt = createdAt;
        }
    }

    private static final class ApprovalRow {
        private final int mutationId;
        private final int irId;
        private final int batchId;
        private final String requestCode;
        private final int itemId;
        private final String code;
        private final String name;
        private final String unit;
        private final Integer qty;
        private final String sourceName;
        private final String targetName;

        private ApprovalRow(int mutationId, int irId, int batchId, String requestCode, int itemId,
                String code, String name, String unit, Integer qty, String sourceName,
                String targetName) {
            this.mutationId = mutationId;
            this.irId = irId;
            this.batchId = batchId;
            this.requestCode = requestCode;
            this.itemId = itemId;
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.qty = qty;
            this.sourceName = sourceName;
            this.targetName = targetName;
        }
    }

    private static final class ApproveContext {
        private final Integer mutationId;
        private final int irId;
        private final int batchId;
        private final int itemId;
        private final int sourceWhouseId;
        private final int targetWhouseId;
        private final Integer qty;

        private ApproveContext(Integer mutationId, int irId, int batchId, int itemId,
                int sourceWhouseId, int targetWhouseId, Integer qty) {
            this.mutationId = mutationId;
            this.irId = irId;
            this.batchId = batchId;
            this.itemId = itemId;
            this.sourceWhouseId = sourceWhouseId;
            this.targetWhouseId = targetWhouseId;
            this.qty = qty;
        }
    }
}
