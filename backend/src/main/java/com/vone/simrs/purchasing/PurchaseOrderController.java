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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0193 (ORDER PEMBELIAN / purchaseOrder.zul).
 */
@RestController
@RequestMapping("/api/purchasing/purchase-order")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final LegacyAuthService legacyAuthService;
    private final PurchaseOrderPrintPdfService purchaseOrderPrintPdfService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService,
            LegacyAuthService legacyAuthService,
            PurchaseOrderPrintPdfService purchaseOrderPrintPdfService) {
        this.purchaseOrderService = purchaseOrderService;
        this.legacyAuthService = legacyAuthService;
        this.purchaseOrderPrintPdfService = purchaseOrderPrintPdfService;
    }

    @GetMapping("/masters")
    public ApiResponse<PurchaseOrderMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderService.getMasters(username));
    }

    @GetMapping("/opp/search")
    public ApiResponse<List<PurchaseRequestOppOptionResponse>> searchOpp(
            @RequestParam(required = false) String prCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderService.searchOpp(prCode));
    }

    @GetMapping("/opp/detail")
    public ApiResponse<PurchaseOrderOppDetailResponse> oppDetail(
            @RequestParam String prCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderService.getOppDetail(prCode));
    }

    @GetMapping("/po/search")
    public ApiResponse<List<PurchaseOrderPoOptionResponse>> searchPo(
            @RequestParam(required = false) String poCode,
            @RequestParam(required = false) String supName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderService.searchPo(poCode, supName));
    }

    @GetMapping("/po/detail")
    public ApiResponse<PurchaseOrderPoDetailResponse> poDetail(
            @RequestParam String poCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderService.getPoDetail(poCode));
    }

    @GetMapping("/suppliers/search")
    public ApiResponse<List<PurchaseRequestSupplierResponse>> searchSuppliers(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderService.searchSuppliers(code, name));
    }

    @PostMapping
    public ApiResponse<PurchaseOrderSaveResultResponse> save(
            @RequestBody PurchaseOrderSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderService.save(body, username));
    }

    @PostMapping("/update")
    public ApiResponse<Void> update(@RequestBody PurchaseOrderUpdateRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        purchaseOrderService.update(body, username);
        return ApiResponse.ok(null);
    }

    @PostMapping("/revoke")
    public ApiResponse<Void> revoke(@RequestParam String poCode, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        purchaseOrderService.revoke(poCode, username);
        return ApiResponse.ok(null);
    }

    @PostMapping("/close-opp")
    public ApiResponse<Void> closeOpp(@RequestParam String prCode, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        purchaseOrderService.closeOpp(prCode, username);
        return ApiResponse.ok(null);
    }

    /**
     * Cetak PDF PURCHASE ORDER. Migrasi dari legacy
     * {@code POController.cetakPO()} (report orderPembelian.jrxml).
     */
    @GetMapping("/print")
    public ResponseEntity<byte[]> print(@RequestParam String poCode, HttpServletRequest request)
            throws Exception {
        String username = ensureAuthenticated(request.getSession(false));
        PurchaseOrderPrintData data = purchaseOrderService.getPrintData(poCode, username);
        byte[] pdf = purchaseOrderPrintPdfService.generatePoPdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=purchase-order.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
