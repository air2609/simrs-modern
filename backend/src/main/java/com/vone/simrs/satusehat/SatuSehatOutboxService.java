package com.vone.simrs.satusehat;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Outbox / antrian retry untuk sinkronisasi SATUSEHAT.
 *
 * <p>Job disimpan di {@code ss_outbox}. Pemrosesan dilakukan oleh scheduler
 * (lihat {@link SatuSehatOutboxScheduler}) atau manual lewat endpoint.
 * Kegagalan akan dicoba ulang dengan backoff (exponential, dibatasi).
 */
@Service
public class SatuSehatOutboxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SatuSehatOutboxService.class);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Jenis job yang didukung.
    public static final String JOB_PATIENT = "PATIENT";
    public static final String JOB_ENCOUNTER = "ENCOUNTER";
    public static final String JOB_ENCOUNTER_FINISH = "ENCOUNTER_FINISH";
    public static final String JOB_CONDITION = "CONDITION";
    public static final String JOB_PROCEDURE = "PROCEDURE";
    public static final String JOB_OBSERVATION = "OBSERVATION";
    public static final String JOB_TTV = "TTV";
    public static final String JOB_MEDICATION_REQUEST = "MEDICATION_REQUEST";
    public static final String JOB_MEDICATION_DISPENSE = "MEDICATION_DISPENSE";

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final SatuSehatPatientService patientService;
    private final SatuSehatEncounterService encounterService;
    private final SatuSehatClinicalService clinicalService;
    private final SatuSehatObservationService observationService;
    private final SatuSehatMedicationService medicationService;

    @Value("${satusehat.outbox.max-attempts:10}")
    private int maxAttempts;

    public SatuSehatOutboxService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService,
            SatuSehatPatientService patientService, SatuSehatEncounterService encounterService,
            SatuSehatClinicalService clinicalService, SatuSehatObservationService observationService,
            SatuSehatMedicationService medicationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.patientService = patientService;
        this.encounterService = encounterService;
        this.clinicalService = clinicalService;
        this.observationService = observationService;
        this.medicationService = medicationService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Tambahkan/perbarui job sinkronisasi (idempotent per jenis + ref). */
    public void enqueue(String jobType, String localRef, String username) {
        if (!StringUtils.hasText(jobType) || !StringUtils.hasText(localRef)) {
            throw new IllegalArgumentException("Job type dan local ref wajib diisi.");
        }
        jdbcTemplate.update(
                "insert into ss_outbox (v_job_type, v_local_ref, v_status, v_who_create, d_whn_create) "
                        + "values (?, ?, 'PENDING', ?, now()) "
                        + "on conflict (v_job_type, v_local_ref) do update "
                        + "set v_status = 'PENDING', d_next_attempt = now(), v_last_error = null, "
                        + "d_whn_update = now()",
                jobType.trim().toUpperCase(), localRef.trim(), username);
    }

    /** Tambahkan job berdasarkan {@code n_reg_id} (kode registrasi di-resolve otomatis). */
    public void enqueueByRegId(String jobType, Integer regId, String username) {
        if (regId == null) {
            return;
        }
        List<String> codes = jdbcTemplate.queryForList(
                "select v_reg_secondary_id from tb_registration where n_reg_id = ?", String.class, regId);
        if (!codes.isEmpty() && StringUtils.hasText(codes.get(0))) {
            enqueue(jobType, codes.get(0), username);
        }
    }

    public List<SatuSehatOutboxJob> listJobs() {
        return jdbcTemplate.query(
                "select n_outbox_id, v_job_type, v_local_ref, v_status, n_attempts, v_last_error, "
                        + "d_next_attempt, d_whn_update from ss_outbox order by n_outbox_id desc limit 200",
                (resultSet, rowNum) -> new SatuSehatOutboxJob(
                        resultSet.getLong("n_outbox_id"),
                        resultSet.getString("v_job_type"),
                        resultSet.getString("v_local_ref"),
                        resultSet.getString("v_status"),
                        resultSet.getInt("n_attempts"),
                        resultSet.getString("v_last_error"),
                        format(resultSet.getTimestamp("d_next_attempt")),
                        format(resultSet.getTimestamp("d_whn_update"))));
    }

    /** Proses job yang siap dikirim (status PENDING/FAILED dan sudah waktunya). */
    public SatuSehatOutboxProcessResult processPending(int max) {
        List<PendingJob> jobs = jdbcTemplate.query(
                "select n_outbox_id, v_job_type, v_local_ref, n_attempts from ss_outbox "
                        + "where v_status in ('PENDING', 'FAILED') and d_next_attempt <= now() "
                        + "order by n_outbox_id limit ?",
                (resultSet, rowNum) -> new PendingJob(
                        resultSet.getLong("n_outbox_id"),
                        resultSet.getString("v_job_type"),
                        resultSet.getString("v_local_ref"),
                        resultSet.getInt("n_attempts")),
                max);

        int sent = 0;
        int failed = 0;
        for (PendingJob job : jobs) {
            int claimed = jdbcTemplate.update(
                    "update ss_outbox set v_status = 'PROCESSING', d_whn_update = now() "
                            + "where n_outbox_id = ? and v_status in ('PENDING', 'FAILED')",
                    job.id);
            if (claimed == 0) {
                continue;
            }
            try {
                dispatch(job.jobType, job.localRef);
                markSent(job.id);
                sent++;
            } catch (Exception exception) {
                LOGGER.warn("Outbox job {} ({}/{}) gagal: {}", job.id, job.jobType, job.localRef,
                        exception.getMessage());
                markFailed(job.id, job.attempts + 1, exception.getMessage());
                failed++;
            }
        }
        return new SatuSehatOutboxProcessResult(jobs.size(), sent, failed);
    }

    private void dispatch(String jobType, String localRef) {
        switch (jobType) {
            case JOB_PATIENT:
                patientService.syncPatient(localRef);
                return;
            case JOB_ENCOUNTER:
                encounterService.syncEncounter(localRef);
                return;
            case JOB_ENCOUNTER_FINISH:
                encounterService.finishEncounter(localRef);
                return;
            case JOB_CONDITION:
                clinicalService.syncConditions(localRef);
                return;
            case JOB_PROCEDURE:
                clinicalService.syncProcedures(localRef);
                return;
            case JOB_OBSERVATION:
                observationService.syncObservations(localRef);
                return;
            case JOB_TTV:
                observationService.syncVitalSigns(localRef);
                return;
            case JOB_MEDICATION_REQUEST:
                medicationService.syncMedicationRequests(localRef);
                return;
            case JOB_MEDICATION_DISPENSE:
                medicationService.syncMedicationDispenses(localRef);
                return;
            default:
                throw new IllegalArgumentException("Job type tidak dikenal: " + jobType);
        }
    }

    private void markSent(long id) {
        jdbcTemplate.update(
                "update ss_outbox set v_status = 'SENT', n_attempts = n_attempts + 1, "
                        + "v_last_error = null, d_whn_update = now() where n_outbox_id = ?",
                id);
    }

    private void markFailed(long id, int attempts, String error) {
        String message = error == null ? "unknown" : (error.length() > 2000 ? error.substring(0, 2000) : error);
        if (attempts >= maxAttempts) {
            jdbcTemplate.update(
                    "update ss_outbox set v_status = 'DEAD', n_attempts = ?, v_last_error = ?, "
                            + "d_whn_update = now() where n_outbox_id = ?",
                    attempts, message, id);
            return;
        }
        long minutes = Math.min(1L << Math.min(attempts, 6), 60L);
        jdbcTemplate.update(
                "update ss_outbox set v_status = 'FAILED', n_attempts = ?, v_last_error = ?, "
                        + "d_next_attempt = now() + (? || ' minutes')::interval, d_whn_update = now() "
                        + "where n_outbox_id = ?",
                attempts, message, String.valueOf(minutes), id);
    }

    private String format(Timestamp timestamp) {
        return timestamp == null ? null : DISPLAY.format(timestamp.toLocalDateTime());
    }

    private static final class PendingJob {
        private final long id;
        private final String jobType;
        private final String localRef;
        private final int attempts;

        private PendingJob(long id, String jobType, String localRef, int attempts) {
            this.id = id;
            this.jobType = jobType;
            this.localRef = localRef;
            this.attempts = attempts;
        }
    }
}
