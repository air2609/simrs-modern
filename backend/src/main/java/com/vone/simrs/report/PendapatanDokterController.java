package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0013 (LAPORAN PENDAPATAN DOKTER / laporanPendapatanDokter.zul).
 */
@RestController
@RequestMapping("/api/report/pendapatan-dokter")
public class PendapatanDokterController {

    private final PendapatanDokterService pendapatanDokterService;

    public PendapatanDokterController(PendapatanDokterService pendapatanDokterService) {
        this.pendapatanDokterService = pendapatanDokterService;
    }

    @GetMapping("/doctors")
    public ApiResponse<List<DoctorOptionResponse>> doctors(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(pendapatanDokterService.searchDoctors(code, name));
    }

    @GetMapping("/report")
    public ApiResponse<PendapatanDokterResponse> report(
            @RequestParam String tipe,
            @RequestParam(required = false) Integer staffId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String patientType,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(
                pendapatanDokterService.getReport(tipe, staffId, from, to, patientType));
    }

    private String ensureAuthenticated(HttpSession session) {
        return pendapatanDokterService.requireUsername(session);
    }
}
