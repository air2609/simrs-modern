package com.vone.simrs.master.treatmentbatch;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0056 (UPDATE MASTER TINDAKAN / batch).
 */
@RestController
@RequestMapping("/api/master/treatment-batch")
public class BatchTreatmentController {

    private final BatchTreatmentService batchTreatmentService;
    private final LegacyAuthService legacyAuthService;

    public BatchTreatmentController(BatchTreatmentService batchTreatmentService,
            LegacyAuthService legacyAuthService) {
        this.batchTreatmentService = batchTreatmentService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<BatchTreatmentRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(batchTreatmentService.getTreatments());
    }

    @PostMapping("/save")
    public ApiResponse<BatchTreatmentService.BatchTreatmentSaveResult> save(
            @RequestBody BatchTreatmentSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(batchTreatmentService.save(body, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
