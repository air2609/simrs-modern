package com.vone.simrs.master.room;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0019 (FORM KAMAR / ROOM MASTER).
 * Mengikuti logika legacy {@code RoomController} + {@code RoomManagerImpl}
 * + {@code MsRoomDAO} + {@code MsHallDAO}.
 */
@Service
public class RoomService {

    private final JdbcTemplate jdbcTemplate;

    public RoomService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar kamar. Mengikuti {@code RoomController.getDataRoomList} yang
     * menampilkan RUANGAN, KELAS TARIF, NO. KAMAR, dan NAMA KAMAR.
     * Nama kamar berformat {@code HALLNAME-KELAS-NO}.
     */
    public List<RoomRowResponse> getRooms() {
        String sql = "select r.n_room_id, "
                + "coalesce(h.v_hall_name, '') as hall_name, "
                + "coalesce(t.v_tclass_desc, '') as tariff_class, "
                + "coalesce(r.v_room_code, '') as room_code, "
                + "coalesce(r.v_room_name, '') as room_name "
                + "from ms_room r "
                + "left join ms_hall h on h.n_hall_id = r.n_hall_id "
                + "left join ms_treatment_class t on t.n_tclass_id = r.n_tclass_id "
                + "order by r.v_room_name";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new RoomRowResponse(
                resultSet.getInt("n_room_id"),
                resultSet.getString("hall_name"),
                resultSet.getString("tariff_class"),
                resultSet.getString("room_code"),
                resultSet.getString("room_name")));
    }

    /**
     * Pencarian ruangan (hall) untuk bandbox NAMA RUANGAN.
     * Mengikuti {@code MsHallDAO.searchHall} yang mencari berdasarkan nama
     * dan menampilkan RUANGAN + KELAS TARIF.
     */
    public List<HallOptionResponse> searchHalls(String name) {
        String keyword = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
        String sql = "select h.n_hall_id, h.v_hall_name, "
                + "coalesce(t.v_tclass_desc, '') as tariff_class "
                + "from ms_hall h "
                + "left join ms_treatment_class t on t.n_tclass_id = h.n_tclass_id "
                + "where upper(h.v_hall_name) like ? "
                + "order by h.v_hall_name";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new HallOptionResponse(
                resultSet.getInt("n_hall_id"),
                resultSet.getString("v_hall_name"),
                resultSet.getString("tariff_class")),
                "%" + keyword + "%");
    }

    /**
     * Cek apakah nama kamar sudah dipakai. Mengikuti
     * {@code RoomManagerImpl.isRoomAlreadyExist}.
     */
    public boolean isRoomNameExists(String roomName) {
        if (roomName == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_room where v_room_name = ?",
                Integer.class,
                roomName.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update kamar. Mengikuti {@code RoomController.doSaveAdd}
     * dan {@code doSaveModify}. Nama kamar dibentuk dari
     * {@code HALLNAME-KELAS-NO}.
     */
    @Transactional
    public void save(RoomSaveRequest request, String username) {
        Integer hallId = request.getHallId();
        String roomCode = normalize(request.getRoomCode());

        if (hallId == null) {
            throw new IllegalArgumentException("NAMA RUANGAN harus dipilih.");
        }
        if (roomCode == null || roomCode.isEmpty()) {
            throw new IllegalArgumentException("NOMOR KAMAR harus diisi.");
        }

        // Ambil data hall untuk membentuk nama kamar (HALLNAME-KELAS-NO)
        String hallName = jdbcTemplate.queryForObject(
                "select v_hall_name from ms_hall where n_hall_id = ?",
                String.class,
                hallId);
        if (hallName == null) {
            throw new IllegalArgumentException("Ruangan tidak ditemukan.");
        }
        String tclassCode = jdbcTemplate.queryForObject(
                "select coalesce(v_tclass_code, '') from ms_hall where n_hall_id = ?",
                String.class,
                hallId);
        String roomName = hallName + "-" + tclassCode + "-" + roomCode;

        Integer id = request.getId();
        if (id == null) {
            // Cek duplikasi nama kamar (legacy: isRoomAlreadyExist)
            if (isRoomNameExists(roomName)) {
                throw new IllegalArgumentException("NAMA KAMAR " + roomName
                        + " SUDAH ADA. SILAHKAN GANTI DENGAN YANG LAIN!");
            }
            id = nextId();
            jdbcTemplate.update(
                    "insert into ms_room (n_room_id, n_hall_id, n_tclass_id, n_ward_id, "
                            + "v_room_code, v_room_name, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, ?, now())",
                    id,
                    hallId,
                    tclassIdOf(hallId),
                    wardIdOf(hallId),
                    roomCode,
                    roomName,
                    normalizeActor(username));
        } else {
            // Cek duplikasi nama kamar selain id ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_room where v_room_name = ? and n_room_id <> ?",
                    Integer.class,
                    roomName,
                    id);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("NAMA KAMAR " + roomName
                        + " SUDAH ADA. SILAHKAN GANTI DENGAN YANG LAIN!");
            }
            jdbcTemplate.update(
                    "update ms_room set n_hall_id = ?, n_tclass_id = ?, n_ward_id = ?, "
                            + "v_room_code = ?, v_room_name = ?, v_who_change = ?, "
                            + "d_whn_change = now() where n_room_id = ?",
                    hallId,
                    tclassIdOf(hallId),
                    wardIdOf(hallId),
                    roomCode,
                    roomName,
                    normalizeActor(username),
                    id);
        }
    }

    /**
     * Hapus kamar. Mengikuti {@code MsRoomDAO.deleteById}.
     */
    @Transactional
    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update("delete from ms_room where n_room_id = ?", id);
        return affected > 0;
    }

    private Integer tclassIdOf(Integer hallId) {
        return jdbcTemplate.queryForObject(
                "select n_tclass_id from ms_hall where n_hall_id = ?",
                Integer.class,
                hallId);
    }

    private Integer wardIdOf(Integer hallId) {
        return jdbcTemplate.queryForObject(
                "select n_ward_id from ms_hall where n_hall_id = ?",
                Integer.class,
                hallId);
    }

    private Integer nextId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_room_n_room_id_seq')",
                Integer.class);
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
