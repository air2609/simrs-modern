package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
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
 * REST controller untuk screen RPT0005 (LAPORAN PASIEN RAWAT INAP / laporanPasienBangsal.zul).
 */
@RestController
@RequestMapping("/api/report/pasien-bangsal")
public class PasienBangsalController {

    private final PasienBangsalService pasienBangsalService;
    private final PasienBangsalPrintService pasienBangsalPrintService;

    public PasienBangsalController(PasienBangsalService pasienBangsalService,
            PasienBangsalPrintService pasienBangsalPrintService) {
        this.pasienBangsalService = pasienBangsalService;
        this.pasienBangsalPrintService = pasienBangsalPrintService;
    }

    @GetMapping("/masters")
    public ApiResponse<List<WardOptionResponse>> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(pasienBangsalService.getWards());
    }

    @GetMapping("/report")
    public ApiResponse<PasienBangsalResponse> report(
            @RequestParam String ward,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(pasienBangsalService.getReport(ward, from, to));
    }

    @GetMapping("/print")
    public ResponseEntity<byte[]> print(
            @RequestParam String ward,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        PasienBangsalResponse data = pasienBangsalService.getReport(ward, from, to);
        byte[] pdf = pasienBangsalPrintService.generatePdf(data, toDisplayDate(from),
                toDisplayDate(to));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan-pasien-bangsal.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return pasienBangsalService.requireUsername(session);
    }

    private String toDisplayDate(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return "";
        }
        String[] parts = iso.split("-");
        return parts.length == 3 ? parts[2] + "-" + parts[1] + "-" + parts[0] : iso;
    }
}
