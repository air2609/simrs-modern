package com.vone.simrs.master.itemsellingprice;

import com.vone.simrs.master.treatment.TreatmentClassOptionResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0041 (ITEM SELLING PRICE / MASTER HARGA JUAL).
 * Mengikuti logika legacy {@code ItemSellingPriceManagerImpl} +
 * {@code ItemSellingPriceDAO}.
 */
@Service
public class ItemSellingPriceService {

    private final JdbcTemplate jdbcTemplate;

    public ItemSellingPriceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar harga jual item. Mengikuti
     * {@code ItemSellingPriceDAO.getItemSellingPrices()} yang menggabungkan
     * ms_item_selling_price dengan ms_item, memfilter berdasarkan kode/nama
     * item, dan mengurutkan berdasarkan kode item. Seluruh data ditampilkan
     * dan dipaginasi di sisi frontend.
     */
    public List<ItemSellingPriceRowResponse> getSellingPrices(String search) {
        String value = "%" + (search == null ? "" : search.trim()) + "%";
        String sql = "select p.n_item_selling_price_id, p.n_item_id, i.v_item_code, i.v_item_name, "
                + "p.n_tclass_id, t.v_tclass_desc, p.n_selling_price "
                + "from ms_item_selling_price p "
                + "join ms_item i on i.n_item_id = p.n_item_id "
                + "left join ms_treatment_class t on t.n_tclass_id = p.n_tclass_id "
                + "where (i.v_item_code like ? or i.v_item_name like ?) "
                + "order by i.v_item_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new ItemSellingPriceRowResponse(
                resultSet.getInt("n_item_selling_price_id"),
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                toInteger(resultSet.getObject("n_tclass_id")),
                resultSet.getString("v_tclass_desc"),
                toDouble(resultSet.getObject("n_selling_price"))), value, value);
    }

    /**
     * Data master untuk form: opsi kelas tarif dan opsi item.
     * Mengikuti {@code TreatmentClassController.getTClassDataList} dan
     * pencarian item pada bandbox KODE.
     */
    public ItemSellingPriceMastersResponse getMasters() {
        List<TreatmentClassOptionResponse> treatmentClassOptions = jdbcTemplate.query(
                "select n_tclass_id, v_tclass_code, v_tclass_desc "
                        + "from ms_treatment_class order by v_tclass_code",
                (resultSet, rowNum) -> new TreatmentClassOptionResponse(
                        resultSet.getInt("n_tclass_id"),
                        resultSet.getString("v_tclass_code"),
                        resultSet.getString("v_tclass_desc")));

        List<ItemOptionResponse> itemOptions = jdbcTemplate.query(
                "select n_item_id, v_item_code, v_item_name "
                        + "from ms_item order by v_item_code",
                (resultSet, rowNum) -> new ItemOptionResponse(
                        resultSet.getInt("n_item_id"),
                        resultSet.getString("v_item_code"),
                        resultSet.getString("v_item_name")));

        return new ItemSellingPriceMastersResponse(treatmentClassOptions, itemOptions);
    }

    /**
     * Simpan / update harga jual item. Mengikuti
     * {@code ItemSellingPriceDAO.save()} (saveOrUpdate).
     */
    @Transactional
    public void save(ItemSellingPriceSaveRequest request, String username) {
        if (request.getItemId() == null) {
            throw new IllegalArgumentException("Item harus dipilih.");
        }
        if (request.getTclassId() == null) {
            throw new IllegalArgumentException("Kelas tarif harus dipilih.");
        }
        if (request.getSellingPrice() == null) {
            throw new IllegalArgumentException("Harga jual harus diisi.");
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_item_selling_price (n_item_selling_price_id, n_item_id, "
                            + "n_tclass_id, n_selling_price, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, now())",
                    id,
                    request.getItemId(),
                    request.getTclassId(),
                    request.getSellingPrice(),
                    normalizeActor(username));
        } else {
            jdbcTemplate.update(
                    "update ms_item_selling_price set n_item_id = ?, n_tclass_id = ?, "
                            + "n_selling_price = ?, v_who_change = ?, d_whn_change = now() "
                            + "where n_item_selling_price_id = ?",
                    request.getItemId(),
                    request.getTclassId(),
                    request.getSellingPrice(),
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus harga jual item. Mengikuti {@code ItemSellingPriceDAO.delete()}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update(
                "delete from ms_item_selling_price where n_item_selling_price_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_item_selling_price_n_item_selling_price_id_seq')",
                Integer.class);
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
