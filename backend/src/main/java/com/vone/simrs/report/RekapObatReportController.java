package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0016 (REKAP OBAT / rekapObat.zul).
 */
@RestController
@RequestMapping("/api/report/rekap-obat")
public class RekapObatReportController {

    private final RekapObatReportService rekapObatReportService;

    public RekapObatReportController(RekapObatReportService rekapObatReportService) {
        this.rekapObatReportService = rekapObatReportService;
    }

    @GetMapping("/report")
    public ApiResponse<RekapObatReportResponse> report(
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(rekapObatReportService.getReport(from, to));
    }

    private String ensureAuthenticated(HttpSession session) {
        return rekapObatReportService.requireUsername(session);
    }
}
