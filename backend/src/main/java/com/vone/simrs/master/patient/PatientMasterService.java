package com.vone.simrs.master.patient;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SCM0011 (FORM DATA PASIEN / ms_patient.zul).
 *
 * <p>
 * Migrasi dari legacy {@code PatientController} + {@code PatientManagerImpl}
 * + {@code MsPatientDAO.save()/searchPatient()} — CRUD data master pasien:
 * pencarian, detail, simpan/ubah (create + generate no MR baru bila pasien baru).
 */
@Service
public class PatientMasterService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public PatientMasterService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Dropdown statis + list DB, migrasi {@code PatientController.init()}. */
    public PatientMasterMastersResponse getMasters() {
        List<OptionResponse> religions = new ArrayList<>();
        religions.add(new OptionResponse("ISLAM"));
        religions.add(new OptionResponse("PROTESTAN"));
        religions.add(new OptionResponse("KATOLIK"));
        religions.add(new OptionResponse("HINDU"));
        religions.add(new OptionResponse("BUDHA"));

        List<OptionResponse> nationalities = new ArrayList<>();
        nationalities.add(new OptionResponse("WNI"));
        nationalities.add(new OptionResponse("WNA"));

        List<OptionResponse> maritalStatuses = new ArrayList<>();
        maritalStatuses.add(new OptionResponse("Belum Menikah"));
        maritalStatuses.add(new OptionResponse("Menikah"));
        maritalStatuses.add(new OptionResponse("Duda"));
        maritalStatuses.add(new OptionResponse("Janda"));

        List<OptionResponse> educations = new ArrayList<>();
        for (String edu : Arrays.asList("SD/Sederajat", "SMP/Sederajat", "SMA/Sederajat",
                "D1", "D2", "D3", "S1", "S2", "S3")) {
            educations.add(new OptionResponse(edu));
        }

        List<OptionResponse> jobTypes = new ArrayList<>();
        jobTypes.add(new OptionResponse("Pegawai Negri"));
        jobTypes.add(new OptionResponse("Swasta"));
        jobTypes.add(new OptionResponse("Pensiunan"));
        jobTypes.add(new OptionResponse("Ibu Rumah Tangga"));

        List<OptionResponse> priorities = new ArrayList<>();
        priorities.add(new OptionResponse("High"));
        priorities.add(new OptionResponse("Med"));
        priorities.add(new OptionResponse("Low"));

        List<OptionResponse> patientTypes = queryOptions(
                "select n_patient_type_id, v_tpatient_desc from ms_patient_type order by n_patient_type_id");
        List<OptionResponse> provinces = queryOptions(
                "select n_province_id, v_province_name from ms_province order by v_province_name");
        List<OptionResponse> regencies = queryOptions(
                "select n_regency_id, v_regency_name from ms_regency order by v_regency_name");
        List<OptionResponse> subDistricts = queryOptions(
                "select n_subdistrict_id, v_sub_district_name from ms_sub_district order by v_sub_district_name");
        List<OptionResponse> villages = queryOptions(
                "select n_village_id, v_village_name from ms_village order by v_village_name");

        return new PatientMasterMastersResponse(religions, nationalities, maritalStatuses,
                educations, jobTypes, priorities, patientTypes, provinces, regencies,
                subDistricts, villages);
    }

    /** Pencarian pasien, migrasi {@code MsPatientDAO.searchPatient()}. */
    public List<PatientSearchRowResponse> searchPatients(String mrCode, String name, String address,
            String dob) {
        StringBuilder sql = new StringBuilder();
        sql.append("select mr.v_mr_code as mr, p.v_patient_name as nama, ")
                .append("p.d_patient_dob as tgl, p.v_patient_main_addr as alamat ")
                .append("from tb_medical_record mr join ms_patient p on p.n_patient_id = mr.n_patient_id ")
                .append("where mr.v_mr_code like ? and p.v_patient_name like ? ")
                .append("and p.v_patient_main_addr like ? ");
        List<Object> params = new ArrayList<>();
        params.add(likeUpper(mrCode));
        params.add(likeUpper(name));
        params.add(likeUpper(address));
        if (hasText(dob)) {
            sql.append("and p.d_patient_dob = ? ");
            params.add(Date.valueOf(LocalDate.parse(dob)));
        }
        sql.append("limit 100");

        return jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> new PatientSearchRowResponse(
                        resultSet.getString("mr"),
                        resultSet.getString("nama"),
                        toDisplayDate(resultSet.getDate("tgl")),
                        resultSet.getString("alamat")));
    }

    /** Detail pasien utk isi form, migrasi {@code PatientManagerImpl.getPatientDetil()}. */
    public PatientDetailResponse getDetail(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        String code = toMrCode(mrCode);
        List<PatientDetailResponse> rows = jdbcTemplate.query(
                "select mr.v_mr_code as mr, p.n_patient_type_id as tipe, "
                        + "p.n_village_id as kelurahan, p.n_subdistrict_id as kecamatan, "
                        + "p.n_regency_id as kabupaten, p.n_province_id as propinsi, "
                        + "p.v_patient_name, p.v_patient_gender, p.d_patient_dob, "
                        + "p.v_patient_religion, p.v_patient_nationality, p.v_patient_marital_status, "
                        + "p.v_patient_main_addr, p.v_patient_main_rt_rw, p.v_patient_main_ph_no, "
                        + "p.v_patient_alt_addr, p.v_patient_alt_rt_rw, p.v_patient_alt_ph_no, "
                        + "p.v_patient_edu, p.v_patient_job_type, p.v_patient_priority "
                        + "from tb_medical_record mr "
                        + "join ms_patient p on p.n_patient_id = mr.n_patient_id "
                        + "where mr.v_mr_code = ?",
                (resultSet, rowNum) -> {
                    String rtRw = resultSet.getString("v_patient_main_rt_rw");
                    String altRtRw = resultSet.getString("v_patient_alt_rt_rw");
                    return new PatientDetailResponse(
                            resultSet.getString("mr"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_gender"),
                            toDisplayDate(resultSet.getDate("d_patient_dob")),
                            resultSet.getString("v_patient_religion"),
                            resultSet.getString("v_patient_nationality"),
                            resultSet.getString("v_patient_marital_status"),
                            resultSet.getString("v_patient_main_addr"),
                            splitRt(rtRw), splitRw(rtRw),
                            getNullableInteger(resultSet, "kelurahan"),
                            getNullableInteger(resultSet, "kecamatan"),
                            getNullableInteger(resultSet, "kabupaten"),
                            getNullableInteger(resultSet, "propinsi"),
                            resultSet.getString("v_patient_main_ph_no"),
                            resultSet.getString("v_patient_alt_addr"),
                            splitRt(altRtRw), splitRw(altRtRw),
                            resultSet.getString("v_patient_alt_ph_no"),
                            resultSet.getString("v_patient_edu"),
                            resultSet.getString("v_patient_job_type"),
                            getNullableInteger(resultSet, "tipe"),
                            resultSet.getString("v_patient_priority"));
                },
                code);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("ADMISSION MR NOT FOUND");
        }
        return rows.get(0);
    }

    /**
     * Simpan/ubah pasien. Migrasi {@code PatientController.doSaveAdd()}
     * + {@code MsPatientDAO.save()} — pasien baru diberi no MR baru.
     */
    public PatientSaveResultResponse save(PatientSaveRequest request, String username) {
        if (!hasText(request.getNamaPasien())) {
            throw new IllegalArgumentException("NAMA HARUS DI ISI!");
        }
        if (request.getTglLahir() == null) {
            throw new IllegalArgumentException("TANGGAL LAHIR HARUS DI ISI!");
        }
        if (!hasText(request.getAlamat())) {
            throw new IllegalArgumentException("ALAMAT HARUS DI ISI!");
        }

        String normalizedUser = username == null ? "" : username.trim().toUpperCase();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        boolean modify = hasText(request.getMrCode());

        Integer patientId;
        Integer medicalRecordId;
        String mrCode;
        if (modify) {
            PatientIdRow row = findIdsByMrCode(toMrCode(request.getMrCode()));
            if (row == null) {
                throw new IllegalArgumentException("No.MR tidak ditemukan, Mohon Diganti Dengan yang Lain..!");
            }
            patientId = row.patientId;
            medicalRecordId = row.medicalRecordId;
            mrCode = row.mrCode;
        } else {
            patientId = nextSequenceValue("ms_patient_n_patient_id_seq");
            medicalRecordId = nextSequenceValue("tb_medical_record_n_mr_id_seq");
            mrCode = toMrCode(medicalRecordId);
        }

        jdbcTemplate.update(
                "insert into ms_patient (n_patient_id, n_patient_type_id, n_village_id, "
                        + "n_subdistrict_id, n_regency_id, n_province_id, v_patient_name, "
                        + "v_patient_marital_status, v_patient_gender, d_patient_dob, "
                        + "v_patient_religion, v_patient_edu, v_patient_job_type, "
                        + "v_patient_main_addr, v_patient_main_rt_rw, v_patient_main_ph_no, "
                        + "v_patient_alt_addr, v_patient_alt_rt_rw, v_patient_alt_ph_no, "
                        + "v_patient_nationality, v_patient_priority, v_who_create, d_whn_create, "
                        + "v_who_change, d_whn_change) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "on conflict (n_patient_id) do update set "
                        + "n_patient_type_id = excluded.n_patient_type_id, "
                        + "n_village_id = excluded.n_village_id, "
                        + "n_subdistrict_id = excluded.n_subdistrict_id, "
                        + "n_regency_id = excluded.n_regency_id, "
                        + "n_province_id = excluded.n_province_id, "
                        + "v_patient_name = excluded.v_patient_name, "
                        + "v_patient_marital_status = excluded.v_patient_marital_status, "
                        + "v_patient_gender = excluded.v_patient_gender, "
                        + "d_patient_dob = excluded.d_patient_dob, "
                        + "v_patient_religion = excluded.v_patient_religion, "
                        + "v_patient_edu = excluded.v_patient_edu, "
                        + "v_patient_job_type = excluded.v_patient_job_type, "
                        + "v_patient_main_addr = excluded.v_patient_main_addr, "
                        + "v_patient_main_rt_rw = excluded.v_patient_main_rt_rw, "
                        + "v_patient_main_ph_no = excluded.v_patient_main_ph_no, "
                        + "v_patient_alt_addr = excluded.v_patient_alt_addr, "
                        + "v_patient_alt_rt_rw = excluded.v_patient_alt_rt_rw, "
                        + "v_patient_alt_ph_no = excluded.v_patient_alt_ph_no, "
                        + "v_patient_nationality = excluded.v_patient_nationality, "
                        + "v_patient_priority = excluded.v_patient_priority, "
                        + "v_who_change = excluded.v_who_change, "
                        + "d_whn_change = excluded.d_whn_change",
                patientId,
                request.getTipePasienId(),
                request.getKelurahanId(),
                request.getKecamatanId(),
                request.getKabupatenId(),
                request.getPropinsiId(),
                upper(request.getNamaPasien()),
                optional(request.getStatusKawin()),
                normalizeGender(request.getJenisKelamin()),
                Date.valueOf(request.getTglLahir()),
                optional(request.getAgama()),
                optional(request.getPendidikan()),
                optional(request.getJenisPekerjaan()),
                upper(request.getAlamat()),
                mergeRtRw(request.getRt(), request.getRw()),
                optional(request.getNoTelp()),
                optionalUpper(request.getAlamatAlternatif()),
                mergeRtRw(request.getRt1(), request.getRw1()),
                optional(request.getNoTelpAlt()),
                optional(request.getWargaNegara()),
                optional(request.getPrioritas()),
                normalizedUser, now, normalizedUser, now);

        if (!modify) {
            jdbcTemplate.update(
                    "insert into tb_medical_record (n_mr_id, n_patient_id, v_mr_code, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?)",
                    medicalRecordId, patientId, mrCode, normalizedUser, now);
        }

        return new PatientSaveResultResponse(modify,
                modify ? "common.modify.success" : "common.add.success", mrCode);
    }

    private PatientIdRow findIdsByMrCode(String mrCode) {
        List<PatientIdRow> rows = jdbcTemplate.query(
                "select n_mr_id, n_patient_id, v_mr_code from tb_medical_record where v_mr_code = ?",
                (resultSet, rowNum) -> new PatientIdRow(resultSet.getInt("n_mr_id"),
                        resultSet.getInt("n_patient_id"), resultSet.getString("v_mr_code")),
                mrCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<OptionResponse> queryOptions(String sql) {
        return jdbcTemplate.query(sql,
                (resultSet, rowNum) -> new OptionResponse(resultSet.getInt(1),
                        resultSet.getString(2)));
    }

    private Integer nextSequenceValue(String sequenceName) {
        Number number = jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')",
                Number.class);
        return number == null ? null : number.intValue();
    }

    /** Migrasi {@code MedisafeUtil.convertToMrCode()}. */
    public String toMrCode(Integer id) {
        String value = String.valueOf(id);
        if (value.length() == 1) {
            return "00-00-0" + value;
        }
        if (value.length() == 2) {
            return "00-00-" + value;
        }
        if (value.length() == 3) {
            return "00-0" + value.substring(0, 1) + "-" + value.substring(1);
        }
        if (value.length() == 4) {
            return "00-" + value.substring(0, 2) + "-" + value.substring(2);
        }
        if (value.length() == 5) {
            return "0" + value.substring(0, 1) + "-" + value.substring(1, 3) + "-" + value.substring(3);
        }
        return value.substring(0, 2) + "-" + value.substring(2, 4) + "-" + value.substring(4, 6);
    }

    /** Normalisasi input 6/8 digit menjadi format MR code (ex. 111213 -> 11-12-13). */
    public String toMrCode(String input) {
        if (!hasText(input)) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.length() == 6 && trimmed.chars().allMatch(Character::isDigit)) {
            return toMrCode(Integer.valueOf(trimmed));
        }
        return trimmed.toUpperCase();
    }

    private String likeUpper(String value) {
        return "%" + (value == null ? "" : value.trim().toUpperCase()) + "%";
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String optionalUpper(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String optional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "F";
        }
        return gender.trim().equalsIgnoreCase("M") ? "M" : "F";
    }

    private String mergeRtRw(String rt, String rw) {
        if ((rt == null || rt.trim().isEmpty()) || (rw == null || rw.trim().isEmpty())) {
            return null;
        }
        return rt.trim() + "/" + rw.trim();
    }

    private String splitRt(String rtRw) {
        if (rtRw == null || !rtRw.contains("/")) {
            return "";
        }
        return rtRw.split("/")[0];
    }

    private String splitRw(String rtRw) {
        if (rtRw == null || !rtRw.contains("/")) {
            return "";
        }
        return rtRw.split("/")[1];
    }

    private String toDisplayDate(Date date) {
        return date == null ? "" : date.toLocalDate().format(DATE_DISPLAY);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static final class PatientIdRow {
        private final int medicalRecordId;
        private final int patientId;
        private final String mrCode;

        private PatientIdRow(int medicalRecordId, int patientId, String mrCode) {
            this.medicalRecordId = medicalRecordId;
            this.patientId = patientId;
            this.mrCode = mrCode;
        }
    }
}
