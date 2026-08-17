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
 * REST controller untuk screen SC0198 (GENERAL LEDGER / generalLedger.zul).
 */
@RestController
@RequestMapping("/api/accounting/general-ledger")
public class GeneralLedgerController {

    private final GeneralLedgerService generalLedgerService;
    private final GeneralLedgerPrintPdfService generalLedgerPrintPdfService;
    private final LegacyAuthService legacyAuthService;

    public GeneralLedgerController(GeneralLedgerService generalLedgerService,
            GeneralLedgerPrintPdfService generalLedgerPrintPdfService,
            LegacyAuthService legacyAuthService) {
        this.generalLedgerService = generalLedgerService;
        this.generalLedgerPrintPdfService = generalLedgerPrintPdfService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/report")
    public ApiResponse<List<GeneralLedgerRowResponse>> report(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer coaId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(generalLedgerService.getReport(from, to, coaId));
    }

    @GetMapping("/print")
    public ResponseEntity<byte[]> print(
            @RequestParam(required = false) Integer coaId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        GeneralLedgerPrintData data = generalLedgerService.getPrintData(coaId, from, to);
        byte[] pdf = generalLedgerPrintPdfService.generateGeneralLedgerPdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=general-ledger.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/print-all")
    public ResponseEntity<byte[]> printAll(HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        GeneralLedgerPrintData data = generalLedgerService.getPrintAllData();
        byte[] pdf = generalLedgerPrintPdfService.generateGeneralLedgerPdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=general-ledger-all.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
