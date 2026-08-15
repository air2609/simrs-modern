package com.vone.simrs.master.doctor;

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
 * REST controller untuk screen SCM0030 (MASTER DOKTER).
 */
@RestController
@RequestMapping("/api/master/doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final LegacyAuthService legacyAuthService;

    public DoctorController(DoctorService doctorService, LegacyAuthService legacyAuthService) {
        this.doctorService = doctorService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<DoctorRowResponse>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(doctorService.getDoctors(code, name));
    }

    @GetMapping("/masters")
    public ApiResponse<DoctorMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(doctorService.getMasters());
    }

    @GetMapping("/unit-options")
    public ApiResponse<List<UnitOptionResponse>> unitOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(doctorService.getUnitOptions());
    }

    @GetMapping("/coa-search")
    public ApiResponse<List<CoaOptionResponse>> searchCoa(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(doctorService.searchCoa(keyword));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody DoctorSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        doctorService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer staffId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        doctorService.delete(staffId);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
