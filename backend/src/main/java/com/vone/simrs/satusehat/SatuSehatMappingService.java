package com.vone.simrs.satusehat;

import com.vone.simrs.auth.LegacyAuthService;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Mengelola mapping data master lokal ke ID/kode SATUSEHAT:
 * <ul>
 *   <li>ms_unit.ss_location_id : unit/poliklinik -> Location ID</li>
 *   <li>ms_doctor.ss_practitioner_id : dokter -> Practitioner ID</li>
 *   <li>ms_staff.nik : NIK tenaga medis</li>
 *   <li>ms_item.v_kfa_code : item obat -> kode KFA</li>
 *   <li>ms_lab_treatment_detil.v_loinc_code : analyte lab -> kode LOINC</li>
 * </ul>
 */
@Service
public class SatuSehatMappingService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public SatuSehatMappingService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ===================== Unit -> Location =====================

    public List<SatuSehatMappingRow> listUnits() {
        return jdbcTemplate.query(
                "select n_unit_id, v_unit_code, v_unit_name, ss_location_id "
                        + "from ms_unit order by v_unit_name",
                (resultSet, rowNum) -> new SatuSehatMappingRow(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        resultSet.getString("ss_location_id")));
    }

    public void setUnitLocation(int unitId, String locationId) {
        int updated = jdbcTemplate.update(
                "update ms_unit set ss_location_id = ? where n_unit_id = ?",
                normalize(locationId), unitId);
        if (updated == 0) {
            throw new IllegalArgumentException("Unit tidak ditemukan: " + unitId);
        }
    }

    // ===================== Bangsal (hall) -> Location =====================

    public List<SatuSehatMappingRow> listHalls() {
        return jdbcTemplate.query(
                "select n_hall_id, v_hall_code, v_hall_name, ss_location_id "
                        + "from ms_hall order by v_hall_name",
                (resultSet, rowNum) -> new SatuSehatMappingRow(
                        resultSet.getInt("n_hall_id"),
                        resultSet.getString("v_hall_code"),
                        resultSet.getString("v_hall_name"),
                        resultSet.getString("ss_location_id")));
    }

    public void setHallLocation(int hallId, String locationId) {
        int updated = jdbcTemplate.update(
                "update ms_hall set ss_location_id = ? where n_hall_id = ?",
                normalize(locationId), hallId);
        if (updated == 0) {
            throw new IllegalArgumentException("Bangsal tidak ditemukan: " + hallId);
        }
    }

    // ===================== Dokter -> Practitioner =====================

    public List<SatuSehatMappingRow> listDoctors() {
        return jdbcTemplate.query(
                "select d.n_doctor_id, s.v_staff_code, s.v_staff_name, d.ss_practitioner_id "
                        + "from ms_doctor d join ms_staff s on s.n_staff_id = d.n_staff_id "
                        + "order by s.v_staff_name",
                (resultSet, rowNum) -> new SatuSehatMappingRow(
                        resultSet.getInt("n_doctor_id"),
                        resultSet.getString("v_staff_code"),
                        resultSet.getString("v_staff_name"),
                        resultSet.getString("ss_practitioner_id")));
    }

    public void setDoctorPractitioner(int doctorId, String practitionerId) {
        int updated = jdbcTemplate.update(
                "update ms_doctor set ss_practitioner_id = ? where n_doctor_id = ?",
                normalize(practitionerId), doctorId);
        if (updated == 0) {
            throw new IllegalArgumentException("Dokter tidak ditemukan: " + doctorId);
        }
    }

    public void setStaffNik(int staffId, String nik) {
        int updated = jdbcTemplate.update(
                "update ms_staff set nik = ? where n_staff_id = ?",
                normalize(nik), staffId);
        if (updated == 0) {
            throw new IllegalArgumentException("Staff tidak ditemukan: " + staffId);
        }
    }

    // ===================== Item obat -> KFA =====================

    public List<SatuSehatItemMappingRow> listItems(String keyword) {
        String like = StringUtils.hasText(keyword) ? "%" + keyword.trim().toUpperCase() + "%" : "%";
        return jdbcTemplate.query(
                "select i.n_item_id, i.v_item_code, i.v_item_name, i.v_kfa_code, i.v_kfa_display, "
                        + "i.v_ucum_code, i.ss_medication_id "
                        + "from ms_item i "
                        + "join ms_item_group g on g.n_item_group_id = i.n_item_group_id "
                        + "where g.v_item_group_code = 'OBT01' "
                        + "and (upper(i.v_item_code) like ? or upper(i.v_item_name) like ?) "
                        + "order by i.v_item_name limit 200",
                (resultSet, rowNum) -> new SatuSehatItemMappingRow(
                        resultSet.getInt("n_item_id"),
                        resultSet.getString("v_item_code"),
                        resultSet.getString("v_item_name"),
                        resultSet.getString("v_kfa_code"),
                        resultSet.getString("v_kfa_display"),
                        resultSet.getString("v_ucum_code"),
                        StringUtils.hasText(resultSet.getString("ss_medication_id"))),
                like, like);
    }

    public void setItemKfa(int itemId, String kfaCode, String kfaDisplay, String ucumCode) {
        String normalizedCode = normalize(kfaCode);
        int updated = jdbcTemplate.update(
                "update ms_item set v_kfa_code = ?, v_kfa_display = ?, v_ucum_code = ?, "
                        + "ss_medication_id = case when coalesce(v_kfa_code, '') <> coalesce(?, '') "
                        + "then null else ss_medication_id end "
                        + "where n_item_id = ?",
                normalizedCode, normalize(kfaDisplay), normalize(ucumCode), normalizedCode, itemId);
        if (updated == 0) {
            throw new IllegalArgumentException("Item tidak ditemukan: " + itemId);
        }
    }

    // ===================== Lab -> LOINC =====================

    public List<SatuSehatLabMappingRow> listLabAnalytes(String keyword) {
        String like = StringUtils.hasText(keyword) ? "%" + keyword.trim().toUpperCase() + "%" : "%";
        return jdbcTemplate.query(
                "select d.n_lab_detil_id, d.n_treatment_id, t.v_treatment_code, t.v_treatment_name, "
                        + "d.v_detail_name, d.v_quantify, d.v_loinc_code, d.v_loinc_display, d.v_ucum_code "
                        + "from ms_lab_treatment_detil d "
                        + "left join ms_treatment t on t.n_treatment_id = d.n_treatment_id "
                        + "where upper(coalesce(d.v_detail_name, '')) like ? "
                        + "or upper(coalesce(t.v_treatment_name, '')) like ? "
                        + "order by t.v_treatment_name, d.v_detail_name limit 200",
                (resultSet, rowNum) -> new SatuSehatLabMappingRow(
                        resultSet.getInt("n_lab_detil_id"),
                        (Integer) resultSet.getObject("n_treatment_id"),
                        resultSet.getString("v_treatment_code"),
                        resultSet.getString("v_treatment_name"),
                        resultSet.getString("v_detail_name"),
                        resultSet.getString("v_quantify"),
                        resultSet.getString("v_loinc_code"),
                        resultSet.getString("v_loinc_display"),
                        resultSet.getString("v_ucum_code")),
                like, like);
    }

    public void setLabLoinc(int detailId, String loincCode, String loincDisplay, String ucumCode) {
        int updated = jdbcTemplate.update(
                "update ms_lab_treatment_detil set v_loinc_code = ?, v_loinc_display = ?, v_ucum_code = ? "
                        + "where n_lab_detil_id = ?",
                normalize(loincCode), normalize(loincDisplay), normalize(ucumCode), detailId);
        if (updated == 0) {
            throw new IllegalArgumentException("Analyte lab tidak ditemukan: " + detailId);
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
