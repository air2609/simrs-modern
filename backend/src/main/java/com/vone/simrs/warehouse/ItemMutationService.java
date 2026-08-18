package com.vone.simrs.warehouse;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
 * Service untuk screen SC0121 (FORM MUTASI ITEM / itemMutation.zul).
 *
 * <p>
 * Migrasi dari legacy {@code WarehouseController} + {@code WarehouseManagerImpl}
 * + {@code MsWarehouseDAO} (getItemRequest / getTbItemInventory / saveItemMutation).
 * Sisi PENGIRIMAN permintaan O-BM: gudang tujuan membuat tb_item_mutation
 * (n_status=1) dan menambah n_qty_sent pada tb_item_request.
 */
@Service
public class ItemMutationService {

    private static final int MUTATION_PENDING = 1;

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public ItemMutationService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Gudang lokasi transaksi = gudang dari unit tempat user bertugas. Migrasi
     * dari {@code WarehouseManagerImpl.getWhouseByStaffId()}.
     */
    public List<ItemRequestWarehouseResponse> getMasters(String username) {
        return jdbcTemplate.query(
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
    }

    /**
     * Daftar permintaan yang harus dipenuhi gudang ini (n_target_whouse_id = gudang)
     * dan belum terkirim penuh. Migrasi dari {@code MsWarehouseDAO.getItemRequest()}
     * + {@code WarehouseManagerImpl.loadItemRequest()}.
     */
    public List<ItemRequestGroupResponse> getRequests(Integer warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("LOKASI TRANSAKSI HARUS DIISI!");
        }
        List<RequestRow> rows = jdbcTemplate.query(
                "select req.n_ir_id, req.v_request_code, req.n_item_id, req.n_qty_req, "
                        + "req.n_qty_sent, req.d_whn_create, "
                        + "src.v_whouse_name as source_name, tgt.v_whouse_name as target_name, "
                        + "item.v_item_code, item.v_item_name, sat.v_mitem_end_quantify "
                        + "from tb_item_request req "
                        + "join ms_warehouse src on src.n_whouse_id = req.n_source_whouse_id "
                        + "join ms_warehouse tgt on tgt.n_whouse_id = req.n_target_whouse_id "
                        + "join ms_item item on item.n_item_id = req.n_item_id "
                        + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                        + "where req.n_target_whouse_id = ? and req.n_qty_sent < req.n_qty_req "
                        + "order by req.v_request_code, req.n_ir_id",
                (resultSet, rowNum) -> mapRequestRow(resultSet),
                warehouseId);
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
                    entry.getKey(), header.sourceName, header.targetName,
                    0, "", toIsoDateTime(header.createdAt), entry.getValue()));
        }
        return groups;
    }

    /**
     * Batch inventory item di gudang ini untuk dialog DETAIL. Migrasi dari
     * {@code WarehouseDAO.getTbItemInventory()}.
     */
    public List<ItemMutationBatchResponse> getBatches(Integer warehouseId, Integer irId) {
        if (irId == null) {
            throw new IllegalArgumentException("PILIH PERMINTAAN TERLEBIH DAHULU!");
        }
        RequestRow request = findRequest(irId);
        return jdbcTemplate.query(
                "select inv.n_batch_id, inv.n_item_inv_qty as stock "
                        + "from tb_item_inventory inv "
                        + "where inv.n_whouse_id = ? and inv.n_item_id = ? and inv.n_item_inv_qty > 0 "
                        + "order by inv.n_item_inv_qty",
                (resultSet, rowNum) -> new ItemMutationBatchResponse(
                        resultSet.getInt("n_batch_id"),
                        request.code, request.name, request.unit,
                        getNullableInteger(resultSet, "stock")),
                warehouseId, request.itemId);
    }

    /**
     * Kirim mutasi item: simpan tb_item_mutation (v_status=1) + tambah
     * n_qty_sent pada tb_item_request. Migrasi dari legacy
     * {@code WarehouseController.kirimClick()} + {@code MsWarehouseDAO.saveItemMutation()}.
     */
    @Transactional
    public ItemRequestActionResultResponse send(ItemMutationSendRequest request, String username) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("Isi Data Item Terlebih Dahulu..!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (ItemMutationLineRequest line : request.getLines()) {
            if (line.getQty() == null || line.getQty() <= 0) {
                continue;
            }
            RequestRow req = findRequest(line.getIrId());
            // cek stok batch
            Integer stock = findBatchStock(request.getWarehouseId(), req.itemId, line.getBatchId());
            if (stock == null || stock < line.getQty()) {
                throw new IllegalArgumentException(
                        "Stok batch " + line.getBatchId() + " tidak mencukupi.");
            }
            // cek sisa permintaan
            int sisa = req.qtyReq - req.qtySent;
            if (line.getQty() > sisa) {
                throw new IllegalArgumentException(
                        "Jumlah melebihi sisa permintaan (" + sisa + ").");
            }
            jdbcTemplate.update(
                    "insert into tb_item_mutation (n_mitem_qty, v_who_create, d_whn_create, "
                            + "n_batch_id, n_ir_id, v_status) values (?, ?, ?, ?, ?, ?)",
                    line.getQty(), username, now, line.getBatchId(), line.getIrId(),
                    MUTATION_PENDING);
            jdbcTemplate.update(
                    "update tb_item_request set n_qty_sent = n_qty_sent + ?, "
                            + "v_who_change = ?, d_whn_change = ? where n_ir_id = ?",
                    line.getQty(), username, now, line.getIrId());
        }
        return new ItemRequestActionResultResponse(true, "Permintaan Telah Di Kirim..!");
    }

    private RequestRow findRequest(Integer irId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select req.n_ir_id, req.v_request_code, req.n_item_id, req.n_qty_req, "
                            + "req.n_qty_sent, req.d_whn_create, "
                            + "src.v_whouse_name as source_name, tgt.v_whouse_name as target_name, "
                            + "item.v_item_code, item.v_item_name, sat.v_mitem_end_quantify "
                            + "from tb_item_request req "
                            + "join ms_warehouse src on src.n_whouse_id = req.n_source_whouse_id "
                            + "join ms_warehouse tgt on tgt.n_whouse_id = req.n_target_whouse_id "
                            + "join ms_item item on item.n_item_id = req.n_item_id "
                            + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                            + "where req.n_ir_id = ?",
                    (resultSet, rowNum) -> mapRequestRow(resultSet),
                    irId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Permintaan tidak ditemukan.");
        }
    }

    private Integer findBatchStock(Integer warehouseId, Integer itemId, Integer batchId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_item_inv_qty from tb_item_inventory "
                            + "where n_whouse_id = ? and n_item_id = ? and n_batch_id = ?",
                    Integer.class, warehouseId, itemId, batchId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase(Locale.ROOT);
    }

    private String toIsoDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString();
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static final class RequestRow {
        private final int irId;
        private final String requestCode;
        private final int itemId;
        private final String code;
        private final String name;
        private final String unit;
        private final int qtyReq;
        private final int qtySent;
        private final String sourceName;
        private final String targetName;
        private final Timestamp createdAt;

        private RequestRow(int irId, String requestCode, int itemId, String code, String name,
                String unit, int qtyReq, int qtySent, String sourceName, String targetName,
                Timestamp createdAt) {
            this.irId = irId;
            this.requestCode = requestCode;
            this.itemId = itemId;
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.qtyReq = qtyReq;
            this.qtySent = qtySent;
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.createdAt = createdAt;
        }
    }
}
