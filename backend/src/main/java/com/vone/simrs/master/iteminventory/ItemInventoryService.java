package com.vone.simrs.master.iteminventory;

import com.vone.simrs.master.warehouse.WarehouseOptionResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0032 (FORM ALOKASI ITEM).
 * Mengikuti logika legacy {@code ItemInventoryManagerImpl} +
 * {@code ItemInventoryDAO} + {@code UnitInventoryController}.
 */
@Service
public class ItemInventoryService {

    private final JdbcTemplate jdbcTemplate;

    public ItemInventoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar alokasi item pada sebuah gudang berdasarkan kriteria pencarian.
     * Mengikuti {@code ItemInventoryDAO.getInventoryOnWhouseByCriteria()}
     * yang menggabungkan tb_item_inventory, tb_batch_item, dan ms_item,
     * hanya menampilkan qty > 0, dibatasi 100 baris.
     */
    public List<ItemInventoryRowResponse> getInventory(Integer whouseId, String keyword) {
        String crit = "%" + (keyword == null ? "" : keyword.trim().toUpperCase(Locale.ROOT)) + "%";
        String sql = "select inv.n_item_inventory_id, inv.n_item_id, item.v_item_code, item.v_item_name, "
                + "inv.n_batch_id, batch.v_batch_no, inv.n_whouse_id, wh.v_whouse_name, "
                + "inv.n_item_inv_qty, batch.n_cogs_price "
                + "from tb_item_inventory inv "
                + "join tb_batch_item batch on batch.n_batch_id = inv.n_batch_id "
                + "join ms_item item on item.n_item_id = inv.n_item_id "
                + "left join ms_warehouse wh on wh.n_whouse_id = inv.n_whouse_id "
                + "where inv.n_whouse_id = ? "
                + "and inv.n_item_inv_qty > 0 "
                + "and (upper(batch.v_batch_no) like ? "
                + "or upper(item.v_item_code) like ? "
                + "or upper(item.v_item_name) like ?) "
                + "order by inv.n_item_id "
                + "limit 100";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new ItemInventoryRowResponse(
                resultSet.getInt("n_item_inventory_id"),
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                toInteger(resultSet.getObject("n_batch_id")),
                resultSet.getString("v_batch_no"),
                toInteger(resultSet.getObject("n_whouse_id")),
                resultSet.getString("v_whouse_name"),
                toBigDecimal(resultSet.getObject("n_item_inv_qty")),
                toBigDecimal(resultSet.getObject("n_cogs_price"))), whouseId, crit, crit, crit);
    }

    /**
     * Daftar opsi gudang untuk dropdown lokasi (SCM0032).
     */
    public List<WarehouseOptionResponse> getWarehouseOptions() {
        String sql = "select n_whouse_id, v_whouse_code, v_whouse_name "
                + "from ms_warehouse order by v_whouse_name";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new WarehouseOptionResponse(
                resultSet.getInt("n_whouse_id"),
                resultSet.getString("v_whouse_code"),
                resultSet.getString("v_whouse_name")));
    }

    /**
     * Simpan alokasi item baru. Mengikuti {@code ItemInventoryDAO.save()}
     * 
     * yang membuat TbBatchItem baru (batch no, exp date, qty 5000, cogs price)
     * lalu menyimpan TbItemInventory.
     */
    @Transactional
    public void save(ItemInventorySaveRequest request, String username) {
        String itemCode = normalize(request.getItemCode());
        String batchNo = normalize(request.getBatchNo());

        if (request.getWhouseId() == null) {
            throw new IllegalArgumentException("Lokasi gudang harus dipilih.");
        }
        if (itemCode == null || itemCode.isEmpty()) {
            throw new IllegalArgumentException("Kode item harus diisi.");
        }
        if (batchNo == null || batchNo.isEmpty()) {
            throw new IllegalArgumentException("Batch no harus diisi.");
        }
        if (request.getQty() == null) {
            throw new IllegalArgumentException("Jumlah harus diisi.");
        }
        if (request.getCogsPrice() == null) {
            throw new IllegalArgumentException("Harga beli harus diisi.");
        }

        // Cari item berdasarkan kode.
        Integer itemId = request.getItemId();
        if (itemId == null) {
            itemId = findItemIdByCode(itemCode);
            if (itemId == null) {
                throw new IllegalArgumentException("Item dengan kode '" + itemCode + "' tidak ditemukan.");
            }
        }

        Integer id = request.getId();
        if (id == null) {
            // Buat batch item baru (mengikuti ItemInventoryDAO.save).
            Integer batchId = nextBatchId();
            jdbcTemplate.update(
                    "insert into tb_batch_item (n_batch_id, n_item_id, v_batch_no, d_batch_exp_date, "
                            + "n_batch_item_qty, n_cogs_price, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, now(), ?, ?, ?, now())",
                    batchId,
                    itemId,
                    batchNo,
                    (short) 5000,
                    request.getCogsPrice(),
                    normalizeActor(username));

            Integer inventoryId = nextInventoryId();
            jdbcTemplate.update(
                    "insert into tb_item_inventory (n_item_inventory_id, n_item_id, n_batch_id, n_whouse_id, "
                            + "v_item_inv_desc_id, n_item_inv_qty, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, ?, now())",
                    inventoryId,
                    itemId,
                    batchId,
                    request.getWhouseId(),
                    null,
                    request.getQty().floatValue(),
                    normalizeActor(username));
        } else {
            // Update qty alokasi (mengikuti UnitInventoryController.doSaveModify).
            jdbcTemplate.update(
                    "update tb_item_inventory set n_item_inv_qty = ?, v_who_change = ?, d_whn_change = now() "
                            + "where n_item_inventory_id = ?",
                    request.getQty().floatValue(),
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus alokasi item. Mengikuti {@code ItemInventoryDAO.delete()}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from tb_item_inventory where n_item_inventory_id = ?", id);
        return affected > 0;
    }

    private Integer findItemIdByCode(String itemCode) {
        List<Integer> ids = jdbcTemplate.query(
                "select n_item_id from ms_item where upper(v_item_code) = ? limit 1",
                (resultSet, rowNum) -> resultSet.getInt("n_item_id"),
                itemCode);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Integer nextBatchId() {
        return jdbcTemplate.queryForObject("select nextval('tb_batch_item_n_batch_id_seq')", Integer.class);
    }

    private Integer nextInventoryId() {
        return jdbcTemplate.queryForObject(
                "select nextval('tb_item_inventory_n_item_inventory_id_seq')", Integer.class);
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

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
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
