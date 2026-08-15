package com.vone.simrs.accounting.coa;

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
 * REST controller untuk screen SCM0046 (CHART OF ACCOUNT).
 */
@RestController
@RequestMapping("/api/accounting/coa")
public class CoaController {

    private final CoaService coaService;
    private final LegacyAuthService legacyAuthService;

    public CoaController(CoaService coaService, LegacyAuthService legacyAuthService) {
        this.coaService = coaService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<CoaRowResponse>> list(@RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer typeId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(coaService.getCoaTree(status, typeId));
    }

    @GetMapping("/types")
    public ApiResponse<List<CoaTypeOptionResponse>> types(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(coaService.getCoaTypes());
    }

    @GetMapping("/parent-options")
    public ApiResponse<List<CoaRowResponse>> parentOptions(@RequestParam(required = false) Integer typeId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(coaService.getCoaParentOptions(typeId));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody CoaSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        coaService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        coaService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
