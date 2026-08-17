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
 * REST controller untuk screen SC0195 (BUKTI PENERIMAAN BARANG / BPP) beserta
 * tab INPUT BATCH NO. (SC0195B).
 */
@RestController
@RequestMapping("/api/purchasing/delivery-order")
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;
    private final LegacyAuthService legacyAuthService;

    public DeliveryOrderController(DeliveryOrderService deliveryOrderService,
            LegacyAuthService legacyAuthService) {
        this.deliveryOrderService = deliveryOrderService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/masters")
    public ApiResponse<DeliveryOrderMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.getMasters(username));
    }

    @GetMapping("/po/search")
    public ApiResponse<List<PurchaseOrderPoOptionResponse>> searchPo(
            @RequestParam(required = false) String poCode,
            @RequestParam(required = false) String supName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.searchPo(poCode, supName));
    }

    @GetMapping("/po/detail")
    public ApiResponse<DeliveryOrderPoDetailResponse> poDetail(
            @RequestParam String poCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.getPoDetail(poCode));
    }

    @GetMapping("/do/search")
    public ApiResponse<List<DeliveryOrderDoOptionResponse>> searchDo(
            @RequestParam(required = false) String doCode,
            @RequestParam(required = false) String whouseCode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.searchDo(doCode, whouseCode));
    }

    @GetMapping("/do/detail")
    public ApiResponse<DeliveryOrderDoDetailResponse> doDetail(
            @RequestParam String doCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.getDoDetail(doCode));
    }

    @PostMapping
    public ApiResponse<DeliveryOrderResultResponse> save(
            @RequestBody DeliveryOrderSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.save(body, username));
    }

    @PostMapping("/update")
    public ApiResponse<Void> update(@RequestBody DeliveryOrderUpdateRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        deliveryOrderService.update(body, username);
        return ApiResponse.ok(null);
    }

    @PostMapping("/revoke")
    public ApiResponse<Void> revoke(@RequestParam String doCode, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        deliveryOrderService.revoke(doCode, username);
        return ApiResponse.ok(null);
    }

    @GetMapping("/batch/masters")
    public ApiResponse<DeliveryOrderBatchMastersResponse> batchMasters(
            @RequestParam String doCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.getBatchMasters(doCode));
    }

    @GetMapping("/batch/measurements")
    public ApiResponse<List<DeliveryOrderMeasurementOptionResponse>> batchMeasurements(
            @RequestParam String code, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.getMeasurementOptions(code));
    }

    @GetMapping("/batch/check-duplicate")
    public ApiResponse<Boolean> checkDuplicate(@RequestParam String batchNo,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.isBatchDuplicate(batchNo));
    }

    @PostMapping("/approve")
    public ApiResponse<DeliveryOrderResultResponse> approve(
            @RequestBody DeliveryOrderApproveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(deliveryOrderService.approve(body, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
