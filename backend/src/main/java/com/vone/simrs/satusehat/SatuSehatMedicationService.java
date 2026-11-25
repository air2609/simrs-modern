package com.vone.simrs.satusehat;

import com.vone.simrs.auth.LegacyAuthService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sinkronisasi obat ({@code tb_item_trx} yang itemnya grup OBAT) ke SATUSEHAT:
 * membuat resource {@code Medication} (kode KFA) lalu mengirim
 * {@code MedicationRequest} dan/atau {@code MedicationDispense}.
 */
@Service
public class SatuSehatMedicationService {

    private static final String KFA_SYSTEM = "http://sys-ids.kemkes.go.id/kfa";
    private static final String MEDICATION_TYPE_EXT =
            "https://fhir.kemkes.go.id/r4/StructureDefinition/MedicationType";
    private static final String MEDICATION_TYPE_SYSTEM =
            "http://terminology.kemkes.go.id/CodeSystem/medication-type";
    private static final String REQUEST_CATEGORY_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/medicationrequest-category";
    private static final String UCUM_SYSTEM = "http://unitsofmeasure.org";
    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter FHIR_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final SatuSehatClient satuSehatClient;
    private final SatuSehatProperties properties;

    public SatuSehatMedicationService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService,
            SatuSehatClient satuSehatClient, SatuSehatProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.satuSehatClient = satuSehatClient;
        this.properties = properties;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Sinkronkan resep obat menjadi MedicationRequest. */
    public SatuSehatClinicalSyncResponse syncMedicationRequests(String registrationCode) {
        ClinicalContext context = loadContext(registrationCode);
        List<MedicationLine> lines = loadMedicationLines(context.regId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada item obat untuk registrasi ini.");
        }

        int created = 0;
        int skipped = 0;
        for (MedicationLine line : lines) {
            requireKfa(line);
            if (StringUtils.hasText(line.requestId)) {
                skipped++;
                continue;
            }
            String medicationId = ensureMedication(line);
            MedicationRequest request = buildRequest(context, line, medicationId);
            MedicationRequest saved = satuSehatClient.create(request, MedicationRequest.class);
            String requestId = saved.getIdElement() == null ? null : saved.getIdElement().getIdPart();
            if (!StringUtils.hasText(requestId)) {
                throw new SatuSehatException("SATUSEHAT tidak mengembalikan MedicationRequest ID.");
            }
            jdbcTemplate.update(
                    "update tb_item_trx set ss_medication_request_id = ? where n_item_trx_id = ?",
                    requestId, line.trxId);
            created++;
        }
        return new SatuSehatClinicalSyncResponse(registrationCode, context.encounterId,
                "MedicationRequest", created, skipped, "Sinkronisasi resep obat selesai.");
    }

    /** Sinkronkan penyerahan obat menjadi MedicationDispense. */
    public SatuSehatClinicalSyncResponse syncMedicationDispenses(String registrationCode) {
        ClinicalContext context = loadContext(registrationCode);
        List<MedicationLine> lines = loadMedicationLines(context.regId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada item obat untuk registrasi ini.");
        }

        int created = 0;
        int skipped = 0;
        for (MedicationLine line : lines) {
            requireKfa(line);
            if (!StringUtils.hasText(line.requestId)) {
                throw new IllegalArgumentException(
                        "MedicationDispense memerlukan MedicationRequest. "
                                + "Sinkronkan resep obat terlebih dahulu.");
            }
            if (StringUtils.hasText(line.dispenseId)) {
                skipped++;
                continue;
            }
            String medicationId = ensureMedication(line);
            MedicationDispense dispense = buildDispense(context, line, medicationId);
            MedicationDispense saved = satuSehatClient.create(dispense, MedicationDispense.class);
            String dispenseId = saved.getIdElement() == null ? null : saved.getIdElement().getIdPart();
            if (!StringUtils.hasText(dispenseId)) {
                throw new SatuSehatException("SATUSEHAT tidak mengembalikan MedicationDispense ID.");
            }
            jdbcTemplate.update(
                    "update tb_item_trx set ss_medication_dispense_id = ? where n_item_trx_id = ?",
                    dispenseId, line.trxId);
            created++;
        }
        return new SatuSehatClinicalSyncResponse(registrationCode, context.encounterId,
                "MedicationDispense", created, skipped, "Sinkronisasi penyerahan obat selesai.");
    }

    // ------------------------------------------------------------------
    // Pemetaan ke FHIR
    // ------------------------------------------------------------------

    private String ensureMedication(MedicationLine line) {
        if (StringUtils.hasText(line.medicationId)) {
            return line.medicationId;
        }
        String orgId = properties.getOrganizationId();
        Medication medication = new Medication();
        medication.addIdentifier()
                .setSystem("http://sys-ids.kemkes.go.id/medication/" + orgId)
                .setUse(org.hl7.fhir.r4.model.Identifier.IdentifierUse.OFFICIAL)
                .setValue(line.itemCode);
        medication.setCode(new CodeableConcept(new Coding(KFA_SYSTEM, line.kfaCode, line.kfaDisplay)));
        medication.setStatus(Medication.MedicationStatus.ACTIVE);
        Extension type = new Extension(MEDICATION_TYPE_EXT);
        type.setValue(new CodeableConcept(new Coding(MEDICATION_TYPE_SYSTEM, "NC", "Non-compound")));
        medication.addExtension(type);

        Medication saved = satuSehatClient.create(medication, Medication.class);
        String medicationId = saved.getIdElement() == null ? null : saved.getIdElement().getIdPart();
        if (!StringUtils.hasText(medicationId)) {
            throw new SatuSehatException("SATUSEHAT tidak mengembalikan Medication ID.");
        }
        jdbcTemplate.update("update ms_item set ss_medication_id = ? where n_item_id = ?",
                medicationId, line.itemId);
        return medicationId;
    }

    private MedicationRequest buildRequest(ClinicalContext context, MedicationLine line, String medicationId) {
        MedicationRequest request = new MedicationRequest();
        request.addIdentifier()
                .setSystem("http://sys-ids.kemkes.go.id/prescription/" + properties.getOrganizationId())
                .setUse(org.hl7.fhir.r4.model.Identifier.IdentifierUse.OFFICIAL)
                .setValue("TRX-" + line.trxId);
        request.setStatus(MedicationRequest.MedicationRequestStatus.COMPLETED);
        request.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        request.addCategory(new CodeableConcept(
                new Coding(REQUEST_CATEGORY_SYSTEM, "outpatient", "Outpatient")));
        request.setMedication(new Reference("Medication/" + medicationId));
        request.setSubject(new Reference("Patient/" + context.ihsNumber));
        request.setEncounter(new Reference("Encounter/" + context.encounterId));
        if (StringUtils.hasText(context.practitionerId)) {
            request.setRequester(new Reference("Practitioner/" + context.practitionerId));
        }
        request.setAuthoredOn(new Date());
        if (StringUtils.hasText(line.aturanPakai)) {
            Dosage dosage = request.addDosageInstruction();
            dosage.setText(line.aturanPakai);
        }
        request.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent());
        request.getDispenseRequest().setQuantity(buildQuantity(line));
        return request;
    }

    private MedicationDispense buildDispense(ClinicalContext context, MedicationLine line, String medicationId) {
        MedicationDispense dispense = new MedicationDispense();
        dispense.addIdentifier()
                .setSystem("http://sys-ids.kemkes.go.id/prescription/" + properties.getOrganizationId())
                .setUse(org.hl7.fhir.r4.model.Identifier.IdentifierUse.OFFICIAL)
                .setValue("DSP-" + line.trxId);
        dispense.setStatus(MedicationDispense.MedicationDispenseStatus.COMPLETED);
        dispense.setMedication(new Reference("Medication/" + medicationId));
        dispense.addAuthorizingPrescription(new Reference("MedicationRequest/" + line.requestId));
        dispense.setSubject(new Reference("Patient/" + context.ihsNumber));
        dispense.setContext(new Reference("Encounter/" + context.encounterId));
        if (StringUtils.hasText(context.practitionerId)) {
            dispense.addPerformer().setActor(new Reference("Practitioner/" + context.practitionerId));
        }
        dispense.setQuantity(buildQuantity(line));
        dispense.setWhenHandedOver(new Date());
        if (StringUtils.hasText(line.aturanPakai)) {
            dispense.addDosageInstruction().setText(line.aturanPakai);
        }
        return dispense;
    }

    private Quantity buildQuantity(MedicationLine line) {
        String ucum = StringUtils.hasText(line.ucumCode) ? line.ucumCode.trim() : "U";
        Quantity quantity = new Quantity();
        quantity.setValue(line.qty == null ? BigDecimal.ONE : line.qty);
        quantity.setSystem(UCUM_SYSTEM);
        quantity.setUnit(ucum);
        quantity.setCode(ucum);
        return quantity;
    }

    private void requireKfa(MedicationLine line) {
        if (!StringUtils.hasText(line.kfaCode)) {
            throw new IllegalArgumentException(
                    "Obat \"" + line.itemName + "\" belum dipetakan ke kode KFA (ms_item.v_kfa_code).");
        }
    }

    // ------------------------------------------------------------------
    // Data lokal
    // ------------------------------------------------------------------

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

    private List<MedicationLine> loadMedicationLines(int registrationId) {
        return jdbcTemplate.query(
                "select t.n_item_trx_id, t.n_item_id, t.n_qty, t.aturan_pakai, "
                        + "t.ss_medication_request_id, t.ss_medication_dispense_id, "
                        + "i.v_item_code, i.v_item_name, i.v_kfa_code, i.v_kfa_display, i.v_ucum_code, i.ss_medication_id "
                        + "from tb_item_trx t "
                        + "join tb_examination e on e.n_exam_id = t.n_note_id "
                        + "join ms_item i on i.n_item_id = t.n_item_id "
                        + "join ms_item_group g on g.n_item_group_id = i.n_item_group_id "
                        + "where e.n_reg_id = ? and g.v_item_group_code = 'OBT01' "
                        + "order by t.n_item_trx_id",
                (resultSet, rowNum) -> new MedicationLine(
                        resultSet.getLong("n_item_trx_id"),
                        resultSet.getLong("n_item_id"),
                        resultSet.getBigDecimal("n_qty"),
                        resultSet.getString("aturan_pakai"),
                        resultSet.getString("ss_medication_request_id"),
                        resultSet.getString("ss_medication_dispense_id"),
                        resultSet.getString("v_item_code"),
                        resultSet.getString("v_item_name"),
                        resultSet.getString("v_kfa_code"),
                        resultSet.getString("v_kfa_display"),
                        resultSet.getString("v_ucum_code"),
                        resultSet.getString("ss_medication_id")),
                registrationId);
    }

    private String toFhirDate(Timestamp timestamp) {
        return FHIR_DATE.format((timestamp == null ? new Timestamp(System.currentTimeMillis()) : timestamp)
                .toInstant().atZone(JAKARTA).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS));
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

    private static final class MedicationLine {
        private final long trxId;
        private final long itemId;
        private final BigDecimal qty;
        private final String aturanPakai;
        private final String requestId;
        private final String dispenseId;
        private final String itemCode;
        private final String itemName;
        private final String kfaCode;
        private final String kfaDisplay;
        private final String ucumCode;
        private final String medicationId;

        private MedicationLine(long trxId, long itemId, BigDecimal qty, String aturanPakai, String requestId,
                String dispenseId, String itemCode, String itemName, String kfaCode, String kfaDisplay,
                String ucumCode, String medicationId) {
            this.trxId = trxId;
            this.itemId = itemId;
            this.qty = qty;
            this.aturanPakai = aturanPakai;
            this.requestId = requestId;
            this.dispenseId = dispenseId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.kfaCode = kfaCode;
            this.kfaDisplay = kfaDisplay;
            this.ucumCode = ucumCode;
            this.medicationId = medicationId;
        }
    }
}
