package com.vone.simrs.satusehat;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sinkronisasi kunjungan ({@code tb_registration}) ke resource FHIR
 * {@code Encounter} SATUSEHAT.
 *
 * <p>Class Encounter ditentukan otomatis:
 * <ul>
 *   <li>{@code IMP} (inpatient) -> registrasi rawat inap (tanpa unit / ada room reservation)</li>
 *   <li>{@code EMER} (emergency) -> unit IGD/UGD</li>
 *   <li>{@code AMB} (ambulatory) -> rawat jalan (default)</li>
 * </ul>
 * Location rawat inap diambil dari bangsal (via bed occupancy) atau
 * {@code satusehat.inpatient-location-id}.
 */
@Service
public class SatuSehatEncounterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SatuSehatEncounterService.class);

    private static final String PARTICIPATION_TYPE_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/v3-ParticipationType";
    private static final String ACT_CODE_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/v3-ActCode";
    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter FHIR_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final SatuSehatClient satuSehatClient;
    private final SatuSehatProperties properties;

    public SatuSehatEncounterService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService,
            SatuSehatClient satuSehatClient, SatuSehatProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.satuSehatClient = satuSehatClient;
        this.properties = properties;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Buat/sinkronkan Encounter untuk sebuah registrasi.
     * Bila Encounter sudah pernah dibuat, kembalikan yang ada (idempotent).
     */
    public SatuSehatEncounterSyncResponse syncEncounter(String registrationCode) {
        RegistrationRow row = loadRegistration(registrationCode);
        validateBasic(row);
        String locationId = resolveLocation(row);
        if (!StringUtils.hasText(locationId)) {
            throw new IllegalArgumentException(
                    "Location ID belum tersedia. Petakan unit (rawat jalan/IGD) atau bangsal (rawat inap) "
                            + "ke Location ID SatuSehat, atau set satusehat.inpatient-location-id.");
        }

        if (StringUtils.hasText(row.encounterId)) {
            return new SatuSehatEncounterSyncResponse(row.registrationCode, row.ihsNumber,
                    row.encounterId, row.encounterStatus, "EXISTING",
                    "Encounter sudah pernah dibuat untuk registrasi ini.");
        }

        String encounterClass = determineClass(row);
        Encounter encounter = toFhirEncounter(row, locationId, encounterClass);
        Encounter created = satuSehatClient.create(encounter, Encounter.class);
        String encounterId = created.getIdElement() == null ? null : created.getIdElement().getIdPart();
        if (!StringUtils.hasText(encounterId)) {
            throw new SatuSehatException("SATUSEHAT tidak mengembalikan Encounter ID.");
        }
        String status = created.getStatus() == null ? "arrived" : created.getStatus().toCode();
        persistEncounter(row.regId, encounterId, status);

        return new SatuSehatEncounterSyncResponse(row.registrationCode, row.ihsNumber,
                encounterId, status, "CREATED",
                "Encounter (" + encounterClass + ") berhasil dibuat di SATUSEHAT.");
    }

    /** Tandai Encounter selesai (status {@code finished}) dan isi period.end. */
    public SatuSehatEncounterSyncResponse finishEncounter(String registrationCode) {
        RegistrationRow row = loadRegistration(registrationCode);
        if (!StringUtils.hasText(row.encounterId)) {
            throw new IllegalArgumentException(
                    "Encounter belum dibuat untuk registrasi ini. Jalankan sinkronisasi terlebih dahulu.");
        }

        Encounter encounter = satuSehatClient.read("Encounter", row.encounterId, Encounter.class);
        String end = nowFhirDate();

        // SATUSEHAT mewajibkan setiap statusHistory.period memiliki start dan end.
        if (encounter.getPeriod() == null) {
            encounter.setPeriod(new Period());
        }
        if (!encounter.getPeriod().hasStart()) {
            encounter.getPeriod().setStartElement(new DateTimeType(end));
        }
        encounter.getPeriod().setEndElement(new DateTimeType(end));

        for (Encounter.StatusHistoryComponent history : encounter.getStatusHistory()) {
            if (history.getPeriod() == null) {
                history.setPeriod(new Period());
            }
            if (!history.getPeriod().hasStart()) {
                history.getPeriod().setStartElement(new DateTimeType(end));
            }
            history.getPeriod().setEndElement(new DateTimeType(end));
        }
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.addStatusHistory()
                .setStatus(Encounter.EncounterStatus.FINISHED)
                .setPeriod(new Period()
                        .setStartElement(new DateTimeType(end))
                        .setEndElement(new DateTimeType(end)));

        // SATUSEHAT mewajibkan Encounter (finished) memiliki minimal satu diagnosis.
        List<String> conditionIds = loadConditionIds(row.regId);
        if (conditionIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Encounter tidak dapat diselesaikan karena belum ada diagnosa (Condition) "
                            + "pada kunjungan ini. Sinkronkan diagnosa terlebih dahulu.");
        }
        encounter.getDiagnosis().clear();
        int rank = 1;
        for (String conditionId : conditionIds) {
            Encounter.DiagnosisComponent diagnosis = encounter.addDiagnosis();
            diagnosis.setCondition(new Reference("Condition/" + conditionId));
            diagnosis.setUse(new CodeableConcept(new Coding(
                    "http://terminology.hl7.org/CodeSystem/diagnosis-role", "DD",
                    "Discharge diagnosis")));
            diagnosis.setRank(rank++);
        }

        Encounter updated = satuSehatClient.update(encounter, Encounter.class);
        String status = updated.getStatus() == null ? "finished" : updated.getStatus().toCode();
        persistEncounter(row.regId, row.encounterId, status);

        return new SatuSehatEncounterSyncResponse(row.registrationCode, row.ihsNumber,
                row.encounterId, status, "FINISHED", "Encounter ditandai selesai di SATUSEHAT.");
    }

    // ------------------------------------------------------------------
    // Class & Location
    // ------------------------------------------------------------------

    private String determineClass(RegistrationRow row) {
        if (row.unitId == null || row.hasRoomReservation) {
            return "IMP"; // rawat inap
        }
        if ("IGD".equalsIgnoreCase(row.unitCode) || "UGD".equalsIgnoreCase(row.unitCode)) {
            return "EMER"; // gawat darurat
        }
        return "AMB"; // rawat jalan
    }

    private String classDisplay(String encounterClass) {
        switch (encounterClass) {
            case "IMP":
                return "inpatient encounter";
            case "EMER":
                return "emergency";
            default:
                return "ambulatory";
        }
    }

    private String resolveLocation(RegistrationRow row) {
        if (StringUtils.hasText(row.unitLocationId)) {
            return row.unitLocationId;
        }
        String hallLocation = findHallLocation(row.regId);
        if (StringUtils.hasText(hallLocation)) {
            return hallLocation;
        }
        return StringUtils.hasText(properties.getInpatientLocationId())
                ? properties.getInpatientLocationId().trim() : null;
    }

    private String findHallLocation(int registrationId) {
        try {
            List<String> rows = jdbcTemplate.queryForList(
                    "select h.ss_location_id "
                            + "from tb_bed_occupancy bo "
                            + "join ms_bed b on b.n_bed_id = bo.n_bed_primary_id "
                            + "join ms_room r on r.n_room_id = b.n_room_id "
                            + "join ms_hall h on h.n_hall_id = r.n_hall_id "
                            + "where bo.n_reg_primary_id = ? and bo.d_check_out_time is null "
                            + "and h.ss_location_id is not null "
                            + "order by bo.d_check_in_time desc limit 1",
                    String.class, registrationId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (RuntimeException exception) {
            LOGGER.warn("Gagal mencari Location bangsal untuk registrasi {}: {}",
                    registrationId, exception.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Pemetaan ke FHIR
    // ------------------------------------------------------------------

    private Encounter toFhirEncounter(RegistrationRow row, String locationId, String encounterClass) {
        String orgId = properties.getOrganizationId();
        Encounter encounter = new Encounter();

        encounter.addIdentifier()
                .setSystem("http://sys-ids.kemkes.go.id/encounter/" + orgId)
                .setValue(row.registrationCode);

        encounter.setStatus(Encounter.EncounterStatus.ARRIVED);
        encounter.setClass_(new Coding(ACT_CODE_SYSTEM, encounterClass, classDisplay(encounterClass)));

        encounter.setSubject(new Reference("Patient/" + row.ihsNumber).setDisplay(row.patientName));

        Encounter.EncounterParticipantComponent participant = encounter.addParticipant();
        participant.addType(new CodeableConcept(
                new Coding(PARTICIPATION_TYPE_SYSTEM, "ATND", "attender")));
        participant.setIndividual(new Reference("Practitioner/" + row.practitionerId)
                .setDisplay(row.doctorName));

        String start = toFhirDate(row.registrationDate);
        encounter.setPeriod(new Period().setStartElement(new DateTimeType(start)));

        Encounter.EncounterLocationComponent location = encounter.addLocation();
        location.setLocation(new Reference("Location/" + locationId));
        location.setPeriod(new Period().setStartElement(new DateTimeType(start)));

        encounter.setServiceProvider(new Reference("Organization/" + orgId));

        encounter.addStatusHistory()
                .setStatus(Encounter.EncounterStatus.ARRIVED)
                .setPeriod(new Period().setStartElement(new DateTimeType(start)));

        return encounter;
    }

    // ------------------------------------------------------------------
    // Validasi & data lokal
    // ------------------------------------------------------------------

    private void validateBasic(RegistrationRow row) {
        if (!StringUtils.hasText(row.ihsNumber)) {
            throw new IllegalArgumentException(
                    "Pasien belum punya IHS number. Sinkronkan data pasien terlebih dahulu.");
        }
        if (!StringUtils.hasText(properties.getOrganizationId())) {
            throw new IllegalStateException("satusehat.organization-id belum dikonfigurasi.");
        }
        if (!StringUtils.hasText(row.practitionerId)) {
            throw new IllegalArgumentException(
                    "Dokter registrasi belum dipetakan ke Practitioner ID SatuSehat "
                            + "(ms_doctor.ss_practitioner_id).");
        }
    }

    private RegistrationRow loadRegistration(String registrationCode) {
        if (!StringUtils.hasText(registrationCode)) {
            throw new IllegalArgumentException("KODE REGISTRASI HARUS DI ISI!");
        }
        List<RegistrationRow> rows = jdbcTemplate.query(
                "select r.n_reg_id, r.v_reg_secondary_id, r.d_registration_date, "
                        + "r.ss_encounter_id, r.ss_encounter_status, "
                        + "mr.v_mr_code, mr.ihs_number, p.v_patient_name, "
                        + "u.n_unit_id, u.v_unit_code, u.v_unit_name, u.ss_location_id, "
                        + "s.n_staff_id, s.v_staff_name, d.ss_practitioner_id, "
                        + "exists (select 1 from tb_room_reservation rr where rr.n_reg_id = r.n_reg_id) as has_room "
                        + "from tb_registration r "
                        + "join tb_medical_record mr on mr.n_mr_id = r.n_mr_id "
                        + "join ms_patient p on p.n_patient_id = mr.n_patient_id "
                        + "left join ms_unit u on u.n_unit_id = r.n_unit_id "
                        + "left join ms_staff s on s.n_staff_id = r.n_staff_id "
                        + "left join ms_doctor d on d.n_staff_id = r.n_staff_id "
                        + "where r.v_reg_secondary_id = ? "
                        + "order by r.n_reg_id desc limit 1",
                (resultSet, rowNum) -> new RegistrationRow(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_reg_secondary_id"),
                        resultSet.getTimestamp("d_registration_date"),
                        resultSet.getString("ss_encounter_id"),
                        resultSet.getString("ss_encounter_status"),
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("ihs_number"),
                        resultSet.getString("v_patient_name"),
                        (Integer) resultSet.getObject("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        resultSet.getString("ss_location_id"),
                        (Integer) resultSet.getObject("n_staff_id"),
                        resultSet.getString("v_staff_name"),
                        resultSet.getString("ss_practitioner_id"),
                        resultSet.getBoolean("has_room")),
                registrationCode.trim());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Registrasi tidak ditemukan: " + registrationCode);
        }
        return rows.get(0);
    }

    private List<String> loadConditionIds(int registrationId) {
        return jdbcTemplate.queryForList(
                "select i.ss_condition_id from tb_diagnose d "
                        + "join tb_icd_diagnose i on i.n_diagnose_id = d.n_diagnose_id "
                        + "where d.n_reg_id = ? and i.ss_condition_id is not null "
                        + "order by i.n_icd_diagnose_id",
                String.class, registrationId);
    }

    private void persistEncounter(int registrationId, String encounterId, String status) {
        jdbcTemplate.update(
                "update tb_registration set ss_encounter_id = ?, ss_encounter_status = ?, "
                        + "d_ss_encounter_sync = now() where n_reg_id = ?",
                encounterId, status, registrationId);
    }

    private String toFhirDate(Timestamp timestamp) {
        if (timestamp == null) {
            return nowFhirDate();
        }
        return FHIR_DATE.format(timestamp.toInstant().atZone(JAKARTA).toOffsetDateTime()
                .truncatedTo(ChronoUnit.SECONDS));
    }

    private String nowFhirDate() {
        return FHIR_DATE.format(OffsetDateTime.now(JAKARTA).truncatedTo(ChronoUnit.SECONDS));
    }

    private static final class RegistrationRow {
        private final int regId;
        private final String registrationCode;
        private final Timestamp registrationDate;
        private final String encounterId;
        private final String encounterStatus;
        private final String mrCode;
        private final String ihsNumber;
        private final String patientName;
        private final Integer unitId;
        private final String unitCode;
        private final String unitName;
        private final String unitLocationId;
        private final Integer staffId;
        private final String doctorName;
        private final String practitionerId;
        private final boolean hasRoomReservation;

        private RegistrationRow(int regId, String registrationCode, Timestamp registrationDate,
                String encounterId, String encounterStatus, String mrCode, String ihsNumber,
                String patientName, Integer unitId, String unitCode, String unitName, String unitLocationId,
                Integer staffId, String doctorName, String practitionerId, boolean hasRoomReservation) {
            this.regId = regId;
            this.registrationCode = registrationCode;
            this.registrationDate = registrationDate;
            this.encounterId = encounterId;
            this.encounterStatus = encounterStatus;
            this.mrCode = mrCode;
            this.ihsNumber = ihsNumber;
            this.patientName = patientName;
            this.unitId = unitId;
            this.unitCode = unitCode;
            this.unitName = unitName;
            this.unitLocationId = unitLocationId;
            this.staffId = staffId;
            this.doctorName = doctorName;
            this.practitionerId = practitionerId;
            this.hasRoomReservation = hasRoomReservation;
        }
    }
}
