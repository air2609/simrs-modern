package com.vone.simrs.master.insurance;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.master.treatment.CoaOptionResponse;
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
 * REST controller untuk screen SCM0034 (INSURANCE MASTER).
 */
@RestController
@RequestMapping("/api/master/insurance")
public class InsuranceController {

    private final InsuranceService insuranceService;
    private final LegacyAuthService legacyAuthService;

    public InsuranceController(InsuranceService insuranceService, LegacyAuthService legacyAuthService) {
        this.insuranceService = insuranceService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<InsuranceRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(insuranceService.getInsurances());
    }

    @GetMapping("/coa-search")
    public ApiResponse<List<CoaOptionResponse>> coaSearch(@RequestParam String keyword, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(insuranceService.searchCoa(keyword));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody InsuranceSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        insuranceService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        insuranceService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
