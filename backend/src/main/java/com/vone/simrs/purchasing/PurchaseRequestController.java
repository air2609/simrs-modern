package com.vone.simrs.purchasing;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0191 (ORDER PERMINTAAN PEMBELIAN).
 */
@RestController
@RequestMapping("/api/purchasing/purchase-request")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final LegacyAuthService legacyAuthService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService,
            LegacyAuthService legacyAuthService) {
        this.purchaseRequestService = purchaseRequestService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/masters")
    public ApiResponse<PurchaseRequestMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestService.getMasters(username));
    }

    @GetMapping("/items")
    public ApiResponse<List<PurchaseRequestItemResponse>> items(
            @RequestParam Integer warehouseId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestService.getItems(warehouseId));
    }

    @GetMapping("/add-items")
    public ApiResponse<List<PurchaseRequestAddItemResponse>> addItems(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestService.searchAddItems(code, name));
    }

    @GetMapping("/opp/search")
    public ApiResponse<List<PurchaseRequestOppOptionResponse>> searchOpp(
            @RequestParam(required = false) String prCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestService.searchOpp(prCode));
    }

    @GetMapping("/opp/detail")
    public ApiResponse<PurchaseRequestOppDetailResponse> oppDetail(
            @RequestParam String prCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestService.getOppDetail(prCode));
    }

    @GetMapping("/suppliers/search")
    public ApiResponse<List<PurchaseRequestSupplierResponse>> searchSuppliers(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestService.searchSuppliers(code, name));
    }

    @PostMapping
    public ApiResponse<PurchaseRequestSaveResultResponse> save(
            @RequestBody PurchaseRequestSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseRequestService.save(body, username));
    }

    @PostMapping("/update")
    public ApiResponse<Void> update(@RequestBody PurchaseRequestUpdateRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        purchaseRequestService.update(body, username);
        return ApiResponse.ok(null);
    }

    @PostMapping("/revoke")
    public ApiResponse<Void> revoke(@RequestParam String prCode, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        purchaseRequestService.revoke(prCode, username);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}