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
 * Service untuk screen SC0191 (ORDER PERMINTAAN PEMBELIAN / OPP).
 *
 * <p>
 * Migrasi dari legacy {@code PORController} + {@code PORManagerImpl} +
 * {@code MsWarehouseDAO} + {@code TbPurchaseRequestDAO} +
 * {@code ItemDAO.serachItemUnderBuffer()}:
 * <ul>
 * <li>{@code PORController.init()} → {@link #getMasters(String)}</li>
 * <li>{@code PORManagerImpl.redrawPORController()} →
 * {@link #getItems(Integer)}</li>
 * <li>{@code ItemPicker.search()} + {@code ItemDAO.serachItemUnderBuffer()} →
 * {@link #searchAddItems(String, String)}</li>
 * <li>{@code PORManagerImpl.doSearchPORController()} →
 * {@link #searchOpp(String)}</li>
 * <li>{@code PORManagerImpl.redrawSearchPORController()} →
 * {@link #getOppDetail(String)}</li>
 * <li>{@code PORController.searchSupplier()} →
 * {@link #searchSuppliers(String, String)}</li>
 * <li>{@code PORManagerImpl.doSaveAddPORController()} →
 * {@link #save(PurchaseRequestSaveRequest, String)}</li>
 * </ul>
 */
@Service
public class PurchaseRequestService {

    private static final String SCREEN_CODE = "SC0191";
    private static final String STATUS_OPEN = "OPEN";
    private static final String DEFAULT_MONTHS = "JANUARI,PEBRUARI,MARET,APRIL,MEI,JUNI,JULI,AGUSTUS,SEPTEMBER,OKTOBER,NOPEMBER,DESEMBER";

    private final JdbcTemplate jdbcTemplate;

    public PurchaseRequestService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Data master untuk form: unit LOKASI TRANSAKSI untuk screen SC0191 dan
     * daftar satuan (measurement). Migrasi dari legacy
     * {@code PORController.init()} +
     * {@code ItemMeasurementManagerImpl.getMeasurementType()}.
     */
    public PurchaseRequestMastersResponse getMasters(String username) {
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

        return new PurchaseRequestMastersResponse(units, measurements);
    }

    /**
     * Daftar item di bawah buffer untuk sebuah warehouse. Migrasi dari legacy
     * {@code MsWarehouseDAO.getItemUnderBuffer()}.
     */
    public List<PurchaseRequestItemResponse> getItems(Integer warehouseId) {
        String sql = "select it.n_item_id, it.v_item_code, it.v_item_name, "
                + "coalesce(g.v_item_group_code, '-') as v_item_group_code, "
                + "coalesce(q1.qty, 0) as qty, "
                + "coalesce(it.n_item_buffer_limit, 0) as n_item_buffer_limit, "
                + "coalesce(it.n_max_order, 0) as n_max_order, "
                + "coalesce(m.v_mitem_early_quantify, '-') as v_mitem_early_quantify, "
                + "coalesce(m.n_mitem_id, 0) as n_mitem_id "
                + "from ms_item it "
                + "left join ms_item_group g on g.n_item_group_id = it.n_item_group_id "
                + "left join ms_item_measurement m on m.n_mitem_id = it.n_mitem_id "
                + "left join ("
                + "  select inv.n_item_id, sum(inv.n_item_inv_qty) as qty "
                + "  from tb_item_inventory inv "
                + "  where inv.n_whouse_id = ? and inv.n_item_inv_qty > 0 "
                + "  group by inv.n_item_id"
                + ") q1 on q1.n_item_id = it.n_item_id "
                + "where q1.qty <= it.n_item_buffer_limit "
                + "order by it.n_item_id";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new PurchaseRequestItemResponse(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_item_group_code"),
                resultSet.getInt("qty"),
                resultSet.getInt("n_item_buffer_limit"),
                resultSet.getInt("n_max_order"),
                resultSet.getString("v_mitem_early_quantify"),
                countOpenOpp(resultSet.getInt("n_item_id")),
                openOppNumbers(resultSet.getInt("n_item_id"))),
                warehouseId);
    }

    /**
     * Pencarian item untuk dialog TAMBAH ITEM. Migrasi dari legacy
     * {@code ItemDAO.serachItemUnderBuffer()}.
     */
    public List<PurchaseRequestAddItemResponse> searchAddItems(String code, String name) {
        String sql = "select coalesce(sum(inv.n_item_inv_qty), 0) as qty, "
                + "it.n_item_id as n_item_id, coalesce(it.n_item_buffer_limit, 0) as n_item_buffer_limit, "
                + "it.v_item_code, it.v_item_name, "
                + "coalesce(m.v_mitem_early_quantify, '-') as v_mitem_early_quantify, "
                + "coalesce(it.n_max_order, 0) as n_max_order, "
                + "coalesce(g.v_item_group_code, '-') as v_item_group_code, "
                + "coalesce(m.n_mitem_id, 0) as n_mitem_id "
                + "from ms_item it "
                + "left join tb_item_inventory inv on it.n_item_id = inv.n_item_id "
                + "inner join ms_item_measurement m on m.n_mitem_id = it.n_mitem_id "
                + "inner join ms_item_group g on g.n_item_group_id = it.n_item_group_id "
                + "where upper(it.v_item_code) like ? and upper(it.v_item_name) like ? "
                + "group by it.n_item_id, it.n_item_buffer_limit, it.v_item_code, it.v_item_name, "
                + "m.v_mitem_early_quantify, g.v_item_group_code, it.n_max_order, m.n_mitem_id "
                + "having coalesce(sum(inv.n_item_inv_qty), 0) <= it.n_item_buffer_limit "
                + "order by it.v_item_code limit 200";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new PurchaseRequestAddItemResponse(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_item_group_code"),
                resultSet.getInt("qty"),
                resultSet.getInt("n_item_buffer_limit"),
                resultSet.getInt("n_max_order"),
                resultSet.getString("v_mitem_early_quantify"),
                resultSet.getInt("n_mitem_id")),
                like(code), like(name));
    }

    /**
     * Pencarian OPP dengan status OPEN untuk bandbox NO. OPP. Migrasi dari
     * legacy
     * {@code TbPurchaseRequestDAO.searchTbPurchaseRequestByCodeForApproval()}.
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
                like(prCode), STATUS_OPEN);
    }

    /**
     * Header + detail OPP. Migrasi dari legacy
     * {@code PORManagerImpl.redrawSearchPORController()}.
     */
    public PurchaseRequestOppDetailResponse getOppDetail(String prCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select pr.n_pr_id, pr.v_pr_code, pr.v_pr_status, "
                            + "pr.n_unit_id, coalesce(unt.v_unit_name, '-') as v_unit_name, "
                            + "coalesce(stf.v_staff_code, '') || '-' || coalesce(stf.v_staff_name, '') as issuer_name, "
                            + "pr.n_supplier_id, coalesce(v.v_vendor_name, '-') as v_vendor_name "
                            + "from tb_purchase_request pr "
                            + "left join ms_unit unt on unt.n_unit_id = pr.n_unit_id "
                            + "left join ms_staff stf on stf.n_staff_id = pr.n_issuer_id "
                            + "left join ms_vendor v on v.n_vendor_id = pr.n_supplier_id "
                            + "where pr.v_pr_code = ?",
                    (resultSet, rowNum) -> {
                        List<PurchaseRequestDetailResponse> items = loadDetailItems(
                                resultSet.getInt("n_pr_id"), resultSet.getInt("n_unit_id"));
                        return new PurchaseRequestOppDetailResponse(
                                resultSet.getString("v_pr_code"),
                                resultSet.getString("v_pr_status"),
                                resultSet.getString("issuer_name"),
                                toInteger(resultSet.getObject("n_unit_id")),
                                resultSet.getString("v_unit_name"),
                                toInteger(resultSet.getObject("n_supplier_id")),
                                resultSet.getString("v_vendor_name"),
                                items);
                    },
                    prCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }
    }

    /**
     * Pencarian supplier. Migrasi dari legacy
     * {@code VendorManager.searchVendor()}.
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
     * Simpan OPP baru. Migrasi dari legacy
     * {@code PORManagerImpl.doSaveAddPORController()}.
     */
    @Transactional
    public PurchaseRequestSaveResultResponse save(PurchaseRequestSaveRequest request, String username) {
        if (request.getUnitId() == null) {
            throw new IllegalArgumentException("LOKASI TRANSAKSI WAJIB DIISI.");
        }
        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException("Isi Data Supplier Terlebih Dahulu...!");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("DAFTAR ITEM BELUM ADA.");
        }

        Integer staffId = findStaffId(username);
        if (staffId == null) {
            throw new IllegalArgumentException("STAFF USER TIDAK DITEMUKAN.");
        }

        String prCode = generatePrCode();
        Integer prId = nextVal("tb_purchase_request_n_pr_id_seq");
        Timestamp now = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update(
                "insert into tb_purchase_request (n_pr_id, n_issuer_id, n_unit_id, v_pr_code, "
                        + "v_pr_status, n_supplier_id, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                prId, staffId, request.getUnitId(), prCode, STATUS_OPEN,
                request.getSupplierId(), normalize(username), now);

        for (PurchaseRequestLineRequest line : request.getLines()) {
            Integer detailId = nextVal("tb_purchase_request_detail_n_pr_det_id_seq");
            jdbcTemplate.update(
                    "insert into tb_purchase_request_detail (n_pr_det_id, n_pr_id, n_item_id, "
                            + "n_mitem_id, n_pr_det_qty_requested, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, ?)",
                    detailId, prId, line.getItemId(), line.getItemMeasurementId(),
                    line.getQtyRequested() == null ? 0 : line.getQtyRequested(),
                    normalize(username), now);
        }

        return new PurchaseRequestSaveResultResponse(prCode);
    }

    /**
     * Ubah OPP yang masih berstatus OPEN. Migrasi dari legacy
     * {@code PORManagerImpl.doSaveModifyPORController()}.
     */
    @Transactional
    public void update(PurchaseRequestUpdateRequest request, String username) {
        if (request.getPrCode() == null || request.getPrCode().trim().isEmpty()) {
            throw new IllegalArgumentException("NO. OPP WAJIB DIISI.");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("DAFTAR ITEM BELUM ADA.");
        }

        Integer prId = findPrId(request.getPrCode().trim());
        if (prId == null) {
            throw new IllegalArgumentException("NO. OPP TIDAK DITEMUKAN.");
        }
        String status = jdbcTemplate.queryForObject(
                "select v_pr_status from tb_purchase_request where n_pr_id = ?",
                String.class, prId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("STATUS OPP SUDAH TIDAK OPEN.");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update("delete from tb_purchase_request_detail where n_pr_id = ?", prId);
        jdbcTemplate.update(
                "update tb_purchase_request set n_unit_id = ?, n_supplier_id = ?, "
                        + "v_who_change = ?, d_whn_change = now() where n_pr_id = ?",
                request.getUnitId(), request.getSupplierId(), normalize(username), prId);

        for (PurchaseRequestLineRequest line : request.getLines()) {
            Integer detailId = nextVal("tb_purchase_request_detail_n_pr_det_id_seq");
            jdbcTemplate.update(
                    "insert into tb_purchase_request_detail (n_pr_det_id, n_pr_id, n_item_id, "
                            + "n_mitem_id, n_pr_det_qty_requested, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, ?)",
                    detailId, prId, line.getItemId(), line.getItemMeasurementId(),
                    line.getQtyRequested() == null ? 0 : line.getQtyRequested(),
                    normalize(username), now);
        }
    }

    /**
     * Batalkan OPP yang masih berstatus OPEN. Migrasi dari legacy
     * {@code PORController.doRevoke()} yang mengubah status menjadi REVOKED.
     */
    @Transactional
    public void revoke(String prCode, String username) {
        if (prCode == null || prCode.trim().isEmpty()) {
            throw new IllegalArgumentException("NO. OPP WAJIB DIISI.");
        }
        Integer prId = findPrId(prCode.trim());
        if (prId == null) {
            throw new IllegalArgumentException("NO. OPP TIDAK DITEMUKAN.");
        }
        String status = jdbcTemplate.queryForObject(
                "select v_pr_status from tb_purchase_request where n_pr_id = ?",
                String.class, prId);
        if (!STATUS_OPEN.equals(status)) {
            throw new IllegalArgumentException("STATUS OPP SUDAH TIDAK OPEN.");
        }
        jdbcTemplate.update(
                "update tb_purchase_request set v_pr_status = 'REVOKED', "
                        + "v_who_change = ?, d_whn_change = now() where n_pr_id = ?",
                normalize(username), prId);
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

    private List<PurchaseRequestDetailResponse> loadDetailItems(Integer prId, Integer unitId) {
        String sql = "select d.n_item_id, it.v_item_code, it.v_item_name, "
                + "coalesce(g.v_item_group_code, '-') as v_item_group_code, "
                + "coalesce(m.v_mitem_early_quantify, '-') as v_mitem_early_quantify, "
                + "d.n_mitem_id, d.n_pr_det_qty_requested "
                + "from tb_purchase_request_detail d "
                + "join ms_item it on it.n_item_id = d.n_item_id "
                + "left join ms_item_group g on g.n_item_group_id = it.n_item_group_id "
                + "left join ms_item_measurement m on m.n_mitem_id = d.n_mitem_id "
                + "where d.n_pr_id = ? order by d.n_pr_det_id";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new PurchaseRequestDetailResponse(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_item_group_code"),
                0,
                toInteger(resultSet.getObject("n_mitem_id")) != null
                        ? getBufferLimit(resultSet.getInt("n_item_id"))
                        : 0,
                getMaxOrder(resultSet.getInt("n_item_id")),
                resultSet.getString("v_mitem_early_quantify"),
                resultSet.getInt("n_pr_det_qty_requested")),
                prId);
    }

    private Integer getBufferLimit(Integer itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coalesce(n_item_buffer_limit, 0) from ms_item where n_item_id = ?",
                    Integer.class, itemId);
        } catch (EmptyResultDataAccessException exception) {
            return 0;
        }
    }

    private Integer getMaxOrder(Integer itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coalesce(n_max_order, 0) from ms_item where n_item_id = ?",
                    Integer.class, itemId);
        } catch (EmptyResultDataAccessException exception) {
            return 0;
        }
    }

    private Integer countOpenOpp(Integer itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select count(*) from tb_purchase_request_detail d "
                            + "join tb_purchase_request pr on pr.n_pr_id = d.n_pr_id "
                            + "where d.n_item_id = ? and pr.v_pr_status = 'APPROVED' "
                            + "and pr.n_supplier_id is not null "
                            + "and date_part('day', now() - pr.d_whn_create) between 0 and 360",
                    Integer.class, itemId);
        } catch (EmptyResultDataAccessException exception) {
            return 0;
        }
    }

    private String openOppNumbers(Integer itemId) {
        List<String> codes = jdbcTemplate.query(
                "select pr.v_pr_code from tb_purchase_request_detail d "
                        + "join tb_purchase_request pr on pr.n_pr_id = d.n_pr_id "
                        + "where d.n_item_id = ? and pr.v_pr_status = 'APPROVED' "
                        + "and pr.n_supplier_id is not null "
                        + "and date_part('day', now() - pr.d_whn_create) between 0 and 360 "
                        + "order by pr.v_pr_code",
                (resultSet, rowNum) -> resultSet.getString("v_pr_code"), itemId);
        return String.join(",", codes);
    }

    private String generatePrCode() {
        Integer seq = nextVal("sq_purchase_order_req_code");
        Date date = new Date();
        String day = new SimpleDateFormat("dd").format(date);
        String month = new SimpleDateFormat("MM").format(date);
        String year = new SimpleDateFormat("yyyy").format(date);
        return day + seq + "/IF/RSTS/" + monthName(month) + "/" + year;
    }

    private String monthName(String month) {
        String[] names = DEFAULT_MONTHS.split(",");
        try {
            int index = Integer.parseInt(month) - 1;
            if (index >= 0 && index < names.length) {
                return names[index];
            }
        } catch (NumberFormatException ignored) {
            // fallthrough
        }
        return month;
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

    private Integer nextVal(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String like(String value) {
        return "%" + normalize(value) + "%";
    }
}