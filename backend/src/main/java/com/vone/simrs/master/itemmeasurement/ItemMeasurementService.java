package com.vone.simrs.master.itemmeasurement;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0040 (ITEM MEASUREMENT MASTER).
 * Mengikuti logika legacy {@code ItemMeasurementManagerImpl} +
 * {@code ItemMeasurementDAO}.
 */
@Service
public class ItemMeasurementService {

    private final JdbcTemplate jdbcTemplate;

    public ItemMeasurementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar satuan item. Mengikuti
     * {@code ItemMeasurementDAO.getAllItemMeasurement()} yang menampilkan
     * satuan awal, satuan akhir, dan jumlah pembagi.
     */
    public List<ItemMeasurementRowResponse> getItemMeasurements() {
        String sql = "select n_mitem_id, v_mitem_early_quantify, v_mitem_end_quantify, n_mitem_end_qty "
                + "from ms_item_measurement order by v_mitem_early_quantify";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new ItemMeasurementRowResponse(
                resultSet.getInt("n_mitem_id"),
                resultSet.getString("v_mitem_early_quantify"),
                resultSet.getString("v_mitem_end_quantify"),
                resultSet.getShort("n_mitem_end_qty")));
    }

    /**
     * Simpan / update satuan item. Mengikuti
     * {@code ItemMeasurementDAO.save()} (saveOrUpdate).
     */
    @Transactional
    public void save(ItemMeasurementSaveRequest request, String username) {
        String earlyQuantify = normalize(request.getEarlyQuantify());
        String endQuantify = normalize(request.getEndQuantify());

        if (earlyQuantify == null || earlyQuantify.isEmpty()) {
            throw new IllegalArgumentException("Satuan awal harus diisi.");
        }
        if (endQuantify == null || endQuantify.isEmpty()) {
            throw new IllegalArgumentException("Satuan akhir harus diisi.");
        }
        if (request.getEndQty() == null) {
            throw new IllegalArgumentException("Jumlah pembagi harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_item_measurement (n_mitem_id, v_mitem_early_quantify, "
                            + "v_mitem_end_quantify, n_mitem_end_qty, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, now())",
                    id,
                    earlyQuantify,
                    endQuantify,
                    request.getEndQty(),
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_item_measurement set v_mitem_early_quantify = ?, "
                            + "v_mitem_end_quantify = ?, n_mitem_end_qty = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_mitem_id = ?",
                    earlyQuantify,
                    endQuantify,
                    request.getEndQty(),
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus satuan item. Mengikuti {@code ItemMeasurementDAO.delete()}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_item_measurement where n_mitem_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_item_measurement_n_mitem_id_seq')", Integer.class);
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
