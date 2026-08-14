package com.vone.simrs.master.treatment;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0026 (TREATMENT MASTER).
 */
@RestController
@RequestMapping("/api/master/treatment")
public class TreatmentController {

    private final TreatmentService treatmentService;
    private final LegacyAuthService legacyAuthService;

    public TreatmentController(TreatmentService treatmentService, LegacyAuthService legacyAuthService) {
        this.treatmentService = treatmentService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<TreatmentRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(treatmentService.getTreatments());
    }

    @GetMapping("/search")
    public ApiResponse<List<TreatmentRowResponse>> search(@RequestParam String keyword, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(treatmentService.searchTreatments(keyword));
    }

    @GetMapping("/group-options")
    public ApiResponse<List<TreatmentGroupOptionResponse>> groupOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(treatmentService.getTreatmentGroupOptions());
    }

    @GetMapping("/class-options")
    public ApiResponse<List<TreatmentClassOptionResponse>> classOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(treatmentService.getTreatmentClassOptions());
    }

    @GetMapping("/coa-search")
    public ApiResponse<List<CoaOptionResponse>> coaSearch(@RequestParam String keyword, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(treatmentService.searchCoa(keyword));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody TreatmentSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        treatmentService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        treatmentService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
