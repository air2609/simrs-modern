package com.vone.simrs.ward.beddisplay;

import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BedDisplayService {

    private final JdbcTemplate jdbcTemplate;

    public BedDisplayService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BedDisplayRowResponse> getActiveBeds() {
        return jdbcTemplate.query(
                "select bed.n_bed_id, "
                        + "coalesce(tclass.v_tclass_desc, '-') as v_tclass_desc, "
                        + "room.v_room_name, "
                        + "bed.v_bed_code, "
                        + "bed.v_bed_desc, "
                        + "bed.v_bed_status, "
                        + "bed.is_shown, "
                        + "bed.available_status "
                        + "from ms_bed bed "
                        + "left join ms_treatment_class tclass on tclass.n_tclass_id = bed.n_tclass_id "
                        + "join ms_room room on room.n_room_id = bed.n_room_id "
                        + "where bed.v_bed_active_status = 'A' "
                        + "order by tclass.v_tclass_desc, room.v_room_name, bed.v_bed_code",
                (resultSet, rowNum) -> {
                    String roomName = resultSet.getString("v_room_name");
                    String[] roomParts = roomName == null ? new String[0] : roomName.split("-");
                    String roomLabel = roomParts.length > 0 ? roomParts[0].trim() : roomName;
                    String roomNumber = roomParts.length > 2 ? roomParts[2].trim() : "";

                    String bedStatus = resultSet.getString("v_bed_status");
                    String condition = "1".equals(bedStatus) ? "Terisi" : "Kosong";

                    String shown = resultSet.getString("is_shown");
                    boolean isShown = "Y".equalsIgnoreCase(shown);

                    String availableStatus = resultSet.getString("available_status");
                    if (availableStatus == null || availableStatus.trim().isEmpty()) {
                        availableStatus = "A";
                    }

                    return new BedDisplayRowResponse(
                            resultSet.getInt("n_bed_id"),
                            resultSet.getString("v_tclass_desc"),
                            roomLabel,
                            roomNumber,
                            resultSet.getString("v_bed_code"),
                            resultSet.getString("v_bed_desc"),
                            condition,
                            isShown,
                            availableStatus);
                });
    }

    @Transactional
    public void saveBulk(List<BedDisplaySaveRequest> requests, String username) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        for (BedDisplaySaveRequest request : requests) {
            if (request.getBedId() == null) {
                continue;
            }
            updateBed(request, username);
        }
    }

    private void updateBed(BedDisplaySaveRequest request, String username) {
        Integer bedId = request.getBedId();
        String shown = request.isShown() ? "Y" : "N";
        String availableStatus = normalizeAvailableStatus(request.getAvailableStatus());

        // Hanya update available_status jika bed berstatus kosong ("0"), sesuai logika
        // legacy.
        String bedStatus = getBedStatus(bedId);
        if ("0".equals(bedStatus)) {
            jdbcTemplate.update(
                    "update ms_bed set is_shown = ?, available_status = ?, v_who_change = ?, d_whn_change = now() "
                            + "where n_bed_id = ?",
                    shown,
                    availableStatus,
                    normalizeActor(username),
                    bedId);
        } else {
            jdbcTemplate.update(
                    "update ms_bed set is_shown = ?, v_who_change = ?, d_whn_change = now() "
                            + "where n_bed_id = ?",
                    shown,
                    normalizeActor(username),
                    bedId);
        }
    }

    private String getBedStatus(Integer bedId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select v_bed_status from ms_bed where n_bed_id = ?",
                    String.class,
                    bedId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data bed tidak ditemukan.");
        }
    }

    private String normalizeAvailableStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "A";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"A".equals(normalized) && !"B".equals(normalized) && !"C".equals(normalized)) {
            throw new IllegalArgumentException("Status bed tidak valid.");
        }
        return normalized;
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
