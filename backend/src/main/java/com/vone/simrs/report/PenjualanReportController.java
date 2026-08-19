package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.ward.WardUnitResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0001 (LAPORAN PENJUALAN PASIEN / laporanRajal.zul).
 */
@RestController
@RequestMapping("/api/report/penjualan")
public class PenjualanReportController {

    private final PenjualanReportService penjualanReportService;
    private final PenjualanReportPrintService penjualanReportPrintService;

    public PenjualanReportController(PenjualanReportService penjualanReportService,
            PenjualanReportPrintService penjualanReportPrintService) {
        this.penjualanReportService = penjualanReportService;
        this.penjualanReportPrintService = penjualanReportPrintService;
    }

    @GetMapping("/masters")
    public ApiResponse<List<WardUnitResponse>> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(penjualanReportService.getUnits(username));
    }

    @GetMapping("/report")
    public ApiResponse<PenjualanReportResponse> report(
            @RequestParam String tipe,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam Integer unitId,
            @RequestParam(required = false) String shift,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(penjualanReportService.getReport(tipe, from, to, unitId, shift));
    }

    @GetMapping("/print")
    public ResponseEntity<byte[]> print(
            @RequestParam String tipe,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam Integer unitId,
            @RequestParam(required = false) String shift,
            HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        PenjualanReportResponse data = penjualanReportService.getReport(tipe, from, to, unitId, shift);
        byte[] pdf = penjualanReportPrintService.generatePdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan-penjualan.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return penjualanReportService.requireUsername(session);
    }
}
