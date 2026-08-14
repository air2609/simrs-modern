package com.vone.simrs.master.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0038 (ITEM MASTER).
 * Mengikuti logika legacy {@code ItemManangerImpl} + {@code ItemDAO}.
 */
@Service
public class ItemService {

    private final JdbcTemplate jdbcTemplate;

    public ItemService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar item beserta group, satuan, dan supplier.
     * Mengikuti {@code ItemManangerImpl.getAllItem()} yang menampilkan
     * kode, nama, group, supplier, dan satuan.
     */
    public List<ItemRowResponse> getItems() {
        String sql = "select i.n_item_id, i.v_item_code, i.v_item_name, i.v_barcode_no, "
                + "i.n_item_group_id, g.v_item_group_name, "
                + "i.n_mitem_id, m.v_mitem_end_quantify, "
                + "i.v_item_returnable, i.n_type, i.n_r, "
                + "i.n_item_buffer_limit, i.n_item_plafon, i.n_max_order "
                + "from ms_item i "
                + "left join ms_item_group g on g.n_item_group_id = i.n_item_group_id "
                + "left join ms_item_measurement m on m.n_mitem_id = i.n_mitem_id "
                + "order by i.v_item_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new ItemRowResponse(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_barcode_no"),
                toInteger(resultSet.getObject("n_item_group_id")),
                resultSet.getString("v_item_group_name"),
                toInteger(resultSet.getObject("n_mitem_id")),
                resultSet.getString("v_mitem_end_quantify"),
                resultSet.getString("v_item_returnable"),
                toShort(resultSet.getObject("n_type")),
                toShort(resultSet.getObject("n_r")),
                toShort(resultSet.getObject("n_item_buffer_limit")),
                toShort(resultSet.getObject("n_item_plafon")),
                toInteger(resultSet.getObject("n_max_order")),
                getSuppliers(resultSet.getInt("n_item_id"))));
    }

    /**
     * Daftar supplier untuk sebuah item. Mengikuti relasi
     * {@code MsItem.getMsItemSupplied()} yang menampilkan nama vendor.
     */
    private List<String> getSuppliers(int itemId) {
        String sql = "select v.v_vendor_name from ms_item_supplied s "
                + "join ms_vendor v on v.n_vendor_id = s.n_vendor_id "
                + "where s.n_item_id = ? order by v.v_vendor_name";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> resultSet.getString("v_vendor_name"), itemId);
    }

    /**
     * Daftar group item untuk dropdown "GROUP ITEM".
     * Mengikuti {@code ItemGroupController.getItemGroupForSelect()}.
     */
    public List<ItemGroupOptionResponse> getItemGroupOptions() {
        String sql = "select n_item_group_id, v_item_group_code, v_item_group_name "
                + "from ms_item_group order by v_item_group_name";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new ItemGroupOptionResponse(
                resultSet.getInt("n_item_group_id"),
                resultSet.getString("v_item_group_code"),
                resultSet.getString("v_item_group_name")));
    }

    /**
     * Daftar satuan untuk dropdown "SATUAN".
     * Mengikuti
     * {@code ItemMeasurementController.getItemMeasurementLastQuantityForSelect()}.
     */
    public List<ItemMeasurementOptionResponse> getItemMeasurementOptions() {
        String sql = "select n_mitem_id, v_mitem_early_quantify, v_mitem_end_quantify, n_mitem_end_qty "
                + "from ms_item_measurement order by v_mitem_end_quantify";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new ItemMeasurementOptionResponse(
                resultSet.getInt("n_mitem_id"),
                resultSet.getString("v_mitem_early_quantify"),
                resultSet.getString("v_mitem_end_quantify"),
                resultSet.getShort("n_mitem_end_qty")));
    }

    /**
     * Daftar supplier/vendor untuk dropdown "SUPPLIER".
     * Mengikuti {@code VendorController.getVendorList()}.
     */
    public List<VendorOptionResponse> getVendorOptions() {
        String sql = "select n_vendor_id, v_vendor_code, v_vendor_name "
                + "from ms_vendor order by v_vendor_name";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new VendorOptionResponse(
                resultSet.getInt("n_vendor_id"),
                resultSet.getString("v_vendor_code"),
                resultSet.getString("v_vendor_name")));
    }

    /**
     * Simpan / update item beserta supplier-nya.
     * Mengikuti {@code ItemDAO.save()} dan {@code ItemDAO.update()}.
     */
    @Transactional
    public void save(ItemSaveRequest request, String username) {
        String itemCode = normalize(request.getItemCode());
        String itemName = normalize(request.getItemName());

        if (itemCode == null || itemCode.isEmpty()) {
            throw new IllegalArgumentException("Kode item harus diisi.");
        }
        if (itemName == null || itemName.isEmpty()) {
            throw new IllegalArgumentException("Nama item harus diisi.");
        }
        if (request.getItemGroupId() == null) {
            throw new IllegalArgumentException("Group item harus dipilih.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_item (n_item_id, v_item_code, v_item_name, v_barcode_no, "
                            + "n_item_group_id, n_mitem_id, v_item_returnable, n_type, n_r, "
                            + "n_item_buffer_limit, n_item_plafon, n_max_order, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                    id,
                    itemCode,
                    itemName,
                    normalize(request.getBarcodeNo()),
                    request.getItemGroupId(),
                    request.getMeasurementId(),
                    normalize(request.getItemReturnable()),
                    request.getItemType(),
                    request.getR(),
                    request.getBufferLimit(),
                    request.getPlafon(),
                    request.getMaxOrder(),
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_item set v_item_code = ?, v_item_name = ?, v_barcode_no = ?, "
                            + "n_item_group_id = ?, n_mitem_id = ?, v_item_returnable = ?, n_type = ?, n_r = ?, "
                            + "n_item_buffer_limit = ?, n_item_plafon = ?, n_max_order = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_item_id = ?",
                    itemCode,
                    itemName,
                    normalize(request.getBarcodeNo()),
                    request.getItemGroupId(),
                    request.getMeasurementId(),
                    normalize(request.getItemReturnable()),
                    request.getItemType(),
                    request.getR(),
                    request.getBufferLimit(),
                    request.getPlafon(),
                    request.getMaxOrder(),
                    normalizeActor(username),
                    id);
        }

        // Hapus relasi supplier lama lalu simpan ulang (mengikuti ItemDAO.update).
        jdbcTemplate.update("delete from ms_item_supplied where n_item_id = ?", id);
        if (request.getSupplierIds() != null) {
            for (Integer vendorId : request.getSupplierIds()) {
                if (vendorId == null) {
                    continue;
                }
                jdbcTemplate.update(
                        "insert into ms_item_supplied (n_item_supplied_id, n_item_id, n_vendor_id, "
                                + "v_who_create, d_whn_create) values (?, ?, ?, ?, now())",
                        nextSuppliedId(),
                        id,
                        vendorId,
                        normalizeActor(username));
            }
        }
    }

    /**
     * Hapus item. Mengikuti {@code ItemDAO.delete()}.
     */
    @Transactional
    public boolean delete(Integer id) {
        jdbcTemplate.update("delete from ms_item_supplied where n_item_id = ?", id);
        int affected = jdbcTemplate.update("delete from ms_item where n_item_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject("select nextval('ms_item_n_item_id_seq')", Integer.class);
    }

    private Integer nextSuppliedId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_item_supplied_n_item_supplied_id_seq')", Integer.class);
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

    private Short toShort(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        return Short.valueOf(value.toString());
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
