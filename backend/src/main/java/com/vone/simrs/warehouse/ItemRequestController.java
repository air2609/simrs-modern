package com.vone.simrs.warehouse;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0174 (PERMINTAAN O-BM / itemRequest.zul).
 */
@RestController
@RequestMapping("/api/item-request")
public class ItemRequestController {

    private final ItemRequestService itemRequestService;
    private final ItemRequestPrintPdfService itemRequestPrintPdfService;

    public ItemRequestController(ItemRequestService itemRequestService,
            ItemRequestPrintPdfService itemRequestPrintPdfService) {
        this.itemRequestService = itemRequestService;
        this.itemRequestPrintPdfService = itemRequestPrintPdfService;
    }

    @GetMapping("/masters")
    public ApiResponse<ItemRequestMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.getMasters(username));
    }

    @GetMapping("/items")
    public ApiResponse<List<ItemRequestItemResponse>> items(
            @RequestParam Integer warehouseId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.searchItems(warehouseId, code, name));
    }

    @PostMapping("/requests")
    public ApiResponse<ItemRequestSaveResultResponse> save(
            @RequestBody ItemRequestSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.saveRequest(body, username));
    }

    @GetMapping("/requests")
    public ApiResponse<List<ItemRequestGroupResponse>> pendingRequests(
            @RequestParam Integer sourceWarehouseId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.getPendingRequests(sourceWarehouseId));
    }

    @PostMapping("/requests/{irId}/cancel")
    public ApiResponse<ItemRequestActionResultResponse> cancelRequest(
            @PathVariable Integer irId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.cancelRequests(
                java.util.Collections.singletonList(irId)));
    }

    @GetMapping("/approvals")
    public ApiResponse<List<ItemApprovalGroupResponse>> approvals(
            @RequestParam Integer sourceWarehouseId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.getApprovals(sourceWarehouseId));
    }

    @PostMapping("/approvals/{mutationId}/approve")
    public ApiResponse<ItemRequestActionResultResponse> approve(
            @PathVariable Integer mutationId, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.approveMutations(
                java.util.Collections.singletonList(mutationId), username));
    }

    @PostMapping("/approvals/{mutationId}/cancel")
    public ApiResponse<ItemRequestActionResultResponse> cancelApproval(
            @PathVariable Integer mutationId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.cancelMutations(
                java.util.Collections.singletonList(mutationId)));
    }

    @GetMapping("/history")
    public ApiResponse<List<ItemRequestGroupResponse>> history(
            @RequestParam Integer sourceWarehouseId,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemRequestService.getHistory(sourceWarehouseId, from, to));
    }

    @GetMapping("/history/print")
    public ResponseEntity<byte[]> printHistory(
            @RequestParam Integer sourceWarehouseId,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        byte[] pdf = itemRequestPrintPdfService.generateHistoryPdf(sourceWarehouseId, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=history-permintaan.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return itemRequestService.requireUsername(session);
    }
}
