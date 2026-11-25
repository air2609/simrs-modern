package com.vone.simrs.satusehat;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sinkronisasi diagnosa (ICD-10 -> {@code Condition}) dan tindakan
 * (ICD-9-CM -> {@code Procedure}) ke SATUSEHAT.
 */
@Service
public class SatuSehatClinicalService {

    private static final String ICD10_SYSTEM = "http://hl7.org/fhir/sid/icd-10";
    private static final String ICD9CM_SYSTEM = "http://hl7.org/fhir/sid/icd-9-cm";
    private static final String CONDITION_CLINICAL_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/condition-clinical";
    private static final String CONDITION_VER_STATUS_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/condition-ver-status";
    private static final String CONDITION_CATEGORY_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/condition-category";
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter FHIR_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final SatuSehatClient satuSehatClient;

    public SatuSehatClinicalService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService,
            SatuSehatClient satuSehatClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.satuSehatClient = satuSehatClient;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Sinkronkan seluruh diagnosa ICD-10 registrasi menjadi Condition. */
    public SatuSehatClinicalSyncResponse syncConditions(String registrationCode) {
        ClinicalContext context = loadContext(registrationCode);
        List<DiagnosisRow> rows = loadDiagnoses(context.regId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada diagnosa (ICD-10) untuk registrasi ini.");
        }
        int created = 0;
        int skipped = 0;
        for (DiagnosisRow row : rows) {
            if (StringUtils.hasText(row.conditionId)) {
                skipped++;
                continue;
            }
            Condition condition = buildCondition(context, row);
            Condition saved = satuSehatClient.create(condition, Condition.class);
            String conditionId = saved.getIdElement() == null ? null : saved.getIdElement().getIdPart();
            if (!StringUtils.hasText(conditionId)) {
                throw new SatuSehatException("SATUSEHAT tidak mengembalikan Condition ID.");
            }
            jdbcTemplate.update(
                    "update tb_icd_diagnose set ss_condition_id = ? where n_icd_diagnose_id = ?",
                    conditionId, row.rowId);
            created++;
        }
        return new SatuSehatClinicalSyncResponse(registrationCode, context.encounterId, "Condition",
                created, skipped, "Sinkronisasi diagnosa selesai.");
    }

    /** Sinkronkan seluruh tindakan ICD-9-CM registrasi menjadi Procedure. */
    public SatuSehatClinicalSyncResponse syncProcedures(String registrationCode) {
        ClinicalContext context = loadContext(registrationCode);
        List<ProcedureRow> rows = loadProcedures(context.regId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada tindakan (ICD-9-CM) untuk registrasi ini.");
        }
        int created = 0;
        int skipped = 0;
        for (ProcedureRow row : rows) {
            if (StringUtils.hasText(row.procedureId)) {
                skipped++;
                continue;
            }
            Procedure procedure = buildProcedure(context, row);
            Procedure saved = satuSehatClient.create(procedure, Procedure.class);
            String procedureId = saved.getIdElement() == null ? null : saved.getIdElement().getIdPart();
            if (!StringUtils.hasText(procedureId)) {
                throw new SatuSehatException("SATUSEHAT tidak mengembalikan Procedure ID.");
            }
            jdbcTemplate.update(
                    "update tb_icd9_diagnose set ss_procedure_id = ? where n_icd9_diagnose_id = ?",
                    procedureId, row.rowId);
            created++;
        }
        return new SatuSehatClinicalSyncResponse(registrationCode, context.encounterId, "Procedure",
                created, skipped, "Sinkronisasi tindakan selesai.");
    }

    // ------------------------------------------------------------------
    // Pemetaan ke FHIR
    // ------------------------------------------------------------------

    private Condition buildCondition(ClinicalContext context, DiagnosisRow row) {
        Condition condition = new Condition();
        condition.setClinicalStatus(new CodeableConcept(
                new Coding(CONDITION_CLINICAL_SYSTEM, "active", "Active")));
        condition.setVerificationStatus(new CodeableConcept(
                new Coding(CONDITION_VER_STATUS_SYSTEM, "confirmed", "Confirmed")));
        condition.addCategory(new CodeableConcept(
                new Coding(CONDITION_CATEGORY_SYSTEM, "encounter-diagnosis", "Encounter Diagnosis")));
        condition.setCode(new CodeableConcept(
                new Coding(ICD10_SYSTEM, row.code, cleanDisplay(row.name))));
        condition.setSubject(new Reference("Patient/" + context.ihsNumber));
        condition.setEncounter(new Reference("Encounter/" + context.encounterId));
        return condition;
    }

    private Procedure buildProcedure(ClinicalContext context, ProcedureRow row) {
        Procedure procedure = new Procedure();
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setCategory(new CodeableConcept(
                new Coding(SNOMED_SYSTEM, "103693007", "Diagnostic procedure")));
        procedure.setCode(new CodeableConcept(
                new Coding(ICD9CM_SYSTEM, row.code, cleanDisplay(row.name))));
        procedure.setSubject(new Reference("Patient/" + context.ihsNumber));
        procedure.setEncounter(new Reference("Encounter/" + context.encounterId));
        String date = toFhirDate(context.registrationDate);
        procedure.setPerformed(new Period()
                .setStartElement(new DateTimeType(date))
                .setEndElement(new DateTimeType(date)));
        return procedure;
    }

    // ------------------------------------------------------------------
    // Data lokal
    // ------------------------------------------------------------------

    private ClinicalContext loadContext(String registrationCode) {
        if (!StringUtils.hasText(registrationCode)) {
            throw new IllegalArgumentException("KODE REGISTRASI HARUS DI ISI!");
        }
        List<ClinicalContext> rows = jdbcTemplate.query(
                "select r.n_reg_id, r.d_registration_date, r.ss_encounter_id, mr.ihs_number "
                        + "from tb_registration r "
                        + "join tb_medical_record mr on mr.n_mr_id = r.n_mr_id "
                        + "where r.v_reg_secondary_id = ? "
                        + "order by r.n_reg_id desc limit 1",
                (resultSet, rowNum) -> new ClinicalContext(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getTimestamp("d_registration_date"),
                        resultSet.getString("ss_encounter_id"),
                        resultSet.getString("ihs_number")),
                registrationCode.trim());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Registrasi tidak ditemukan: " + registrationCode);
        }
        ClinicalContext context = rows.get(0);
        if (!StringUtils.hasText(context.ihsNumber)) {
            throw new IllegalArgumentException("Pasien belum punya IHS number.");
        }
        if (!StringUtils.hasText(context.encounterId)) {
            throw new IllegalArgumentException(
                    "Encounter belum dibuat. Sinkronkan Encounter terlebih dahulu.");
        }
        return context;
    }

    private List<DiagnosisRow> loadDiagnoses(int registrationId) {
        return jdbcTemplate.query(
                "select i.n_icd_diagnose_id, m.v_icd_code, m.v_icd_name, i.ss_condition_id "
                        + "from tb_diagnose d "
                        + "join tb_icd_diagnose i on i.n_diagnose_id = d.n_diagnose_id "
                        + "join ms_icd m on m.n_icd_id = i.n_icd_id "
                        + "where d.n_reg_id = ? order by i.n_icd_diagnose_id",
                (resultSet, rowNum) -> new DiagnosisRow(
                        resultSet.getInt("n_icd_diagnose_id"),
                        resultSet.getString("v_icd_code"),
                        resultSet.getString("v_icd_name"),
                        resultSet.getString("ss_condition_id")),
                registrationId);
    }

    private List<ProcedureRow> loadProcedures(int registrationId) {
        return jdbcTemplate.query(
                "select i.n_icd9_diagnose_id, c.v_icd9cm_code, c.v_icd9cm_name, i.ss_procedure_id "
                        + "from tb_diagnose d "
                        + "join tb_icd9_diagnose i on i.n_diagnose_id = d.n_diagnose_id "
                        + "join ms_icd_9cm c on c.n_icd9cm_id = i.n_icd9cm_id "
                        + "where d.n_reg_id = ? order by i.n_icd9_diagnose_id",
                (resultSet, rowNum) -> new ProcedureRow(
                        resultSet.getInt("n_icd9_diagnose_id"),
                        resultSet.getString("v_icd9cm_code"),
                        resultSet.getString("v_icd9cm_name"),
                        resultSet.getString("ss_procedure_id")),
                registrationId);
    }

    private String toFhirDate(Timestamp timestamp) {
        if (timestamp == null) {
            return FHIR_DATE.format(java.time.OffsetDateTime.now(JAKARTA).truncatedTo(ChronoUnit.SECONDS));
        }
        return FHIR_DATE.format(timestamp.toInstant().atZone(JAKARTA).toOffsetDateTime()
                .truncatedTo(ChronoUnit.SECONDS));
    }

    private String cleanDisplay(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static final class ClinicalContext {
        private final int regId;
        private final Timestamp registrationDate;
        private final String encounterId;
        private final String ihsNumber;

        private ClinicalContext(int regId, Timestamp registrationDate, String encounterId, String ihsNumber) {
            this.regId = regId;
            this.registrationDate = registrationDate;
            this.encounterId = encounterId;
            this.ihsNumber = ihsNumber;
        }
    }

    private static final class DiagnosisRow {
        private final int rowId;
        private final String code;
        private final String name;
        private final String conditionId;

        private DiagnosisRow(int rowId, String code, String name, String conditionId) {
            this.rowId = rowId;
            this.code = code;
            this.name = name;
            this.conditionId = conditionId;
        }
    }

    private static final class ProcedureRow {
        private final int rowId;
        private final String code;
        private final String name;
        private final String procedureId;

        private ProcedureRow(int rowId, String code, String name, String procedureId) {
            this.rowId = rowId;
            this.code = code;
            this.name = name;
            this.procedureId = procedureId;
        }
    }
}
