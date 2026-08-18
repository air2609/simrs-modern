package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0022 (LAPORAN REKAP KASIR / rekapAllKasir.zul).
 */
@RestController
@RequestMapping("/api/report/rekap-kasir")
public class RekapKasirController {

    private final RekapKasirService rekapKasirService;

    public RekapKasirController(RekapKasirService rekapKasirService) {
        this.rekapKasirService = rekapKasirService;
    }

    @GetMapping("/report")
    public ApiResponse<RekapKasirResponse> report(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String laporanType,
            @RequestParam(required = false) String pasienType,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(rekapKasirService.getReport(from, to, laporanType, pasienType));
    }

    private String ensureAuthenticated(HttpSession session) {
        return rekapKasirService.requireUsername(session);
    }
}
