package com.vone.simrs.antrian.polidokter;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
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
 * REST controller untuk screen SCM0059 (POLI DOKTER).
 */
@RestController
@RequestMapping("/api/antrian/poli-dokter")
public class PolyDoctorController {

    private final PolyDoctorService polyDoctorService;
    private final LegacyAuthService legacyAuthService;

    public PolyDoctorController(PolyDoctorService polyDoctorService, LegacyAuthService legacyAuthService) {
        this.polyDoctorService = polyDoctorService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<PolyDoctorRowResponse>> list(
            @RequestParam(required = false) String search,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyDoctorService.getPolyDoctors(search));
    }

    @GetMapping("/masters")
    public ApiResponse<PolyDoctorMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyDoctorService.getMasters());
    }

    @GetMapping("/doctors")
    public ApiResponse<List<DoctorOptionResponse>> searchDoctors(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyDoctorService.searchDoctors(code, name));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody PolyDoctorSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        polyDoctorService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        polyDoctorService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/schedules")
    public ApiResponse<List<DoctorScheduleResponse>> schedules(
            @RequestParam Integer doctorId,
            @RequestParam String month,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyDoctorService.getSchedules(doctorId, month));
    }

    @PostMapping("/schedules/save")
    public ApiResponse<Void> saveSchedules(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        Integer doctorId = (Integer) body.get("doctorId");
        @SuppressWarnings("unchecked")
        List<String> dates = (List<String>) body.get("dates");
        polyDoctorService.saveSchedules(doctorId, dates, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/schedules/delete")
    public ApiResponse<Void> deleteSchedule(
            @RequestParam Integer doctorId,
            @RequestParam String date,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        polyDoctorService.deleteSchedule(doctorId, date);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
