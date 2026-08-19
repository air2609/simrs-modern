package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0015 (LAPORAN RAWAT INAP/JALAN / laporanRawatInapJalan.zul).
 */
@RestController
@RequestMapping("/api/report/rawat-inap-jalan")
public class RawatInapJalanController {

    private final RawatInapJalanService rawatInapJalanService;

    public RawatInapJalanController(RawatInapJalanService rawatInapJalanService) {
        this.rawatInapJalanService = rawatInapJalanService;
    }

    @GetMapping("/report")
    public ApiResponse<RawatInapJalanResponse> report(
            @RequestParam String tipe,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(rawatInapJalanService.getReport(tipe, from, to));
    }

    private String ensureAuthenticated(HttpSession session) {
        return rawatInapJalanService.requireUsername(session);
    }
}
