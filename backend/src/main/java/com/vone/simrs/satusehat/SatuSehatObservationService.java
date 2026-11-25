package com.vone.simrs.satusehat;

import com.vone.simrs.auth.LegacyAuthService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpSession;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sinkronisasi hasil laboratorium dan tanda-tanda vital (TTV) menjadi resource
 * FHIR {@code Observation} SATUSEHAT.
 */
@Service
public class SatuSehatObservationService {

    private static final String OBSERVATION_CATEGORY_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/observation-category";
    private static final String LOINC_SYSTEM = "http://loinc.org";
    private static final String UCUM_SYSTEM = "http://unitsofmeasure.org";
    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter FHIR_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final SatuSehatClient satuSehatClient;

    public SatuSehatObservationService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService,
            SatuSehatClient satuSehatClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.satuSehatClient = satuSehatClient;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ==================================================================
    // Hasil laboratorium
    // ==================================================================

    /** Sinkronkan seluruh hasil lab registrasi menjadi Observation. */
    public SatuSehatClinicalSyncResponse syncObservations(String registrationCode) {
        ClinicalContext context = loadContext(registrationCode);
        List<ObservationRow> rows = loadObservations(context.regId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada hasil laboratorium untuk registrasi ini.");
        }

        int created = 0;
        int skipped = 0;
        List<String> withoutLoinc = new ArrayList<>();
        for (ObservationRow row : rows) {
            if (StringUtils.hasText(row.observationId)) {
                skipped++;
                continue;
            }
            if (!StringUtils.hasText(row.loincCode)) {
                withoutLoinc.add(row.detailId + " (treatment " + row.treatmentId + ")");
                continue;
            }
            Observation observation = buildLabObservation(context, row);
            Observation saved = satuSehatClient.create(observation, Observation.class);
            String observationId = saved.getIdElement() == null ? null : saved.getIdElement().getIdPart();
            if (!StringUtils.hasText(observationId)) {
                throw new SatuSehatException("SATUSEHAT tidak mengembalikan Observation ID.");
            }
            jdbcTemplate.update(
                    "update tb_laboratory_result_detail set ss_observation_id = ? "
                            + "where n_lab_rslt_det_id = ?",
                    observationId, row.detailId);
            created++;
        }

        String message = "Sinkronisasi hasil lab selesai.";
        if (!withoutLoinc.isEmpty()) {
            message = message + " Dilewati (belum ada kode LOINC): " + String.join(", ", withoutLoinc) + ".";
        }
        return new SatuSehatClinicalSyncResponse(registrationCode, context.encounterId, "Observation",
                created, skipped, message);
    }

    private Observation buildLabObservation(ClinicalContext context, ObservationRow row) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.addCategory(new CodeableConcept(
                new Coding(OBSERVATION_CATEGORY_SYSTEM, "laboratory", "Laboratory")));
        observation.setCode(new CodeableConcept(
                new Coding(LOINC_SYSTEM, row.loincCode, row.loincDisplay)));
        setSubjectEncounterPerformer(observation, context);

        String date = toFhirDate(row.resultDate);
        observation.setEffective(new DateTimeType(date));
        observation.setIssued(new Date());

        if (StringUtils.hasText(row.value)) {
            BigDecimal numeric = tryParseNumber(row.value);
            if (numeric != null) {
                String unit = StringUtils.hasText(row.unit) ? row.unit.trim() : null;
                String ucum = StringUtils.hasText(row.ucumCode) ? row.ucumCode.trim() : unit;
                observation.setValue(buildQuantity(numeric, unit, ucum));
            } else {
                observation.setValue(new StringType(row.value));
            }
        }
        return observation;
    }

    // ==================================================================
    // Tanda-tanda vital (TTV)
    // ==================================================================

    /** Sinkronkan data tanda vital registrasi menjadi Observation. */
    public SatuSehatClinicalSyncResponse syncVitalSigns(String registrationCode) {
        ClinicalContext context = loadContext(registrationCode);
        List<VitalRow> rows = loadVitalSigns(context.regId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada data tanda vital untuk registrasi ini.");
        }
        Set<String> existing = loadExistingVitalMappings(context.regId);

        Counter counter = new Counter();
        for (VitalRow row : rows) {
            for (VitalValue vital : row.values()) {
                String key = row.ttvId + "|" + vital.loincCode;
                if (existing.contains(key)) {
                    counter.skipped++;
                    continue;
                }
                Observation observation = buildVitalObservation(context, row.ttvDate, vital);
                Observation saved = satuSehatClient.create(observation, Observation.class);
                String observationId = saved.getIdElement() == null ? null : saved.getIdElement().getIdPart();
                if (!StringUtils.hasText(observationId)) {
                    throw new SatuSehatException("SATUSEHAT tidak mengembalikan Observation ID.");
                }
                jdbcTemplate.update(
                        "insert into tb_ttv_ss_observation (n_ttv_id, v_loinc_code, ss_observation_id) "
                                + "values (?, ?, ?)",
                        row.ttvId, vital.loincCode, observationId);
                existing.add(key);
                counter.created++;
            }
        }

        return new SatuSehatClinicalSyncResponse(registrationCode, context.encounterId, "Observation",
                counter.created, counter.skipped, "Sinkronisasi tanda vital selesai.");
    }

    private Observation buildVitalObservation(ClinicalContext context, Timestamp date, VitalValue vital) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.addCategory(new CodeableConcept(
                new Coding(OBSERVATION_CATEGORY_SYSTEM, "vital-signs", "Vital Signs")));
        observation.setCode(new CodeableConcept(
                new Coding(LOINC_SYSTEM, vital.loincCode, vital.loincDisplay)));
        setSubjectEncounterPerformer(observation, context);

        String effective = toFhirDate(date);
        observation.setEffective(new DateTimeType(effective));
        observation.setIssued(new Date());
        observation.setValue(buildQuantity(vital.value, vital.unit, vital.ucum));
        return observation;
    }

    private void setSubjectEncounterPerformer(Observation observation, ClinicalContext context) {
        observation.setSubject(new Reference("Patient/" + context.ihsNumber));
        observation.setEncounter(new Reference("Encounter/" + context.encounterId));
        if (StringUtils.hasText(context.practitionerId)) {
            observation.addPerformer(new Reference("Practitioner/" + context.practitionerId));
        }
    }

    private Quantity buildQuantity(BigDecimal value, String unit, String ucum) {
        Quantity quantity = new Quantity();
        quantity.setValue(value);
        quantity.setSystem(UCUM_SYSTEM);
        if (unit != null) {
            quantity.setUnit(unit);
        }
        if (ucum != null) {
            quantity.setCode(ucum);
        }
        return quantity;
    }

    // ==================================================================
    // Data lokal
    // ==================================================================

    private ClinicalContext loadContext(String registrationCode) {
        if (!StringUtils.hasText(registrationCode)) {
            throw new IllegalArgumentException("KODE REGISTRASI HARUS DI ISI!");
        }
        List<ClinicalContext> rows = jdbcTemplate.query(
                "select r.n_reg_id, r.ss_encounter_id, mr.ihs_number, d.ss_practitioner_id "
                        + "from tb_registration r "
                        + "join tb_medical_record mr on mr.n_mr_id = r.n_mr_id "
                        + "left join ms_doctor d on d.n_staff_id = r.n_staff_id "
                        + "where r.v_reg_secondary_id = ? order by r.n_reg_id desc limit 1",
                (resultSet, rowNum) -> new ClinicalContext(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("ss_encounter_id"),
                        resultSet.getString("ihs_number"),
                        resultSet.getString("ss_practitioner_id")),
                registrationCode.trim());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Registrasi tidak ditemukan: " + registrationCode);
        }
        ClinicalContext context = rows.get(0);
        if (!StringUtils.hasText(context.ihsNumber)) {
            throw new IllegalArgumentException("Pasien belum punya IHS number.");
        }
        if (!StringUtils.hasText(context.encounterId)) {
            throw new IllegalArgumentException("Encounter belum dibuat. Sinkronkan Encounter terlebih dahulu.");
        }
        return context;
    }

    private List<ObservationRow> loadObservations(int registrationId) {
        return jdbcTemplate.query(
                "select det.n_lab_rslt_det_id, det.n_treatment_id, det.v_lab_rslt_quantify, det.ss_observation_id, "
                        + "coalesce(md.v_loinc_code, mt.v_loinc_code) as loinc_code, "
                        + "coalesce(md.v_loinc_display, mt.v_loinc_display) as loinc_display, "
                        + "coalesce(md.v_ucum_code, mt.v_ucum_code) as ucum_code, "
                        + "coalesce(md.v_quantify, mt.v_lab_test_quantify) as unit, "
                        + "res.d_whn_create as result_date "
                        + "from tb_laboratory_result_detail det "
                        + "join tb_laboratory_result res on res.n_lab_rslt_id = det.n_lab_rslt_id "
                        + "join tb_examination e on e.n_exam_id = res.n_exam_id "
                        + "left join ms_lab_treatment_detil md on md.n_lab_detil_id = det.n_lab_detil_id "
                        + "left join ms_lab_test_detail mt on mt.n_treatment_id = det.n_treatment_id "
                        + "where e.n_reg_id = ? order by det.n_lab_rslt_det_id",
                (resultSet, rowNum) -> new ObservationRow(
                        resultSet.getInt("n_lab_rslt_det_id"),
                        resultSet.getInt("n_treatment_id"),
                        resultSet.getString("v_lab_rslt_quantify"),
                        resultSet.getString("ss_observation_id"),
                        resultSet.getString("loinc_code"),
                        resultSet.getString("loinc_display"),
                        resultSet.getString("ucum_code"),
                        resultSet.getString("unit"),
                        resultSet.getTimestamp("result_date")),
                registrationId);
    }

    private List<VitalRow> loadVitalSigns(int registrationId) {
        return jdbcTemplate.query(
                "select n_ttv_id, d_ttv_date, n_systole, n_diastole, n_pulse, n_respiratory_rate, "
                        + "n_temperature, n_spo2, n_weight, n_height, n_pain_scale "
                        + "from tb_ttv where n_reg_id = ? order by d_ttv_date",
                (resultSet, rowNum) -> new VitalRow(
                        resultSet.getLong("n_ttv_id"),
                        resultSet.getTimestamp("d_ttv_date"),
                        (Integer) resultSet.getObject("n_systole"),
                        (Integer) resultSet.getObject("n_diastole"),
                        (Integer) resultSet.getObject("n_pulse"),
                        (Integer) resultSet.getObject("n_respiratory_rate"),
                        resultSet.getBigDecimal("n_temperature"),
                        (Integer) resultSet.getObject("n_spo2"),
                        resultSet.getBigDecimal("n_weight"),
                        resultSet.getBigDecimal("n_height"),
                        (Integer) resultSet.getObject("n_pain_scale")),
                registrationId);
    }

    private Set<String> loadExistingVitalMappings(int registrationId) {
        List<String> keys = jdbcTemplate.queryForList(
                "select m.n_ttv_id || '|' || m.v_loinc_code "
                        + "from tb_ttv_ss_observation m "
                        + "join tb_ttv t on t.n_ttv_id = m.n_ttv_id "
                        + "where t.n_reg_id = ? and m.ss_observation_id is not null",
                String.class, registrationId);
        return new HashSet<>(keys);
    }

    private BigDecimal tryParseNumber(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String toFhirDate(Timestamp timestamp) {
        if (timestamp == null) {
            return FHIR_DATE.format(java.time.OffsetDateTime.now(JAKARTA).truncatedTo(ChronoUnit.SECONDS));
        }
        return FHIR_DATE.format(timestamp.toInstant().atZone(JAKARTA).toOffsetDateTime()
                .truncatedTo(ChronoUnit.SECONDS));
    }

    private static final class Counter {
        private int created;
        private int skipped;
    }

    private static final class ClinicalContext {
        private final int regId;
        private final String encounterId;
        private final String ihsNumber;
        private final String practitionerId;

        private ClinicalContext(int regId, String encounterId, String ihsNumber, String practitionerId) {
            this.regId = regId;
            this.encounterId = encounterId;
            this.ihsNumber = ihsNumber;
            this.practitionerId = practitionerId;
        }
    }

    private static final class ObservationRow {
        private final int detailId;
        private final int treatmentId;
        private final String value;
        private final String observationId;
        private final String loincCode;
        private final String loincDisplay;
        private final String ucumCode;
        private final String unit;
        private final Timestamp resultDate;

        private ObservationRow(int detailId, int treatmentId, String value, String observationId,
                String loincCode, String loincDisplay, String ucumCode, String unit,
                Timestamp resultDate) {
            this.detailId = detailId;
            this.treatmentId = treatmentId;
            this.value = value;
            this.observationId = observationId;
            this.loincCode = loincCode;
            this.loincDisplay = loincDisplay;
            this.ucumCode = ucumCode;
            this.unit = unit;
            this.resultDate = resultDate;
        }
    }

    private static final class VitalRow {
        private final long ttvId;
        private final Timestamp ttvDate;
        private final Integer systole;
        private final Integer diastole;
        private final Integer pulse;
        private final Integer respiratoryRate;
        private final BigDecimal temperature;
        private final Integer spo2;
        private final BigDecimal weight;
        private final BigDecimal height;
        private final Integer painScale;

        private VitalRow(long ttvId, Timestamp ttvDate, Integer systole, Integer diastole, Integer pulse,
                Integer respiratoryRate, BigDecimal temperature, Integer spo2, BigDecimal weight,
                BigDecimal height, Integer painScale) {
            this.ttvId = ttvId;
            this.ttvDate = ttvDate;
            this.systole = systole;
            this.diastole = diastole;
            this.pulse = pulse;
            this.respiratoryRate = respiratoryRate;
            this.temperature = temperature;
            this.spo2 = spo2;
            this.weight = weight;
            this.height = height;
            this.painScale = painScale;
        }

        private List<VitalValue> values() {
            List<VitalValue> list = new ArrayList<>();
            if (systole != null) {
                list.add(new VitalValue("8480-6", "Systolic blood pressure", BigDecimal.valueOf(systole), "mmHg", "mm[Hg]"));
            }
            if (diastole != null) {
                list.add(new VitalValue("8462-4", "Diastolic blood pressure", BigDecimal.valueOf(diastole), "mmHg", "mm[Hg]"));
            }
            if (pulse != null) {
                list.add(new VitalValue("8867-4", "Heart rate", BigDecimal.valueOf(pulse), "/menit", "/min"));
            }
            if (respiratoryRate != null) {
                list.add(new VitalValue("9279-1", "Respiratory rate", BigDecimal.valueOf(respiratoryRate), "/menit", "/min"));
            }
            if (temperature != null) {
                list.add(new VitalValue("8310-5", "Body temperature", temperature, "C", "Cel"));
            }
            if (spo2 != null) {
                list.add(new VitalValue("2708-6", "Oxygen saturation in Arterial blood", BigDecimal.valueOf(spo2), "%", "%"));
            }
            if (weight != null) {
                list.add(new VitalValue("29463-7", "Body weight", weight, "kg", "kg"));
            }
            if (height != null) {
                list.add(new VitalValue("8302-2", "Body height", height, "cm", "cm"));
            }
            if (painScale != null) {
                list.add(new VitalValue("72514-3", "Pain severity", BigDecimal.valueOf(painScale), "score", "{score}"));
            }
            return list;
        }
    }

    private static final class VitalValue {
        private final String loincCode;
        private final String loincDisplay;
        private final BigDecimal value;
        private final String unit;
        private final String ucum;

        private VitalValue(String loincCode, String loincDisplay, BigDecimal value, String unit, String ucum) {
            this.loincCode = loincCode;
            this.loincDisplay = loincDisplay;
            this.value = value;
            this.unit = unit;
            this.ucum = ucum;
        }
    }
}
