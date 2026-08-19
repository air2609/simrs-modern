package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0012 (LAPORAN REKAP PENERIMAAN KASIR / laporanKasir.zul).
 */
@RestController
@RequestMapping("/api/report/penerimaan-kasir")
public class PenerimaanKasirController {

    private final PenerimaanKasirService penerimaanKasirService;

    public PenerimaanKasirController(PenerimaanKasirService penerimaanKasirService) {
        this.penerimaanKasirService = penerimaanKasirService;
    }

    @GetMapping("/report")
    public ApiResponse<PenerimaanKasirResponse> report(
            @RequestParam String date,
            @RequestParam(required = false) String shift,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(penerimaanKasirService.getReport(date, shift));
    }

    private String ensureAuthenticated(HttpSession session) {
        return penerimaanKasirService.requireUsername(session);
    }
}
