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
 * REST controller untuk screen RPT0010 (LAPORAN PENDAFTARAN / laporanPendaftaran.zul).
 */
@RestController
@RequestMapping("/api/report/laporan-pendaftaran")
public class LaporanPendaftaranController {

    private final LaporanPendaftaranService laporanPendaftaranService;
    private final LaporanPendaftaranPrintService laporanPendaftaranPrintService;

    public LaporanPendaftaranController(LaporanPendaftaranService laporanPendaftaranService,
            LaporanPendaftaranPrintService laporanPendaftaranPrintService) {
        this.laporanPendaftaranService = laporanPendaftaranService;
        this.laporanPendaftaranPrintService = laporanPendaftaranPrintService;
    }

    @GetMapping("/masters")
    public ApiResponse<List<WardUnitResponse>> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanPendaftaranService.getUnits());
    }

    @GetMapping("/report")
    public ApiResponse<LaporanPendaftaranResponse> report(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer unitId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanPendaftaranService.getReport(from, to, unitId));
    }

    @GetMapping("/print")
    public ResponseEntity<byte[]> print(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer unitId,
            HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        LaporanPendaftaranResponse data = laporanPendaftaranService.getReport(from, to, unitId);
        byte[] pdf = laporanPendaftaranPrintService.generatePdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan-pendaftaran.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return laporanPendaftaranService.requireUsername(session);
    }
}
