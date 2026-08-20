package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0017 (EARNING REPORT / doctorReport.zul).
 * Laporan pendapatan dokter yang sedang login (tanpa pemilihan dokter).
 */
@RestController
@RequestMapping("/api/report/earning")
public class EarningReportController {

    private final PendapatanDokterService pendapatanDokterService;

    public EarningReportController(PendapatanDokterService pendapatanDokterService) {
        this.pendapatanDokterService = pendapatanDokterService;
    }

    @GetMapping
    public ApiResponse<PendapatanDokterResponse> report(
            @RequestParam String tipe,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String patientType,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = pendapatanDokterService.requireUsername(session);
        Integer staffId = pendapatanDokterService.requireDoctorStaffId(username);
        return ApiResponse.ok(pendapatanDokterService.getEarningReport(staffId, tipe, from, to,
                patientType));
    }
}
