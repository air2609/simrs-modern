package com.vone.simrs.master.iteminventory;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0057 (FORM UPDATE ITEM / updateInventory.zul).
 *
 * <p>
 * Migrasi dari legacy {@code UpdateInventoryController} +
 * {@code ItemDAO.serachItemUnderBuffer()} + {@code ItemInventoryDAO.getLastInventory()/update()}.
 */
@Service
public class UpdateInventoryService {

    /**
     * Gudang inventory yang di-update. Legacy {@code ItemInventoryDAO.getLastInventory()}
     * hardcoded ke warehouse 8 (APOTIK RAJAL).
     */
    private static final int INVENTORY_WAREHOUSE_ID = 8;

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public UpdateInventoryService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Daftar item dengan total stok <= buffer limit. Migrasi dari
     * {@code ItemDAO.serachItemUnderBuffer()} + filter UI legacy
     * {@code pojo[0].equals("0")} (hanya item stok 0 yang ditampilkan).
     *
     * @param keyword filter nama item
     */
    public List<UpdateInventoryItemResponse> getItems(String keyword) {
        StringBuilder sql = new StringBuilder();
        sql.append("select coalesce(sum(inv.n_item_inv_qty), 0) as jumlah, ")
                .append("i.n_item_id as id, i.n_item_buffer_limit as buffer, ")
                .append("i.v_item_code as kode, i.v_item_name as nama, ")
                .append("m.v_mitem_early_quantify as satuan ")
                .append("from ms_item i ")
                .append("left join tb_item_inventory inv on i.n_item_id = inv.n_item_id ")
                .append("join ms_item_measurement m on m.n_mitem_id = i.n_mitem_id ")
                .append("join ms_item_group g on g.n_item_group_id = i.n_item_group_id ");
        List<Object> params = new ArrayList<>();
        if (hasText(keyword)) {
            sql.append("where upper(i.v_item_name) like ? ");
            params.add("%" + keyword.trim().toUpperCase(Locale.ROOT) + "%");
        }
        sql.append("group by i.n_item_id, i.n_item_buffer_limit, i.v_item_code, i.v_item_name, ")
                .append("m.v_mitem_early_quantify, g.v_item_group_code ")
                .append("having coalesce(sum(inv.n_item_inv_qty), 0) <= i.n_item_buffer_limit ")
                .append("order by i.v_item_name");

        List<UpdateInventoryItemResponse> all = jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> new UpdateInventoryItemResponse(
                        resultSet.getInt("id"),
                        resultSet.getString("kode"),
                        resultSet.getString("nama"),
                        resultSet.getString("satuan"),
                        getNullableInteger(resultSet, "buffer"),
                        getNullableInteger(resultSet, "jumlah")));

        // filter UI legacy: hanya tampilkan item dengan jumlah stok 0
        List<UpdateInventoryItemResponse> result = new ArrayList<>();
        for (UpdateInventoryItemResponse item : all) {
            if (item.getJumlah() != null && item.getJumlah() == 0) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Update jumlah inventory item (baris inventory terakhir di warehouse 8).
     * Migrasi dari {@code UpdateInventoryController.doSaveModify()} +
     * {@code ItemInventoryDAO.getLastInventory()/update()}.
     */
    @Transactional
    public void save(UpdateInventorySaveRequest request, String username) {
        if (request.getItemCode() == null || request.getItemCode().trim().isEmpty()) {
            throw new IllegalArgumentException("KODE ITEM HARUS DI ISI!");
        }
        if (request.getQty() == null) {
            throw new IllegalArgumentException("JUMLAH HARUS DI ISI!");
        }
        Integer itemId = findItemIdByCode(request.getItemCode().trim());
        if (itemId == null) {
            throw new IllegalArgumentException("Item dengan kode tersebut tidak ditemukan!");
        }
        Integer inventoryId = findLastInventoryId(itemId);
        if (inventoryId == null) {
            throw new IllegalArgumentException(
                    "Item belum memiliki inventory di warehouse tujuan (APOTIK RAJAL)!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_item_inventory set n_item_inv_qty = ?, d_whn_change = ?, v_who_change = ? "
                        + "where n_item_inventory_id = ?",
                request.getQty().floatValue(), now, username, inventoryId);
    }

    private Integer findItemIdByCode(String code) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_item_id from ms_item where upper(v_item_code) = ?",
                    Integer.class, code.toUpperCase(Locale.ROOT));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Integer findLastInventoryId(Integer itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_item_inventory_id from tb_item_inventory "
                            + "where n_item_id = ? and n_whouse_id = ? "
                            + "order by n_item_inventory_id desc limit 1",
                    Integer.class, itemId, INVENTORY_WAREHOUSE_ID);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }
}
