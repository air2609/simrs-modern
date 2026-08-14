package com.vone.simrs.ward.bedinfo;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BedInfoService {

    private final JdbcTemplate jdbcTemplate;

    public BedInfoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Menghasilkan ringkasan informasi kamar ranap (SC0072) yang dikelompokkan
     * berdasarkan kelas tarif dan ruangan (hall). Logika mengikuti legacy
     * {@code MsBedDAO.getBedInfo()} + {@code getBedTerisi} + {@code getBedBooked}
     * + {@code getBedInservice}, namun digabung menjadi satu query agregat.
     */
    public List<BedInfoRowResponse> getBedInfo() {
        String sql = "select t.v_tclass_desc as kelas, "
                + "h.v_hall_name as ruangan, "
                + "count(1) as total, "
                + "count(*) filter (where b.v_bed_status = '1') as terisi, "
                + "count(*) filter (where b.available_status = 'B') as booked, "
                + "count(*) filter (where b.available_status = 'C') as inservice "
                + "from ms_bed b "
                + "join ms_treatment_class t on t.n_tclass_id = b.n_tclass_id "
                + "join ms_room r on r.n_room_id = b.n_room_id "
                + "join ms_hall h on h.n_hall_id = r.n_hall_id "
                + "where b.v_bed_active_status = 'A' and b.is_shown = 'Y' "
                + "group by kelas, ruangan "
                + "order by kelas";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            int total = resultSet.getInt("total");
            int occupied = resultSet.getInt("terisi");
            int booked = resultSet.getInt("booked");
            int inService = resultSet.getInt("inservice");
            int empty = total - occupied - booked - inService;

            return new BedInfoRowResponse(
                    resultSet.getString("kelas"),
                    resultSet.getString("ruangan"),
                    total,
                    occupied,
                    booked,
                    inService,
                    empty);
        });
    }
}
