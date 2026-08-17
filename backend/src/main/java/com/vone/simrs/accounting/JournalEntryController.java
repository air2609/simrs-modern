package com.vone.simrs.accounting;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0199 (MANUAL JOURNAL ENTRY / journalEntry.zul).
 */
@RestController
@RequestMapping("/api/accounting/journal-entry")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;
    private final JournalEntryPrintPdfService journalEntryPrintPdfService;
    private final LegacyAuthService legacyAuthService;

    public JournalEntryController(JournalEntryService journalEntryService,
            JournalEntryPrintPdfService journalEntryPrintPdfService,
            LegacyAuthService legacyAuthService) {
        this.journalEntryService = journalEntryService;
        this.journalEntryPrintPdfService = journalEntryPrintPdfService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/masters")
    public ApiResponse<JournalEntryMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(journalEntryService.getMasters());
    }

    @PostMapping
    public ApiResponse<String> save(@RequestBody JournalEntrySaveRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(journalEntryService.save(body, username));
    }

    @GetMapping("/print")
    public ResponseEntity<byte[]> print(@RequestParam String voucherNo, HttpServletRequest request)
            throws Exception {
        ensureAuthenticated(request.getSession(false));
        JournalEntryPrintData data = journalEntryService.getPrintData(voucherNo);
        byte[] pdf = journalEntryPrintPdfService.generateManualJournalPdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=manual-journal.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
