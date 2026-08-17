package com.vone.simrs.accounting;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0050 (FORM ACCT DEFAULT /
 * acctDefaultDataInput.zul).
 */
@RestController
@RequestMapping("/api/accounting/acct-default")
public class AcctDefaultController {

    private final AcctDefaultService acctDefaultService;
    private final LegacyAuthService legacyAuthService;

    public AcctDefaultController(AcctDefaultService acctDefaultService,
            LegacyAuthService legacyAuthService) {
        this.acctDefaultService = acctDefaultService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/masters")
    public ApiResponse<AcctDefaultMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(acctDefaultService.getMasters());
    }

    @PostMapping
    public ApiResponse<Void> save(@RequestBody AcctDefaultSaveRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        acctDefaultService.save(body, username);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
