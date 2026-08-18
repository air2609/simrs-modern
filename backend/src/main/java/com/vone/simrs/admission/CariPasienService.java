package com.vone.simrs.admission;

import com.vone.simrs.auth.LegacyAuthService;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0005 (PENCARIAN PASIEN RAWAT INAP / CariPasien.zul).
 *
 * <p>
 * Migrasi dari legacy {@code PencarianPasientRanapController} +
 * {@code HallManagerImpl.searchHall()} + {@code PatientManagerImpl.searchPatient()}.
 */
@Service
public class CariPasienService {

    private static final int PATIENT_TYPE_BPJS = 8;

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public CariPasienService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Cari ruangan (hall). Migrasi dari {@code MsHallDAO.searchHall(hallCode, hallName)}.
     */
    public List<CariPasienHallResponse> searchHalls(String code, String name) {
        return jdbcTemplate.query(
                "select n_hall_id, v_hall_code, v_hall_name from ms_hall "
                        + "where v_hall_code like ? and v_hall_name like ? "
                        + "order by v_hall_name",
                (resultSet, rowNum) -> new CariPasienHallResponse(
                        resultSet.getInt("n_hall_id"),
                        resultSet.getString("v_hall_code"),
                        resultSet.getString("v_hall_name")),
                like(normalizeOptionalUpper(code)),
                like(normalizeOptionalUpper(name)));
    }

    /**
     * Cari pasien rawat inap yang sedang dirawat (reg aktif + bed occupancy
     * belum check-out). Migrasi dari {@code MsPatientDAO.searchRanapPatient(...)}.
     */
    public List<CariPasienPatientResponse> searchRanapPatients(String mrCode, String patientName,
            String address, String hall, String doctor) {
        return jdbcTemplate.query(
                "select mr.v_mr_code as kode, pat.v_patient_name as namaPasien, "
                        + "pat.n_patient_type_id as tipePasien, pat.v_patient_main_addr as alamat, "
                        + "hall.v_hall_name as ruangan, bed.v_bed_desc as namabed, "
                        + "staff.v_staff_name as doctor, "
                        + "date_part('day', now() - reg.d_registration_date) as durasi "
                        + "from ms_patient pat "
                        + "join tb_medical_record mr on mr.n_patient_id = pat.n_patient_id "
                        + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                        + "join ms_staff staff on staff.n_staff_id = reg.n_staff_id "
                        + "join tb_bed_occupancy boc on boc.n_reg_primary_id = reg.n_reg_id "
                        + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                        + "join ms_room room on room.n_room_id = bed.n_room_id "
                        + "join ms_hall hall on hall.n_hall_id = room.n_hall_id "
                        + "where reg.reg_status = 1 and boc.d_check_out_time is null "
                        + "and mr.v_mr_code like ? and pat.v_patient_name like ? "
                        + "and pat.v_patient_main_addr like ? and hall.v_hall_name like ? "
                        + "and staff.v_staff_name like ?",
                (resultSet, rowNum) -> {
                    Integer typeId = getNullableInteger(resultSet, "tipePasien");
                    return new CariPasienPatientResponse(
                            resultSet.getString("kode"),
                            resultSet.getString("namaPasien"),
                            typeId,
                            typeId != null && typeId == PATIENT_TYPE_BPJS ? "BPJS" : "NON BPJS",
                            resultSet.getString("alamat"),
                            resultSet.getString("ruangan"),
                            resultSet.getString("namabed"),
                            resultSet.getString("doctor"),
                            toInteger(resultSet.getObject("durasi")));
                },
                like(normalizeOptionalUpper(mrCode)),
                like(normalizeOptionalUpper(patientName)),
                like(normalizeOptionalUpper(address)),
                like(normalizeOptionalUpper(hall)),
                like(normalizeOptionalUpper(doctor)));
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String like(String value) {
        return "%" + (value != null ? value : "") + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
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
}
