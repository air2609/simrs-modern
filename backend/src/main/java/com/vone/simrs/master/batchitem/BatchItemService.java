package com.vone.simrs.master.batchitem;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0055 (UPDATE BATCH ITEM).
 * Mengikuti logika legacy {@code ItemManangerImpl.getItemObat()} +
 * {@code updateItemBatch()} dan {@code ItemDAO.getObatDetail()}.
 */
@Service
public class BatchItemService {

    private final JdbcTemplate jdbcTemplate;

    public BatchItemService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar item obat untuk batch update. Mengikuti
     * {@code ItemDAO.getObatDetail()} pada tabel ms_item.
     */
    public List<BatchItemRowResponse> getItems() {
        String sql = "select i.n_item_id as id, i.v_item_code as code, i.v_item_name as name, "
                + "i.n_item_buffer_limit as buffer, i.n_max_order as max, "
                + "(select (d.n_subtotal/d.n_do_det_qty) * (1.1) from tb_delivery_order_detail d "
                + " where d.n_item_id = i.n_item_id order by d.n_do_det_id desc limit 1) as hargabeli, "
                + "(select s.n_selling_price from ms_item_selling_price s "
                + " where s.n_item_id = i.n_item_id limit 1) as hargajual "
                + "from ms_item i "
                + "where i.n_item_group_id = 15 and i.n_item_buffer_limit > 0 "
                + "and i.n_max_order is not null "
                + "order by i.v_item_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new BatchItemRowResponse(
                resultSet.getInt("id"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                toInteger(resultSet.getObject("buffer")),
                toInteger(resultSet.getObject("max")),
                toDouble(resultSet.getObject("hargabeli")),
                toDouble(resultSet.getObject("hargajual"))));
    }

    /**
     * Simpan batch update item. Mengikuti
     * {@code ItemManangerImpl.updateItemBatch()}:
     * - update n_item_buffer_limit & n_max_order pada ms_item (by v_item_code)
     * - update n_selling_price pada ms_item_selling_price (by n_item_id)
     */
    @Transactional
    public void save(BatchItemSaveRequest request, String username) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Tidak ada data item untuk disimpan.");
        }

        for (BatchItemRowResponse item : request.getItems()) {
            if (item.getCode() == null || item.getCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Kode item tidak boleh kosong.");
            }
            if (item.getBuffer() == null) {
                throw new IllegalArgumentException("Buffer item " + item.getCode() + " harus diisi.");
            }
            if (item.getMaxOrder() == null) {
                throw new IllegalArgumentException("Max order item " + item.getCode() + " harus diisi.");
            }
            if (item.getSellPrice() == null) {
                throw new IllegalArgumentException("Harga jual item " + item.getCode() + " harus diisi.");
            }

            // Update ms_item: buffer & max order (by v_item_code)
            jdbcTemplate.update(
                    "update ms_item set n_item_buffer_limit = ?, n_max_order = ?, "
                            + "v_who_change = ?, d_whn_change = now() where v_item_code = ?",
                    item.getBuffer().shortValue(),
                    item.getMaxOrder(),
                    normalizeActor(username),
                    item.getCode().trim().toUpperCase(Locale.ROOT));

            // Update ms_item_selling_price: harga jual (by n_item_id)
            if (item.getId() != null) {
                jdbcTemplate.update(
                        "update ms_item_selling_price set n_selling_price = ?, "
                                + "v_who_change = ?, d_whn_change = now() where n_item_id = ?",
                        item.getSellPrice(),
                        normalizeActor(username),
                        item.getId());
            }
        }
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

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.valueOf(value.toString());
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
