package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.ward.WardUnitResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0018 (BUFFER MONITORING / buffer.zul).
 *
 * <p>
 * Migrasi dari legacy {@code BufferController} + {@code PORManagerImpl.redrawPORController()}
 * + {@code MsWarehouseDAO.getItemUnderBuffer()/getOpenOpp()} — daftar item yang
 * stoknya di bawah batas buffer per gudang, plus jumlah open OPP (PR APPROVED).
 */
@Service
public class BufferService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public BufferService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Unit user yang memiliki gudang (dropdown LOKASI). */
    public List<WardUnitResponse> getUnits(String username) {
        return jdbcTemplate.query(
                "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name, unt.n_whouse_id "
                        + "from ms_unit unt "
                        + "join ms_staff_in_unit stfunit on stfunit.n_unit_id = unt.n_unit_id "
                        + "join ms_user usr on usr.n_staff_id = stfunit.n_staff_id "
                        + "where upper(usr.v_user_name) = ? "
                        + "and unt.n_whouse_id is not null "
                        + "order by unt.v_unit_name",
                (resultSet, rowNum) -> new WardUnitResponse(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        getNullableInteger(resultSet, "n_whouse_id")),
                normalizeUsername(username));
    }

    /**
     * Daftar stok di bawah buffer per gudang. Migrasi
     * {@code MsWarehouseDAO.getItemUnderBuffer()} + {@code getOpenOpp()}.
     */
    public BufferResponse getReport(Integer warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("LOKASI HARUS DIPILIH!");
        }

        String sql = "select it.n_item_id, it.v_item_code, it.v_item_name, "
                + "ig.v_item_group_code as jenis, q1.qty as stok, "
                + "it.n_item_buffer_limit as buffer, "
                + "im.v_mitem_early_quantify as satuan "
                + "from ms_item it "
                + "join (select inv.n_item_id, sum(n_item_inv_qty) as qty "
                + "from tb_item_inventory inv "
                + "where inv.n_whouse_id = ? and inv.n_item_inv_qty > 0 "
                + "group by inv.n_item_id) q1 on q1.n_item_id = it.n_item_id "
                + "left join ms_item_group ig on ig.n_item_group_id = it.n_item_group_id "
                + "left join ms_item_measurement im on im.n_mitem_id = it.n_mitem_id "
                + "where q1.qty <= it.n_item_buffer_limit "
                + "order by q1.n_item_id";

        List<ItemRow> items = jdbcTemplate.query(sql, new Object[] { warehouseId },
                (resultSet, rowNum) -> new ItemRow(
                        resultSet.getInt("n_item_id"),
                        resultSet.getString("v_item_code"),
                        resultSet.getString("v_item_name"),
                        resultSet.getString("jenis"),
                        resultSet.getInt("stok"),
                        resultSet.getInt("buffer"),
                        resultSet.getString("satuan")));

        // open OPP per item (PR APPROVED, ada supplier, dalam 360 hari)
        Map<Integer, OppInfo> oppMap = queryOpenOpp(items);

        List<BufferRowResponse> rows = new ArrayList<>();
        for (ItemRow item : items) {
            OppInfo opp = oppMap.get(item.itemId);
            int openOpp = opp == null ? 0 : opp.count;
            String prCodes = opp == null ? "" : opp.codes;
            rows.add(new BufferRowResponse(item.code, item.name, item.jenis, item.stok,
                    item.buffer, item.satuan, openOpp, prCodes));
        }
        return new BufferResponse("", rows);
    }

    private Map<Integer, OppInfo> queryOpenOpp(List<ItemRow> items) {
        Map<Integer, OppInfo> map = new HashMap<>();
        if (items.isEmpty()) {
            return map;
        }
        StringBuilder inClause = new StringBuilder();
        Integer[] ids = new Integer[items.size()];
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                inClause.append(",");
            }
            inClause.append("?");
            ids[i] = items.get(i).itemId;
        }
        String sql = "select d.n_item_id as item_id, count(*) as cnt, "
                + "string_agg(distinct pr.v_pr_code, ',' order by pr.v_pr_code) as codes "
                + "from tb_purchase_request_detail d "
                + "join tb_purchase_request pr on pr.n_pr_id = d.n_pr_id "
                + "where pr.v_pr_status = 'APPROVED' and pr.n_supplier_id is not null "
                + "and date_part('day', now() - pr.d_whn_create) between 0 and 360 "
                + "and d.n_item_id in (" + inClause + ") group by d.n_item_id";
        jdbcTemplate.query(sql, (Object[]) ids,
                (resultSet, rowNum) -> {
                    map.put(resultSet.getInt("item_id"),
                            new OppInfo(resultSet.getInt("cnt"),
                                    resultSet.getString("codes")));
                    return null;
                });
        return map;
    }

    private static final class ItemRow {
        private final int itemId;
        private final String code;
        private final String name;
        private final String jenis;
        private final int stok;
        private final int buffer;
        private final String satuan;

        private ItemRow(int itemId, String code, String name, String jenis, int stok, int buffer,
                String satuan) {
            this.itemId = itemId;
            this.code = code;
            this.name = name;
            this.jenis = jenis;
            this.stok = stok;
            this.buffer = buffer;
            this.satuan = satuan;
        }
    }

    private static final class OppInfo {
        private final int count;
        private final String codes;

        private OppInfo(int count, String codes) {
            this.count = count;
            this.codes = codes;
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase();
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }
}
