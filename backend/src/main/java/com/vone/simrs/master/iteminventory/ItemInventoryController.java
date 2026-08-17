package com.vone.simrs.master.iteminventory;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.master.warehouse.WarehouseOptionResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0032 (FORM ALOKASI ITEM).
 */
@RestController
@RequestMapping("/api/master/item-inventory")
public class ItemInventoryController {

    private final ItemInventoryService itemInventoryService;
    private final LegacyAuthService legacyAuthService;

    public ItemInventoryController(ItemInventoryService itemInventoryService,
            LegacyAuthService legacyAuthService) {
        this.itemInventoryService = itemInventoryService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<ItemInventoryRowResponse>> list(
            @RequestParam(required = false) Integer whouseId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemInventoryService.getInventory(whouseId, keyword));
    }

    @GetMapping("/warehouse-options")
    public ApiResponse<List<WarehouseOptionResponse>> warehouseOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemInventoryService.getWarehouseOptions());
    }

    @GetMapping("/expired-report")
    public ApiResponse<List<ExpiredItemReportResponse>> expiredReport(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemInventoryService.getExpiredReport());
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody ItemInventorySaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        itemInventoryService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        itemInventoryService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
