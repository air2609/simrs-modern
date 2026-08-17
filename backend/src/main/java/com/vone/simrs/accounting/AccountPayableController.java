package com.vone.simrs.accounting;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0196 (ACCOUNT PAYABLE / apScreen.zul).
 */
@RestController
@RequestMapping("/api/accounting/account-payable")
public class AccountPayableController {

    private final AccountPayableService accountPayableService;
    private final LegacyAuthService legacyAuthService;

    public AccountPayableController(AccountPayableService accountPayableService,
            LegacyAuthService legacyAuthService) {
        this.accountPayableService = accountPayableService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<AccountPayablePageResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(accountPayableService.getAllAp(keyword, page, pageSize));
    }

    @GetMapping("/masters")
    public ApiResponse<AccountPayableMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(accountPayableService.getMasters());
    }

    @GetMapping("/journal")
    public ApiResponse<List<AccountPayableJournalResponse>> journal(
            @RequestParam String batchId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(accountPayableService.getJournalByBatch(batchId));
    }

    @GetMapping("/history")
    public ApiResponse<List<AccountPayableJournalResponse>> history(
            @RequestParam Integer apId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(accountPayableService.getPaymentHistory(apId));
    }

    @PostMapping("/pay")
    public ApiResponse<String> pay(@RequestBody AccountPayablePayRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(accountPayableService.pay(body, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
