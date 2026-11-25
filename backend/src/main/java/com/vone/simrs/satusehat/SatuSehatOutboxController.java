package com.vone.simrs.satusehat;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint outbox/retry SATUSEHAT.
 */
@RestController
@RequestMapping("/api/satusehat/outbox")
public class SatuSehatOutboxController {

    private final SatuSehatOutboxService outboxService;

    @Value("${satusehat.outbox.batch-size:20}")
    private int batchSize;

    public SatuSehatOutboxController(SatuSehatOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @GetMapping
    public ApiResponse<List<SatuSehatOutboxJob>> list(HttpServletRequest request) {
        outboxService.requireUsername(request.getSession(false));
        return ApiResponse.ok(outboxService.listJobs());
    }

    /** Proses job yang menunggu sekarang juga. */
    @PostMapping("/process")
    public ApiResponse<SatuSehatOutboxProcessResult> process(HttpServletRequest request) {
        outboxService.requireUsername(request.getSession(false));
        return ApiResponse.ok(outboxService.processPending(batchSize));
    }

    /** Tambahkan job sinkronisasi tertentu. */
    @PostMapping("/enqueue")
    public ApiResponse<String> enqueue(
            @RequestParam String jobType,
            @RequestParam String localRef,
            HttpServletRequest request) {
        String username = outboxService.requireUsername(request.getSession(false));
        outboxService.enqueue(jobType, localRef, username);
        return ApiResponse.ok("Job ditambahkan ke antrian");
    }

    /** Tambahkan seluruh job sinkronisasi untuk satu registrasi. */
    @PostMapping("/enqueue-registration")
    public ApiResponse<String> enqueueRegistration(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        String username = outboxService.requireUsername(request.getSession(false));
        for (String jobType : new String[] {
                SatuSehatOutboxService.JOB_ENCOUNTER,
                SatuSehatOutboxService.JOB_CONDITION,
                SatuSehatOutboxService.JOB_PROCEDURE,
                SatuSehatOutboxService.JOB_OBSERVATION,
                SatuSehatOutboxService.JOB_TTV,
                SatuSehatOutboxService.JOB_MEDICATION_REQUEST,
                SatuSehatOutboxService.JOB_MEDICATION_DISPENSE }) {
            outboxService.enqueue(jobType, registrationCode, username);
        }
        return ApiResponse.ok("Job sinkronisasi registrasi ditambahkan ke antrian");
    }
}
