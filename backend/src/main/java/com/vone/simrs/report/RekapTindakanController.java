package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0011 (LAPORAN REKAP TINDAKAN / rekapTindakan.zul).
 */
@RestController
@RequestMapping("/api/report/rekap-tindakan")
public class RekapTindakanController {

    private final RekapTindakanService rekapTindakanService;

    public RekapTindakanController(RekapTindakanService rekapTindakanService) {
        this.rekapTindakanService = rekapTindakanService;
    }

    @GetMapping("/report")
    public ApiResponse<RekapTindakanResponse> report(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String tipePasien,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(rekapTindakanService.getReport(from, to, tipePasien));
    }

    private String ensureAuthenticated(HttpSession session) {
        return rekapTindakanService.requireUsername(session);
    }
}
