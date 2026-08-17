package com.vone.simrs.accounting;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0207 (TRIAL BALANCE / trialBalance.zul).
 */
@RestController
@RequestMapping("/api/accounting/trial-balance")
public class TrialBalanceController {

    private final TrialBalanceService trialBalanceService;
    private final LegacyAuthService legacyAuthService;

    public TrialBalanceController(TrialBalanceService trialBalanceService,
            LegacyAuthService legacyAuthService) {
        this.trialBalanceService = trialBalanceService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<TrialBalanceRowResponse>> trialBalance(
            @RequestParam String date, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(trialBalanceService.getTrialBalance(date));
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
