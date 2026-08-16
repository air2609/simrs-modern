package com.vone.simrs.master.staff;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.master.doctor.CoaOptionResponse;
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
 * REST controller untuk screen SCM0031 (MASTER STAFF).
 */
@RestController
@RequestMapping("/api/master/staff")
public class StaffController {

    private final StaffService staffService;
    private final LegacyAuthService legacyAuthService;

    public StaffController(StaffService staffService, LegacyAuthService legacyAuthService) {
        this.staffService = staffService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<StaffRowResponse>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(staffService.getStaffs(code, name));
    }

    @GetMapping("/masters")
    public ApiResponse<StaffMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(staffService.getMasters());
    }

    @GetMapping("/coa-search")
    public ApiResponse<List<CoaOptionResponse>> searchCoa(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(staffService.searchCoa(keyword));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody StaffSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        staffService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer staffId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        staffService.delete(staffId);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
