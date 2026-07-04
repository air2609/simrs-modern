package com.vone.simrs.admission;

import com.vone.simrs.auth.AuthenticationRequiredException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionRegistrationService {

    private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int REG_ACTIVE = 1;
    private static final int NOTE_VALIDATED = 2;
    private static final short PAYMENT_UNPAID = 0;
    private static final short DOCTOR_GROUP = 4;

    private final JdbcTemplate jdbcTemplate;
    private final int cardFee;
    private final String arCoaKey;
    private final String registrationCoaKey;
    private final String registrationCoaFallbackKey;

    public AdmissionRegistrationService(
        JdbcTemplate jdbcTemplate,
        @Value("${app.admission.card-fee:0}") int cardFee,
        @Value("${app.admission.coa-ar-key:COA_INPATIENT_AR}") String arCoaKey,
        @Value("${app.admission.registration-coa-key:COA_ADM}") String registrationCoaKey,
        @Value("${app.admission.registration-coa-fallback-key:COA_MISC_TRX}") String registrationCoaFallbackKey
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.cardFee = cardFee;
        this.arCoaKey = arCoaKey;
        this.registrationCoaKey = registrationCoaKey;
        this.registrationCoaFallbackKey = registrationCoaFallbackKey;
    }

    public String requireUsername(HttpSession session) {
        if (session == null) {
            throw new AuthenticationRequiredException("Your session has been expired. You need to login again.");
        }

        Object username = session.getAttribute("USER_INFO");
        if (!(username instanceof String)) {
            throw new AuthenticationRequiredException("Your session has been expired. You need to login again.");
        }

        return (String) username;
    }

    public AdmissionRegistrationMastersResponse getMasters() {
        return new AdmissionRegistrationMastersResponse(getUnits(), getPatientTypes(), getProvinces());
    }

    public List<UnitOptionResponse> getUnits() {
        return jdbcTemplate.query(
            "select unit.n_unit_id, unit.n_division_id, unit.v_unit_code, unit.v_unit_name, "
                + "div.v_division_name, coalesce(div.n_registration_charge, 0) as n_registration_charge "
                + "from ms_unit unit "
                + "join ms_division div on div.n_division_id = unit.n_division_id "
                + "where div.v_registration_unit = 'YES' "
                + "order by unit.v_unit_name",
            (resultSet, rowNum) -> new UnitOptionResponse(
                resultSet.getInt("n_unit_id"),
                resultSet.getInt("n_division_id"),
                resultSet.getString("v_unit_code"),
                resultSet.getString("v_unit_name"),
                resultSet.getString("v_division_name"),
                resultSet.getInt("n_registration_charge")
            )
        );
    }

    public List<PatientTypeOptionResponse> getPatientTypes() {
        return jdbcTemplate.query(
            "select n_patient_type_id, v_tpatient, v_tpatient_desc from ms_patient_type order by v_tpatient",
            (resultSet, rowNum) -> new PatientTypeOptionResponse(
                resultSet.getInt("n_patient_type_id"),
                resultSet.getString("v_tpatient"),
                resultSet.getString("v_tpatient_desc")
            )
        );
    }

    public List<OptionResponse> getProvinces() {
        return jdbcTemplate.query(
            "select v_province_id, v_province_name from ms_province order by v_province_name",
            (resultSet, rowNum) -> new OptionResponse(
                resultSet.getString("v_province_id"),
                resultSet.getString("v_province_name")
            )
        );
    }

    public List<OptionResponse> getRegencies(String provinceCode) {
        return jdbcTemplate.query(
            "select reg.v_regency_id, reg.v_regency_name "
                + "from ms_regency reg "
                + "join ms_province prov on prov.n_province_id = reg.n_province_id "
                + "where prov.v_province_id = ? "
                + "order by reg.v_regency_name",
            (resultSet, rowNum) -> new OptionResponse(
                resultSet.getString("v_regency_id"),
                resultSet.getString("v_regency_name")
            ),
            provinceCode
        );
    }

    public List<OptionResponse> getDistricts(String regencyCode) {
        return jdbcTemplate.query(
            "select dist.v_sub_district_id, dist.v_sub_district_name "
                + "from ms_sub_district dist "
                + "join ms_regency reg on reg.n_regency_id = dist.n_regency_id "
                + "where reg.v_regency_id = ? "
                + "order by dist.v_sub_district_name",
            (resultSet, rowNum) -> new OptionResponse(
                resultSet.getString("v_sub_district_id"),
                resultSet.getString("v_sub_district_name")
            ),
            regencyCode
        );
    }

    public List<OptionResponse> getVillages(String districtCode) {
        return jdbcTemplate.query(
            "select vil.v_village_code, vil.v_village_name "
                + "from ms_village vil "
                + "join ms_sub_district dist on dist.n_subdistrict_id = vil.n_subdistrict_id "
                + "where dist.v_sub_district_id = ? "
                + "order by vil.v_village_name",
            (resultSet, rowNum) -> new OptionResponse(
                resultSet.getString("v_village_code"),
                resultSet.getString("v_village_name")
            ),
            districtCode
        );
    }

    public List<DoctorOptionResponse> getDoctors(Integer unitId) {
        return jdbcTemplate.query(
            "select distinct staff.n_staff_id, staff.v_staff_code, staff.v_staff_name "
                + "from ms_doctor dr "
                + "join ms_staff staff on staff.n_staff_id = dr.n_staff_id "
                + "join ms_staff_in_unit siu on siu.n_staff_id = staff.n_staff_id "
                + "where siu.n_unit_id = ? "
                + "and dr.n_msgroup_id = ? "
                + "and staff.d_staff_fired_date is null "
                + "order by staff.v_staff_name",
            (resultSet, rowNum) -> new DoctorOptionResponse(
                resultSet.getInt("n_staff_id"),
                resultSet.getString("v_staff_code"),
                resultSet.getString("v_staff_name")
            ),
            unitId,
            DOCTOR_GROUP
        );
    }

    public List<AdmissionPatientSearchResponse> searchPatients(
        String mrCode,
        String patientName,
        String nik,
        String birthDate,
        String address
    ) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<Object>();

        sql.append("select mr.n_mr_id, mr.v_mr_code, patient.n_patient_id, patient.v_patient_name, patient.nik, ")
            .append("patient.d_patient_dob, patient.v_patient_main_addr ")
            .append("from tb_medical_record mr ")
            .append("join ms_patient patient on patient.n_patient_id = mr.n_patient_id ")
            .append("where 1=1 ");

        if (hasText(mrCode)) {
            sql.append("and mr.v_mr_code like ? ");
            args.add(buildLike(normalizeUpper(mrCode)));
        }
        if (hasText(patientName)) {
            sql.append("and upper(patient.v_patient_name) like ? ");
            args.add(buildLike(normalizeUpper(patientName)));
        }
        if (hasText(nik)) {
            sql.append("and patient.nik like ? ");
            args.add(buildLike(nik.trim()));
        }
        if (hasText(address)) {
            sql.append("and upper(patient.v_patient_main_addr) like ? ");
            args.add(buildLike(normalizeUpper(address)));
        }
        if (hasText(birthDate)) {
            sql.append("and patient.d_patient_dob = ? ");
            args.add(Date.valueOf(LocalDate.parse(birthDate, BIRTH_DATE_FORMATTER)));
        }

        sql.append("order by patient.v_patient_name limit 100");

        return jdbcTemplate.query(
            sql.toString(),
            args.toArray(),
            (resultSet, rowNum) -> new AdmissionPatientSearchResponse(
                resultSet.getInt("n_patient_id"),
                resultSet.getInt("n_mr_id"),
                resultSet.getString("v_mr_code"),
                resultSet.getString("v_patient_name"),
                resultSet.getString("nik"),
                toIsoDate(resultSet.getDate("d_patient_dob")),
                resultSet.getString("v_patient_main_addr")
            )
        );
    }

    public AdmissionPatientDetailResponse getPatientDetail(String mrCode) {
        List<AdmissionPatientDetailResponse> results = jdbcTemplate.query(
            "select patient.n_patient_id, mr.n_mr_id, mr.v_mr_code, mr.ihs_number, patient.n_patient_type_id, "
                + "patient.v_patient_name, patient.v_patient_gender, patient.d_patient_dob, patient.nik, "
                + "patient.v_patient_main_addr, patient.v_patient_main_ph_no, patient.v_patient_main_rt_rw, "
                + "patient.v_patient_alt_addr, patient.v_patient_alt_ph_no, patient.v_patient_alt_rt_rw, "
                + "patient.v_patient_marital_status, patient.v_patient_nationality, patient.v_patient_religion, "
                + "patient.v_patient_edu, patient.v_patient_job_type, patient.v_patient_priority, "
                + "patient.v_patient_etnis, patient.v_patient_language, patient.province_code, patient.city_code, "
                + "patient.district_code, patient.subdistrict_code "
                + "from tb_medical_record mr "
                + "join ms_patient patient on patient.n_patient_id = mr.n_patient_id "
                + "where mr.v_mr_code = ?",
            (resultSet, rowNum) -> {
                String[] mainRtRw = splitRtRw(resultSet.getString("v_patient_main_rt_rw"));
                String[] altRtRw = splitRtRw(resultSet.getString("v_patient_alt_rt_rw"));

                return new AdmissionPatientDetailResponse(
                    resultSet.getInt("n_patient_id"),
                    resultSet.getInt("n_mr_id"),
                    resultSet.getString("v_mr_code"),
                    getActiveRegistrationCode(resultSet.getInt("n_mr_id")),
                    resultSet.getString("ihs_number"),
                    getNullableInteger(resultSet, "n_patient_type_id"),
                    resultSet.getString("v_patient_name"),
                    resultSet.getString("v_patient_gender"),
                    toIsoDate(resultSet.getDate("d_patient_dob")),
                    resultSet.getString("nik"),
                    resultSet.getString("v_patient_main_addr"),
                    resultSet.getString("v_patient_main_ph_no"),
                    mainRtRw[0],
                    mainRtRw[1],
                    resultSet.getString("v_patient_alt_addr"),
                    resultSet.getString("v_patient_alt_ph_no"),
                    altRtRw[0],
                    altRtRw[1],
                    resultSet.getString("v_patient_marital_status"),
                    resultSet.getString("v_patient_nationality"),
                    resultSet.getString("v_patient_religion"),
                    resultSet.getString("v_patient_edu"),
                    resultSet.getString("v_patient_job_type"),
                    resultSet.getString("v_patient_priority"),
                    resultSet.getString("v_patient_etnis"),
                    resultSet.getString("v_patient_language"),
                    resultSet.getString("province_code"),
                    resultSet.getString("city_code"),
                    resultSet.getString("district_code"),
                    resultSet.getString("subdistrict_code")
                );
            },
            normalizeUpper(mrCode)
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException("No.MR tidak ditemukan, Mohon Diganti Dengan yang Lain..!");
        }

        return results.get(0);
    }

    @Transactional
    public AdmissionRegistrationResultResponse saveRegistration(AdmissionRegistrationSaveRequest request, String username) {
        UnitRow unit = findUnitRow(request.getUnitId());
        if (unit == null) {
            throw new IllegalArgumentException("Unit pendaftaran tidak ditemukan.");
        }

        if (!doctorExistsInUnit(request.getDoctorStaffId(), request.getUnitId())) {
            throw new IllegalArgumentException("Dokter pemeriksa tidak ditemukan pada unit yang dipilih.");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        LocalDate birthDate = LocalDate.parse(request.getBirthDate(), BIRTH_DATE_FORMATTER);
        String normalizedUsername = normalizeUpper(username);
        boolean existingPatient = hasText(request.getExistingMrCode());

        Integer patientId;
        Integer medicalRecordId;
        String mrCode;
        String ihsNumber = null;

        if (existingPatient) {
            PatientRecord record = findPatientRecordByMrCode(normalizeUpper(request.getExistingMrCode()));
            if (record == null) {
                throw new IllegalArgumentException("No.MR tidak ditemukan, Mohon Diganti Dengan yang Lain..!");
            }
            if (hasText(getActiveRegistrationCode(record.medicalRecordId))) {
                throw new IllegalArgumentException("Pasien Masih Terdaftar..! Registrasi Tidak Bisa Dilakukan..!");
            }

            patientId = record.patientId;
            medicalRecordId = record.medicalRecordId;
            mrCode = record.mrCode;
            ihsNumber = record.ihsNumber;

            updatePatient(request, patientId, normalizedUsername, now, birthDate);
            jdbcTemplate.update(
                "update tb_medical_record set ihs_number = coalesce(ihs_number, ?) where n_mr_id = ?",
                ihsNumber,
                medicalRecordId
            );
        } else {
            patientId = nextSequenceValue("ms_patient_n_patient_id_seq");
            medicalRecordId = nextSequenceValue("tb_medical_record_n_mr_id_seq");
            mrCode = toMrCode(medicalRecordId);

            insertPatient(request, patientId, normalizedUsername, now, birthDate);
            jdbcTemplate.update(
                "insert into tb_medical_record (n_mr_id, n_patient_id, v_mr_code, ihs_number, v_who_create, d_whn_create) "
                    + "values (?, ?, ?, ?, ?, ?)",
                medicalRecordId,
                patientId,
                mrCode,
                ihsNumber,
                normalizedUsername,
                now
            );
        }

        Integer registrationId = nextSequenceValue("tb_registration_n_reg_id_seq");
        Integer registrationNumber = nextSequenceValue("registration_number");
        String registrationCode = toRegistrationCode(registrationNumber, now, unit.unitCode);

        jdbcTemplate.update(
            "insert into tb_registration (n_reg_id, n_staff_id, n_division_id, n_mr_id, n_unit_id, d_registration_date, "
                + "v_reg_secondary_id, v_who_create, d_whn_create, reg_status) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            registrationId,
            request.getDoctorStaffId(),
            unit.divisionId,
            medicalRecordId,
            unit.unitId,
            now,
            registrationCode,
            normalizedUsername,
            now,
            REG_ACTIVE
        );

        double registrationFee = unit.registrationCharge == null ? 0 : unit.registrationCharge.doubleValue();
        double cardFeeAmount = existingPatient ? 0 : cardFee;
        double total = registrationFee + cardFeeAmount;
        Integer noteSequence = nextSequenceValue("nota_rajal_seq");
        String noteNumber = toNoteNumber(noteSequence, now, "ADM");

        Integer examinationId = nextSequenceValue("tb_examination_n_exam_id_seq");
        jdbcTemplate.update(
            "insert into tb_examination (n_exam_id, n_reg_id, n_patient_id, v_note_no, n_total_amount, n_payment_status, "
                + "n_exam_status, v_who_create, d_whn_create, n_unit_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            examinationId,
            registrationId,
            patientId,
            noteNumber,
            total,
            PAYMENT_UNPAID,
            NOTE_VALIDATED,
            normalizedUsername,
            now,
            unit.unitId
        );

        insertMisc(examinationId, "BIAYA PENDAFTARAN", registrationFee, normalizedUsername, now);
        if (!existingPatient) {
            insertMisc(examinationId, "BIAYA PEMBUATAN KARTU", cardFeeAmount, normalizedUsername, now);
        }

        insertRegistrationJournals(noteNumber, registrationFee, cardFeeAmount, normalizedUsername, now, !existingPatient);

        return new AdmissionRegistrationResultResponse(existingPatient, mrCode, registrationCode, noteNumber, normalizeUpper(request.getPatientName()));
    }

    private void insertPatient(
        AdmissionRegistrationSaveRequest request,
        Integer patientId,
        String username,
        Timestamp now,
        LocalDate birthDate
    ) {
        jdbcTemplate.update(
            "insert into ms_patient (n_patient_id, n_patient_type_id, v_patient_name, v_patient_marital_status, v_patient_gender, "
                + "d_patient_dob, v_patient_religion, v_patient_edu, v_patient_job_type, v_patient_main_addr, v_patient_main_rt_rw, "
                + "v_patient_main_ph_no, v_patient_alt_addr, v_patient_alt_rt_rw, v_patient_alt_ph_no, v_patient_nationality, "
                + "v_patient_priority, v_patient_etnis, v_patient_language, nik, province_code, city_code, district_code, "
                + "subdistrict_code, v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            patientId,
            request.getPatientTypeId(),
            normalizeUpper(request.getPatientName()),
            normalizeOptional(request.getMaritalStatus()),
            normalizeGender(request.getGender()),
            Date.valueOf(birthDate),
            normalizeOptional(request.getReligion()),
            normalizeOptional(request.getEducation()),
            normalizeOptional(request.getJobType()),
            normalizeUpper(request.getMainAddress()),
            mergeRtRw(request.getMainRt(), request.getMainRw()),
            normalizeOptional(request.getMainPhone()),
            normalizeOptionalUpper(request.getAltAddress()),
            mergeRtRw(request.getAltRt(), request.getAltRw()),
            normalizeOptional(request.getAltPhone()),
            normalizeOptional(request.getNationality()),
            normalizeOptional(request.getPriority()),
            normalizeOptionalUpper(request.getEtnis()),
            normalizeOptionalUpper(request.getLanguage()),
            normalizeOptional(request.getNik()),
            normalizeOptional(request.getProvinceCode()),
            normalizeOptional(request.getCityCode()),
            normalizeOptional(request.getDistrictCode()),
            normalizeOptional(request.getSubdistrictCode()),
            username,
            now
        );
    }

    private void updatePatient(
        AdmissionRegistrationSaveRequest request,
        Integer patientId,
        String username,
        Timestamp now,
        LocalDate birthDate
    ) {
        jdbcTemplate.update(
            "update ms_patient set n_patient_type_id = ?, v_patient_name = ?, v_patient_marital_status = ?, v_patient_gender = ?, "
                + "d_patient_dob = ?, v_patient_religion = ?, v_patient_edu = ?, v_patient_job_type = ?, v_patient_main_addr = ?, "
                + "v_patient_main_rt_rw = ?, v_patient_main_ph_no = ?, v_patient_alt_addr = ?, v_patient_alt_rt_rw = ?, v_patient_alt_ph_no = ?, "
                + "v_patient_nationality = ?, v_patient_priority = ?, v_patient_etnis = ?, v_patient_language = ?, nik = ?, province_code = ?, "
                + "city_code = ?, district_code = ?, subdistrict_code = ?, v_who_change = ?, d_whn_change = ? where n_patient_id = ?",
            request.getPatientTypeId(),
            normalizeUpper(request.getPatientName()),
            normalizeOptional(request.getMaritalStatus()),
            normalizeGender(request.getGender()),
            Date.valueOf(birthDate),
            normalizeOptional(request.getReligion()),
            normalizeOptional(request.getEducation()),
            normalizeOptional(request.getJobType()),
            normalizeUpper(request.getMainAddress()),
            mergeRtRw(request.getMainRt(), request.getMainRw()),
            normalizeOptional(request.getMainPhone()),
            normalizeOptionalUpper(request.getAltAddress()),
            mergeRtRw(request.getAltRt(), request.getAltRw()),
            normalizeOptional(request.getAltPhone()),
            normalizeOptional(request.getNationality()),
            normalizeOptional(request.getPriority()),
            normalizeOptionalUpper(request.getEtnis()),
            normalizeOptionalUpper(request.getLanguage()),
            normalizeOptional(request.getNik()),
            normalizeOptional(request.getProvinceCode()),
            normalizeOptional(request.getCityCode()),
            normalizeOptional(request.getDistrictCode()),
            normalizeOptional(request.getSubdistrictCode()),
            username,
            now,
            patientId
        );
    }

    private void insertMisc(Integer examinationId, String miscName, double amount, String username, Timestamp now) {
        Integer miscId = nextSequenceValue("tb_misc_trx_n_misc_trx_id_seq");
        jdbcTemplate.update(
            "insert into tb_misc_trx (n_misc_trx_id, n_note_id, n_amount_trx, n_qty, v_who_create, d_whn_create, "
                + "n_amount_after_disc, n_item_price, v_misc_name) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            miscId,
            examinationId,
            amount,
            1,
            username,
            now,
            amount,
            amount,
            miscName
        );
    }

    private void insertRegistrationJournals(
        String noteNumber,
        double registrationFee,
        double cardFeeAmount,
        String username,
        Timestamp now,
        boolean includeCardFee
    ) {
        double total = registrationFee + cardFeeAmount;
        if (total == 0) {
            return;
        }

        Integer coaArId = findCoaIdByGimKey(arCoaKey);
        Integer coaMiscId = findCoaIdByGimKey(registrationCoaKey);
        if (coaMiscId == null) {
            coaMiscId = findCoaIdByGimKey(registrationCoaFallbackKey);
        }
        if (coaArId == null || coaMiscId == null) {
            throw new IllegalStateException("COA pendaftaran belum terkonfigurasi di database existing.");
        }

        String batchId = buildJournalBatchId();
        insertJournal(batchId, noteNumber, "MISC:BIAYA PENDAFTARAN;QTY:1", total, 0, now, username, coaArId);
        insertJournal(batchId, noteNumber, "MISC:BIAYA PENDAFTARAN;QTY:1", 0, registrationFee, now, username, coaMiscId);
        if (includeCardFee && cardFeeAmount > 0) {
            insertJournal(batchId, noteNumber, "MISC:BIAYA PEMBUATAN KARTU;QTY:1", 0, cardFeeAmount, now, username, coaMiscId);
        }
    }

    private void insertJournal(
        String batchId,
        String voucherNo,
        String description,
        double debit,
        double credit,
        Timestamp now,
        String username,
        Integer coaId
    ) {
        Integer journalId = nextSequenceValue("tb_journal_trx_n_journal_id_seq");
        jdbcTemplate.update(
            "insert into tb_journal_trx (n_journal_id, v_journal_batch_id, v_voucher_no, v_desc, n_debit, n_credit, "
                + "d_whn_create, v_who_create, d_apl_date, n_coa_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            journalId,
            batchId,
            voucherNo,
            description,
            debit,
            credit,
            now,
            username,
            now,
            coaId
        );
    }

    private String buildJournalBatchId() {
        Integer sequence = nextSequenceValue("sq_journal_trx");
        return "AR" + leftPad(sequence.toString(), 15, '0');
    }

    private String getActiveRegistrationCode(Integer medicalRecordId) {
        try {
            return jdbcTemplate.queryForObject(
                "select v_reg_secondary_id from tb_registration where n_mr_id = ? and reg_status = 1 "
                    + "order by d_registration_date desc limit 1",
                String.class,
                medicalRecordId
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private PatientRecord findPatientRecordByMrCode(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                "select mr.n_mr_id, mr.v_mr_code, mr.ihs_number, patient.n_patient_id "
                    + "from tb_medical_record mr "
                    + "join ms_patient patient on patient.n_patient_id = mr.n_patient_id "
                    + "where mr.v_mr_code = ?",
                (resultSet, rowNum) -> new PatientRecord(
                    resultSet.getInt("n_patient_id"),
                    resultSet.getInt("n_mr_id"),
                    resultSet.getString("v_mr_code"),
                    resultSet.getString("ihs_number")
                ),
                mrCode
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private UnitRow findUnitRow(Integer unitId) {
        try {
            return jdbcTemplate.queryForObject(
                "select unit.n_unit_id, unit.n_division_id, unit.v_unit_code, coalesce(div.n_registration_charge, 0) as n_registration_charge "
                    + "from ms_unit unit "
                    + "join ms_division div on div.n_division_id = unit.n_division_id "
                    + "where unit.n_unit_id = ?",
                (resultSet, rowNum) -> new UnitRow(
                    resultSet.getInt("n_unit_id"),
                    resultSet.getInt("n_division_id"),
                    resultSet.getString("v_unit_code"),
                    resultSet.getInt("n_registration_charge")
                ),
                unitId
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private boolean doctorExistsInUnit(Integer doctorStaffId, Integer unitId) {
        Integer total = jdbcTemplate.queryForObject(
            "select count(*) from ms_doctor dr "
                + "join ms_staff staff on staff.n_staff_id = dr.n_staff_id "
                + "join ms_staff_in_unit siu on siu.n_staff_id = staff.n_staff_id "
                + "where dr.n_staff_id = ? and siu.n_unit_id = ? and dr.n_msgroup_id = ? and staff.d_staff_fired_date is null",
            Integer.class,
            doctorStaffId,
            unitId,
            DOCTOR_GROUP
        );
        return total != null && total.intValue() > 0;
    }

    private Integer nextSequenceValue(String sequenceName) {
        Number number = jdbcTemplate.queryForObject(
            "select nextval('" + sequenceName + "')",
            Number.class
        );
        return number == null ? null : number.intValue();
    }

    private Integer findCoaIdByGimKey(String gimKey) {
        try {
            return jdbcTemplate.queryForObject(
                "select coa.n_coa_id from ms_gim gim join ms_coa coa on coa.v_acct_no = gim.v_value where gim.v_key = ?",
                Integer.class,
                gimKey
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private String toMrCode(Integer id) {
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

    private String toRegistrationCode(Integer number, Timestamp date, String unitCode) {
        return "J-" + unitCode + "-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(date.toLocalDateTime()) + "-" + leftPad(number.toString(), 3, '0');
    }

    private String toNoteNumber(Integer number, Timestamp date, String unitCode) {
        return "J-" + unitCode + "-" + DateTimeFormatter.ofPattern("yyMM").format(date.toLocalDateTime()) + "-" + leftPad(number.toString(), 6, '0');
    }

    private String leftPad(String value, int length, char padChar) {
        if (value.length() >= length) {
            return value;
        }

        char[] buffer = new char[length - value.length()];
        Arrays.fill(buffer, padChar);
        return new String(buffer) + value;
    }

    private String[] splitRtRw(String combined) {
        String[] values = new String[] {"", ""};
        if (!hasText(combined)) {
            return values;
        }

        String[] parts = combined.split("/");
        values[0] = parts.length > 0 ? parts[0] : "";
        values[1] = parts.length > 1 ? parts[1] : "";
        return values;
    }

    private String mergeRtRw(String rt, String rw) {
        if (!hasText(rt) || !hasText(rw)) {
            return null;
        }
        return rt.trim() + "/" + rw.trim();
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName) throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? normalizeUpper(value) : null;
    }

    private String normalizeGender(String value) {
        return "F".equalsIgnoreCase(value) ? "F" : "M";
    }

    private String buildLike(String value) {
        return "%" + value + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String toIsoDate(Date date) {
        return date == null ? "" : date.toLocalDate().toString();
    }

    private static class PatientRecord {
        private final Integer patientId;
        private final Integer medicalRecordId;
        private final String mrCode;
        private final String ihsNumber;

        private PatientRecord(Integer patientId, Integer medicalRecordId, String mrCode, String ihsNumber) {
            this.patientId = patientId;
            this.medicalRecordId = medicalRecordId;
            this.mrCode = mrCode;
            this.ihsNumber = ihsNumber;
        }
    }

    private static class UnitRow {
        private final Integer unitId;
        private final Integer divisionId;
        private final String unitCode;
        private final Integer registrationCharge;

        private UnitRow(Integer unitId, Integer divisionId, String unitCode, Integer registrationCharge) {
            this.unitId = unitId;
            this.divisionId = divisionId;
            this.unitCode = unitCode;
            this.registrationCharge = registrationCharge;
        }
    }
}
