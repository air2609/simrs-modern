package com.vone.simrs.accounting;

import com.vone.simrs.auth.LegacyAuthService;
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
 * REST controller untuk screen SC0203 (LABA RUGI / labaRugi.zul).
 */
@RestController
@RequestMapping("/api/accounting/laba-rugi")
public class LabarugiController {

    private final LabarugiService labarugiService;
    private final LabarugiPrintPdfService labarugiPrintPdfService;
    private final LegacyAuthService legacyAuthService;

    public LabarugiController(LabarugiService labarugiService,
            LabarugiPrintPdfService labarugiPrintPdfService,
            LegacyAuthService legacyAuthService) {
        this.labarugiService = labarugiService;
        this.labarugiPrintPdfService = labarugiPrintPdfService;
        this.legacyAuthService = legacyAuthService;
    }

    /**
     * Data laba rugi per rentang tanggal. Migrasi dari legacy
     * {@code LabarugiController.cariClick()} (tombol CARI).
     */
    @GetMapping
    public ApiResponse<List<LabarugiGroupResponse>> labarugi(
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(labarugiService.getLabarugi(from, to));
    }

    /**
     * Cetak laba rugi per rentang tanggal (PDF). Migrasi dari legacy
     * {@code LabarugiController.printLabarugi()} (tombol CETAK BY DATE).
     */
    @GetMapping("/print-by-date")
    public ResponseEntity<byte[]> printByDate(
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        LabarugiPrintData data = labarugiPrintPdfService.loadPrintData(from, to);
        byte[] pdf = labarugiPrintPdfService.generateLabarugiPdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laba-rugi.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Cetak laba rugi seluruh periode (PDF). Migrasi dari legacy
     * {@code AccountingReport.openCurrentLabarugi()} yang memakai
     * {@code report.v_profit_loss} (tombol CETAK).
     */
    @GetMapping("/print")
    public ResponseEntity<byte[]> print(HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        LabarugiPrintData data = labarugiPrintPdfService.loadPrintAllData();
        byte[] pdf = labarugiPrintPdfService.generateLabarugiPdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laba-rugi-all.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
