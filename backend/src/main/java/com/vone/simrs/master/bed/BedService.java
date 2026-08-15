package com.vone.simrs.master.bed;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0020 (BED MASTER).
 * Mengikuti logika legacy {@code BedController} + {@code BedManagerImpl}
 * + {@code MsBedDAO} + {@code MsRoomDAO} + {@code MsTreatmentClassDAO}.
 */
@Service
public class BedService {

    private final JdbcTemplate jdbcTemplate;

    public BedService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar bed. Mengikuti {@code BedManagerImpl.getAllBed} yang menampilkan
     * RUANGAN, NAMA BED, KELAS TARIF, NO. KAMAR, KODE BED, HARGA, dan STATUS.
     */
    public List<BedRowResponse> getBeds() {
        String sql = "select b.n_bed_id, b.n_room_id, r.v_room_name, "
                + "coalesce(b.v_bed_desc, '') as bed_desc, "
                + "coalesce(t.v_tclass_desc, '') as tariff_class, "
                + "coalesce(b.v_bed_code, '') as bed_code, "
                + "coalesce(b.n_bed_price, 0) as bed_price, "
                + "coalesce(b.v_bed_active_status, '') as active_status, "
                + "b.n_coa, coa.v_acct_no, coa.v_acct_name, "
                + "b.n_tclass_id "
                + "from ms_bed b "
                + "join ms_room r on r.n_room_id = b.n_room_id "
                + "left join ms_treatment_class t on t.n_tclass_id = b.n_tclass_id "
                + "left join ms_coa coa on coa.n_coa_id = b.n_coa "
                + "order by r.v_room_name, b.v_bed_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new BedRowResponse(
                resultSet.getInt("n_bed_id"),
                resultSet.getInt("n_room_id"),
                resultSet.getString("v_room_name"),
                resultSet.getString("bed_desc"),
                resultSet.getString("tariff_class"),
                resultSet.getString("bed_code"),
                toDouble(resultSet.getObject("bed_price")),
                resultSet.getString("active_status"),
                toInteger(resultSet.getObject("n_coa")),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name"),
                toInteger(resultSet.getObject("n_tclass_id"))));
    }

    /**
     * Pencarian bed. Mengikuti {@code MsBedDAO.searchBeds} yang mencari
     * berdasarkan kelas tarif, kode bed, nama bed, kode kamar, dan nama hall.
     */
    public List<BedRowResponse> searchBeds(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return getBeds();
        }
        String like = "%" + normalized + "%";
        String sql = "select b.n_bed_id, b.n_room_id, r.v_room_name, "
                + "coalesce(b.v_bed_desc, '') as bed_desc, "
                + "coalesce(t.v_tclass_desc, '') as tariff_class, "
                + "coalesce(b.v_bed_code, '') as bed_code, "
                + "coalesce(b.n_bed_price, 0) as bed_price, "
                + "coalesce(b.v_bed_active_status, '') as active_status, "
                + "b.n_coa, coa.v_acct_no, coa.v_acct_name, "
                + "b.n_tclass_id "
                + "from ms_bed b "
                + "join ms_room r on r.n_room_id = b.n_room_id "
                + "left join ms_treatment_class t on t.n_tclass_id = b.n_tclass_id "
                + "left join ms_coa coa on coa.n_coa_id = b.n_coa "
                + "where upper(t.v_tclass_desc) like ? "
                + "or upper(b.v_bed_code) like ? "
                + "or upper(b.v_bed_desc) like ? "
                + "or upper(r.v_room_code) like ? "
                + "or upper(r.v_room_name) like ? "
                + "order by r.v_room_name, b.v_bed_code limit 100";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new BedRowResponse(
                resultSet.getInt("n_bed_id"),
                resultSet.getInt("n_room_id"),
                resultSet.getString("v_room_name"),
                resultSet.getString("bed_desc"),
                resultSet.getString("tariff_class"),
                resultSet.getString("bed_code"),
                toDouble(resultSet.getObject("bed_price")),
                resultSet.getString("active_status"),
                toInteger(resultSet.getObject("n_coa")),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name"),
                toInteger(resultSet.getObject("n_tclass_id"))), like, like, like, like, like);
    }

    /**
     * Pencarian kamar (room) untuk bandbox NAMA KAMAR.
     * Mengikuti {@code MsRoomDAO.searchRoomByName}.
     */
    public List<RoomOptionResponse> searchRooms(String name) {
        String keyword = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
        String sql = "select r.n_room_id, r.v_room_name, "
                + "coalesce(t.v_tclass_desc, '') as tariff_class "
                + "from ms_room r "
                + "left join ms_treatment_class t on t.n_tclass_id = r.n_tclass_id "
                + "where upper(r.v_room_name) like ? "
                + "order by r.v_room_name";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new RoomOptionResponse(
                resultSet.getInt("n_room_id"),
                resultSet.getString("v_room_name"),
                resultSet.getString("tariff_class")),
                "%" + keyword + "%");
    }

    /**
     * Opsi dropdown kelas tarif. Mengikuti
     * {@code TreatmentClassManagerImpl.getTClassForSelect}.
     */
    public List<TreatmentClassOptionResponse> getTreatmentClassOptions() {
        String sql = "select n_tclass_id, v_tclass_code, v_tclass_desc "
                + "from ms_treatment_class order by v_tclass_code";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new TreatmentClassOptionResponse(
                resultSet.getInt("n_tclass_id"),
                resultSet.getString("v_tclass_code"),
                resultSet.getString("v_tclass_desc")));
    }

    /**
     * Pencarian COA. Mengikuti {@code CoaDAO.getCoaByCodeAndName()}.
     * Kata kunci dicocokkan pada nomor akun ATAU nama akun sekaligus.
     */
    public List<CoaOptionResponse> searchCoa(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        String like = "%" + normalized.toUpperCase(Locale.ROOT) + "%";
        String sql = "select n_coa_id, v_acct_no, v_acct_name from ms_coa "
                + "where upper(v_acct_no) like ? or upper(v_acct_name) like ? "
                + "order by v_acct_no limit 100";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new CoaOptionResponse(
                resultSet.getInt("n_coa_id"),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")), like, like);
    }

    /**
     * Simpan / update bed. Mengikuti {@code BedController.doSaveAdd} dan
     * {@code doSaveModify}. Nama bed dibentuk dari
     * {@code HALLCODE-TCLASSCODE-ROOMCODE-BEDCODE}.
     */
    @Transactional
    public void save(BedSaveRequest request, String username) {
        Integer roomId = request.getRoomId();
        Integer tclassId = request.getTreatmentClassId();
        String bedCode = normalize(request.getBedCode());

        if (roomId == null) {
            throw new IllegalArgumentException("NAMA KAMAR harus dipilih.");
        }
        if (tclassId == null) {
            throw new IllegalArgumentException("KELAS TARIF harus dipilih.");
        }
        if (bedCode == null || bedCode.isEmpty()) {
            throw new IllegalArgumentException("KODE BED harus diisi.");
        }

        // Ambil data kamar untuk membentuk nama bed
        // (HALLCODE-TCLASSCODE-ROOMCODE-BEDCODE)
        String hallCode = jdbcTemplate.queryForObject(
                "select coalesce(h.v_hall_code, '') from ms_room r "
                        + "left join ms_hall h on h.n_hall_id = r.n_hall_id "
                        + "where r.n_room_id = ?",
                String.class,
                roomId);
        String tclassCode = jdbcTemplate.queryForObject(
                "select coalesce(v_tclass_code, '') from ms_treatment_class where n_tclass_id = ?",
                String.class,
                tclassId);
        String roomCode = jdbcTemplate.queryForObject(
                "select coalesce(v_room_code, '') from ms_room where n_room_id = ?",
                String.class,
                roomId);
        String bedDesc = hallCode + "-" + tclassCode + "-" + roomCode + "-" + bedCode;

        double bedPrice = valueOrZero(request.getBedPrice());
        String activeStatus = normalize(request.getActiveStatus());
        if (activeStatus == null || activeStatus.isEmpty()) {
            activeStatus = "A";
        }

        Integer id = request.getId();
        if (id == null) {
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_bed (n_bed_id, n_tclass_id, n_coa, n_room_id, "
                            + "v_bed_code, v_bed_desc, n_bed_price, v_who_create, "
                            + "d_whn_create, v_bed_status, v_bed_active_status) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, now(), '0', ?)",
                    id,
                    tclassId,
                    request.getCoaId(),
                    roomId,
                    bedCode,
                    bedDesc,
                    bedPrice,
                    normalizeActor(username),
                    activeStatus);
        } else {
            jdbcTemplate.update(
                    "update ms_bed set n_tclass_id = ?, n_coa = ?, n_room_id = ?, "
                            + "v_bed_code = ?, v_bed_desc = ?, n_bed_price = ?, "
                            + "v_bed_active_status = ?, v_who_change = ?, d_whn_change = now() "
                            + "where n_bed_id = ?",
                    tclassId,
                    request.getCoaId(),
                    roomId,
                    bedCode,
                    bedDesc,
                    bedPrice,
                    activeStatus,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus bed. Mengikuti {@code MsBedDAO.deleteById}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_bed where n_bed_id = ?", id);
        return affected > 0;
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_bed_n_bed_id_seq')",
                Integer.class);
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
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

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
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
