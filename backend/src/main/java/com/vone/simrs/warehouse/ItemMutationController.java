package com.vone.simrs.warehouse;

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
 * REST controller untuk screen SC0121 (FORM MUTASI ITEM / itemMutation.zul).
 */
@RestController
@RequestMapping("/api/item-mutation")
public class ItemMutationController {

    private final ItemMutationService itemMutationService;

    public ItemMutationController(ItemMutationService itemMutationService) {
        this.itemMutationService = itemMutationService;
    }

    @GetMapping("/masters")
    public ApiResponse<List<ItemRequestWarehouseResponse>> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemMutationService.getMasters(username));
    }

    @GetMapping("/requests")
    public ApiResponse<List<ItemRequestGroupResponse>> requests(
            @RequestParam Integer warehouseId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemMutationService.getRequests(warehouseId));
    }

    @GetMapping("/requests/{irId}/batches")
    public ApiResponse<List<ItemMutationBatchResponse>> batches(
            @RequestParam Integer warehouseId,
            @RequestParam Integer irId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemMutationService.getBatches(warehouseId, irId));
    }

    @PostMapping("/send")
    public ApiResponse<ItemRequestActionResultResponse> send(
            @RequestBody ItemMutationSendRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemMutationService.send(requestBody, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return itemMutationService.requireUsername(session);
    }
}
