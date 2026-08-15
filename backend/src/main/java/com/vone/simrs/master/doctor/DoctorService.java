package com.vone.simrs.master.doctor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0030 (MASTER DOKTER).
 * Mengikuti logika legacy {@code DoctorController} + {@code MsDoctorDAO}
 * pada tabel ms_doctor, ms_staff, dan ms_staff_in_unit.
 */
@Service
public class DoctorService {

    private static final int GRUP_DOKTER = 4;

    private final JdbcTemplate jdbcTemplate;

    public DoctorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar dokter. Mengikuti {@code MsDoctorDAO.searchDocttor} yang
     * menggabungkan ms_doctor dengan ms_staff, memfilter grup dokter dan
     * staff yang masih aktif (d_staff_fired_date is null). Kolom unitIds
     * berisi daftar id unit dari ms_staff_in_unit.
     */
    public List<DoctorRowResponse> getDoctors(String code, String name) {
        String likeCode = "%" + normalizeLike(code) + "%";
        String likeName = "%" + normalizeLike(name) + "%";

        String sql = "select dr.n_doctor_id, dr.n_staff_id, "
                + "staff.v_staff_code, staff.v_staff_name, staff.v_staff_addr, "
                + "staff.v_staff_ph_no, staff.n_coa, coa.v_acct_no, "
                + "staff.d_staff_hired_date, staff.d_staff_fired_date, "
                + "dr.n_msgroup_id, dr.v_doc_lvl_of_expertise, "
                + "dr.n_out_patient_earnings, dr.v_doc_bank_acc_no, "
                + "dr.n_assisten_of, dr.n_percentage_in_patient_wage, "
                + "dr.v_doc_status, dr.n_doc_type, dr.flag_antrian "
                + "from ms_doctor dr "
                + "join ms_staff staff on staff.n_staff_id = dr.n_staff_id "
                + "left join ms_coa coa on coa.n_coa_id = staff.n_coa "
                + "where dr.n_msgroup_id = ? "
                + "and staff.d_staff_fired_date is null "
                + "and upper(staff.v_staff_code) like ? "
                + "and upper(staff.v_staff_name) like ? "
                + "order by staff.v_staff_code";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            Integer staffId = resultSet.getInt("n_staff_id");
            Integer assistenOf = toInteger(resultSet.getObject("n_assisten_of"));
            return new DoctorRowResponse(
                    resultSet.getInt("n_doctor_id"),
                    staffId,
                    resultSet.getString("v_staff_code"),
                    resultSet.getString("v_staff_name"),
                    resultSet.getString("v_staff_addr"),
                    resultSet.getString("v_staff_ph_no"),
                    toInteger(resultSet.getObject("n_coa")),
                    resultSet.getString("v_acct_no"),
                    toInteger(resultSet.getObject("n_msgroup_id")),
                    getStaffGroupName(toInteger(resultSet.getObject("n_msgroup_id"))),
                    resultSet.getString("v_doc_lvl_of_expertise"),
                    resultSet.getString("v_doc_status"),
                    toInteger(resultSet.getObject("n_out_patient_earnings")),
                    resultSet.getString("v_doc_bank_acc_no"),
                    assistenOf,
                    getAssistenOfName(assistenOf),
                    toInteger(resultSet.getObject("n_percentage_in_patient_wage")),
                    toInteger(resultSet.getObject("n_doc_type")),
                    toInteger(resultSet.getObject("flag_antrian")),
                    toDateString(resultSet.getObject("d_staff_hired_date")),
                    toDateString(resultSet.getObject("d_staff_fired_date")),
                    getUnitIds(staffId));
        }, GRUP_DOKTER, likeCode, likeName);
    }

    /**
     * Nama staff (dokter) yang menjadi asisten dari, dicari dari ms_staff
     * berdasarkan n_staff_id yang tersimpan di kolom n_assisten_of.
     */
    private String getAssistenOfName(Integer assistenOf) {
        if (assistenOf == null) {
            return null;
        }
        List<String> names = jdbcTemplate.query(
                "select v_staff_name from ms_staff where n_staff_id = ?",
                (resultSet, rowNum) -> resultSet.getString("v_staff_name"),
                assistenOf);
        return names.isEmpty() ? null : names.get(0);
    }

    /**
     * Daftar id unit untuk seorang staff dari ms_staff_in_unit.
     */
    private List<Integer> getUnitIds(Integer staffId) {
        String sql = "select n_unit_id from ms_staff_in_unit "
                + "where n_staff_id = ? order by n_unit_id";
        return jdbcTemplate.query(sql,
                (resultSet, rowNum) -> resultSet.getInt("n_unit_id"), staffId);
    }

    /**
     * Nama group staff medis dari id. Mengikuti pilihan statis pada legacy
     * {@code msDokter.zul} (medicStaffGroupList).
     */
    private String getStaffGroupName(Integer groupId) {
        if (groupId == null) {
            return null;
        }
        switch (groupId) {
            case 4:
                return "DOKTER";
            case 5:
                return "ANASTESI";
            case 10:
                return "RADIOGRAFER";
            default:
                return null;
        }
    }

    /**
     * Data master untuk form dokter: opsi unit, opsi COA, opsi group staff
     * medis, opsi tingkat keahlian, dan opsi status.
     */
    public DoctorMastersResponse getMasters() {
        return new DoctorMastersResponse(
                getUnitOptions(),
                getCoaOptions(),
                getMedicStaffGroupOptions(),
                getLevelOfExpertiseOptions(),
                getStatusOptions());
    }

    /**
     * Opsi dropdown unit. Mengikuti {@code MsDoctorDAO.getAllUnit()}.
     */
    public List<UnitOptionResponse> getUnitOptions() {
        String sql = "select n_unit_id, v_unit_code, v_unit_name "
                + "from ms_unit order by v_unit_name";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new UnitOptionResponse(
                resultSet.getInt("n_unit_id"),
                resultSet.getString("v_unit_code"),
                resultSet.getString("v_unit_name")));
    }

    /**
     * Opsi dropdown COA. Mengikuti {@code MsDoctorDAO} yang memakai COA dari
     * ms_coa untuk bandbox NO. COA.
     */
    public List<CoaOptionResponse> getCoaOptions() {
        String sql = "select n_coa_id, v_acct_no, v_acct_name from ms_coa "
                + "order by v_acct_no limit 100";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new CoaOptionResponse(
                resultSet.getInt("n_coa_id"),
                resultSet.getString("v_acct_no"),
                resultSet.getString("v_acct_name")));
    }

    /**
     * Pencarian COA untuk bandbox NO. COA. Mencari berdasarkan account code
     * (v_acct_no) ATAU account name (v_acct_name), keduanya memakai LIKE.
     */
    public List<CoaOptionResponse> searchCoa(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
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
     * Opsi dropdown group staff medis. Mengikuti pilihan statis pada legacy
     * {@code msDokter.zul} (medicStaffGroupList): 4 = DOKTER, 5 = ANASTESI,
     * 10 = RADIOGRAFER.
     */
    public List<MedicStaffGroupOptionResponse> getMedicStaffGroupOptions() {
        List<MedicStaffGroupOptionResponse> options = new ArrayList<>();
        options.add(new MedicStaffGroupOptionResponse(4, "DOKTER"));
        options.add(new MedicStaffGroupOptionResponse(5, "ANASTESI"));
        options.add(new MedicStaffGroupOptionResponse(10, "RADIOGRAFER"));
        return options;
    }

    /**
     * Opsi dropdown tingkat keahlian. Mengikuti pilihan statis pada legacy
     * {@code msDokter.zul} (tingkatKeahlian): value "Dokter Umum" dan
     * "Dokter Spesialis". Nilai inilah yang tersimpan di kolom
     * v_doc_lvl_of_expertise, sehingga harus sama persis agar dropdown
     * terpilih dengan benar saat data dokter diedit.
     */
    public List<String> getLevelOfExpertiseOptions() {
        return Arrays.asList("Dokter Umum", "Dokter Spesialis");
    }

    /**
     * Opsi dropdown status dokter. Mengikuti pilihan statis pada legacy
     * {@code msDokter.zul} (statusList): DOKTER TETAP dan DOKTER TAMU.
     */
    public List<String> getStatusOptions() {
        return Arrays.asList("DOKTER TETAP", "DOKTER TAMU");
    }

    /**
     * Cek apakah kode staff sudah dipakai. Mengikuti kolom unik v_staff_code.
     */
    public boolean isCodeExists(String code) {
        if (code == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ms_staff where v_staff_code = ?",
                Integer.class,
                code.trim());
        return count != null && count > 0;
    }

    /**
     * Simpan / update dokter. Mengikuti {@code MsDoctorDAO.saveDoctor} yang
     * menyimpan ms_staff, ms_doctor, dan relasi ms_staff_in_unit.
     */
    @Transactional
    public void save(DoctorSaveRequest request, String username) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Kode staff harus diisi.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nama staff harus diisi.");
        }
        if (request.getUnitId() == null) {
            throw new IllegalArgumentException("Unit harus dipilih.");
        }

        Integer staffId = request.getStaffId();
        Integer doctorId = request.getId();
        String actor = normalizeActor(username);

        if (staffId == null) {
            // Cek duplikasi kode (kolom unik v_staff_code)
            if (isCodeExists(code)) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            staffId = nextStaffId();
            jdbcTemplate.update(
                    "insert into ms_staff (n_staff_id, v_staff_code, v_staff_name, "
                            + "v_staff_addr, v_staff_ph_no, n_staff_salary, "
                            + "d_staff_hired_date, d_staff_fired_date, n_coa, "
                            + "n_staff_role, v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                    staffId,
                    code,
                    name,
                    request.getAddress(),
                    request.getPhone(),
                    request.getSalary(),
                    parseDate(request.getHiredDate()),
                    parseDate(request.getFiredDate()),
                    request.getCoaId(),
                    GRUP_DOKTER,
                    actor);

            doctorId = nextDoctorId();
            jdbcTemplate.update(
                    "insert into ms_doctor (n_doctor_id, n_staff_id, n_msgroup_id, "
                            + "v_doc_lvl_of_expertise, n_out_patient_earnings, "
                            + "n_percentage_in_patient_wage, v_doc_bank_acc_no, "
                            + "n_assisten_of, v_doc_status, n_doc_type, flag_antrian, "
                            + "v_who_create, d_whn_create) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                    doctorId,
                    staffId,
                    request.getStaffGroup() == null ? GRUP_DOKTER : request.getStaffGroup(),
                    request.getLevelOfExpertise(),
                    request.getOutPatientEarnings(),
                    request.getPercentageInPatientWage(),
                    request.getBankAccNo(),
                    request.getAssistenOf(),
                    request.getStatus(),
                    request.getDocType(),
                    request.getFlagAntrian(),
                    actor);

            jdbcTemplate.update(
                    "insert into ms_staff_in_unit (n_staff_id, n_unit_id, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, now())",
                    staffId,
                    request.getUnitId(),
                    actor);
        } else {
            // Cek duplikasi kode selain staff ini
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from ms_staff where v_staff_code = ? and n_staff_id <> ?",
                    Integer.class,
                    code,
                    staffId);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Kode sudah terdaftar.");
            }
            jdbcTemplate.update(
                    "update ms_staff set v_staff_code = ?, v_staff_name = ?, "
                            + "v_staff_addr = ?, v_staff_ph_no = ?, n_staff_salary = ?, "
                            + "d_staff_hired_date = ?, d_staff_fired_date = ?, n_coa = ?, "
                            + "v_who_change = ?, d_whn_change = now() where n_staff_id = ?",
                    code,
                    name,
                    request.getAddress(),
                    request.getPhone(),
                    request.getSalary(),
                    parseDate(request.getHiredDate()),
                    parseDate(request.getFiredDate()),
                    request.getCoaId(),
                    actor,
                    staffId);

            if (doctorId != null) {
                jdbcTemplate.update(
                        "update ms_doctor set n_msgroup_id = ?, "
                                + "v_doc_lvl_of_expertise = ?, n_out_patient_earnings = ?, "
                                + "n_percentage_in_patient_wage = ?, v_doc_bank_acc_no = ?, "
                                + "n_assisten_of = ?, v_doc_status = ?, n_doc_type = ?, "
                                + "flag_antrian = ?, v_who_change = ?, d_whn_change = now() "
                                + "where n_doctor_id = ?",
                        request.getStaffGroup() == null ? GRUP_DOKTER : request.getStaffGroup(),
                        request.getLevelOfExpertise(),
                        request.getOutPatientEarnings(),
                        request.getPercentageInPatientWage(),
                        request.getBankAccNo(),
                        request.getAssistenOf(),
                        request.getStatus(),
                        request.getDocType(),
                        request.getFlagAntrian(),
                        actor,
                        doctorId);
            }

            // Update relasi unit staff (hapus lama, insert baru)
            jdbcTemplate.update("delete from ms_staff_in_unit where n_staff_id = ?", staffId);
            jdbcTemplate.update(
                    "insert into ms_staff_in_unit (n_staff_id, n_unit_id, "
                            + "v_who_create, d_whn_create) values (?, ?, ?, now())",
                    staffId,
                    request.getUnitId(),
                    actor);
        }
    }

    /**
     * Hapus dokter. Mengikuti {@code MsDoctorDAO.deleteById} yang menghapus
     * dari ms_staff (relasi ms_doctor dan ms_staff_in_unit ikut terhapus
     * melalui foreign key).
     */
    @Transactional
    public boolean delete(Integer staffId) {
        int affected = jdbcTemplate.update("delete from ms_staff where n_staff_id = ?", staffId);
        return affected > 0;
    }

    private Integer nextStaffId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_staff_n_staff_id_seq')",
                Integer.class);
    }

    private Integer nextDoctorId() {
        return jdbcTemplate.queryForObject(
                "select nextval('ms_doctor_n_doctor_id_seq')",
                Integer.class);
    }

    private java.sql.Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    private String toDateString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toString();
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toString();
        }
        return value.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLike(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
