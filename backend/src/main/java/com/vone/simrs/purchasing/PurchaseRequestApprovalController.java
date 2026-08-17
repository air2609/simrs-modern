package com.vone.simrs.purchasing;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0192 (FORM PERSETUJUAN ORDER PERMINTAAN
 * PEMBELIAN / prApproval.zul).
 */
@RestController
@RequestMapping("/api/purchasing/purchase-request-approval")
public class PurchaseRequestApprovalController {

    private final PurchaseRequestApprovalService purchaseRequestApprovalService;
    private final LegacyAuthService legacyAuthService;
    private final OppPrintPdfService oppPrintPdfService;

    public PurchaseRequestApprovalController(
            PurchaseRequestApprovalService purchaseRequestApprovalService,
            LegacyAuthService legacyAuthService,
            OppPrintPdfService oppPrintPdfService) {
        this.purchaseRequestApprovalService = purchaseRequestApprovalService;
        this.legacyAuthService = legacyAuthService;
        this.oppPrintPdfService = oppPrintPdfService;
    }

    @GetMapping("/opp/search")
    public ApiResponse<List<PurchaseRequestOppOptionResponse>> searchOpp(
            @RequestParam(required = false) String prCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestApprovalService.searchOpp(prCode));
    }

    @GetMapping("/opp/detail")
    public ApiResponse<PurchaseRequestApprovalDetailResponse> oppDetail(
            @RequestParam String prCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestApprovalService.getApprovalDetail(prCode));
    }

    @PostMapping("/approve")
    public ApiResponse<PurchaseRequestApprovalResultResponse> approve(
            @RequestParam String prCode, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestApprovalService.approve(prCode, username));
    }

    /**
     * Cetak PDF "SURAT PEMESANAN" (OPP). Migrasi dari legacy
     * {@code PORApproval.print()} + {@code createOppPdf()}.
     */
    @GetMapping("/print")
    public ResponseEntity<byte[]> print(@RequestParam String prCode, HttpServletRequest request)
            throws Exception {
        ensureAuthenticated(request.getSession(false));
        PurchaseRequestApprovalDetailResponse detail = purchaseRequestApprovalService.getApprovalDetail(prCode);
        byte[] pdf = oppPrintPdfService.generateOppPdf(detail);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=opp.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
