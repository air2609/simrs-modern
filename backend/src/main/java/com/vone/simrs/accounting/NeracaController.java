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
 * REST controller untuk screen SC0202 (NERACA / neraca.zul).
 */
@RestController
@RequestMapping("/api/accounting/neraca")
public class NeracaController {

    private final NeracaService neracaService;
    private final NeracaPrintPdfService neracaPrintPdfService;
    private final LegacyAuthService legacyAuthService;

    public NeracaController(NeracaService neracaService,
            NeracaPrintPdfService neracaPrintPdfService,
            LegacyAuthService legacyAuthService) {
        this.neracaService = neracaService;
        this.neracaPrintPdfService = neracaPrintPdfService;
        this.legacyAuthService = legacyAuthService;
    }

    /**
     * Data neraca per periode. Migrasi dari legacy
     * {@code NeracaController.openNeraca()} (tombol LIHAT NERACA).
     */
    @GetMapping
    public ApiResponse<List<NeracaGroupResponse>> neraca(
            @RequestParam String date, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(neracaService.getNeraca(date));
    }

    /**
     * Cetak neraca (PDF). Migrasi dari legacy
     * {@code AccountingReport.openCurrentNeraca()} yang memakai report
     * {@code jasper/balance_sheet.jrxml} + {@code report.balance_sheet()}.
     */
    @GetMapping("/print")
    public ResponseEntity<byte[]> print(HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        byte[] pdf = neracaPrintPdfService.generateNeracaPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=neraca.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
