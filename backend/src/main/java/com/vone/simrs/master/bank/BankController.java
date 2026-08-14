package com.vone.simrs.master.bank;

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
 * REST controller untuk screen SCM0033 (BANK MASTER).
 */
@RestController
@RequestMapping("/api/master/bank")
public class BankController {

    private final BankService bankService;
    private final LegacyAuthService legacyAuthService;

    public BankController(BankService bankService, LegacyAuthService legacyAuthService) {
        this.bankService = bankService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<BankRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bankService.getBanks());
    }

    @GetMapping("/coa-search")
    public ApiResponse<List<CoaOptionResponse>> coaSearch(@RequestParam String keyword, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bankService.searchCoa(keyword));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody BankSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        bankService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        bankService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
