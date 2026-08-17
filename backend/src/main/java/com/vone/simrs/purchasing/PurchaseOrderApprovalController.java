package com.vone.simrs.purchasing;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0194 (FORM PERSETUJUAN &amp; PEMBATALAN
 * ORDER PEMBELIAN / poApproval.zul).
 */
@RestController
@RequestMapping("/api/purchasing/purchase-order-approval")
public class PurchaseOrderApprovalController {

    private final PurchaseOrderApprovalService purchaseOrderApprovalService;
    private final LegacyAuthService legacyAuthService;

    public PurchaseOrderApprovalController(
            PurchaseOrderApprovalService purchaseOrderApprovalService,
            LegacyAuthService legacyAuthService) {
        this.purchaseOrderApprovalService = purchaseOrderApprovalService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/op/search")
    public ApiResponse<List<PurchaseOrderPoOptionResponse>> searchOp(
            @RequestParam(required = false) String poCode,
            @RequestParam(defaultValue = "false") boolean validated,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderApprovalService.searchPo(poCode, validated));
    }

    @GetMapping("/op/detail")
    public ApiResponse<PurchaseOrderApprovalDetailResponse> opDetail(
            @RequestParam String poCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderApprovalService.getApprovalDetail(poCode));
    }

    @PostMapping("/approve")
    public ApiResponse<PurchaseOrderApprovalResultResponse> approve(
            @RequestParam String poCode, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(purchaseOrderApprovalService.approve(poCode, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
