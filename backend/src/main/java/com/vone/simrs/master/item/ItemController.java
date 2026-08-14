package com.vone.simrs.master.item;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
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
 * REST controller untuk screen SCM0038 (ITEM MASTER).
 */
@RestController
@RequestMapping("/api/master/item")
public class ItemController {

    private final ItemService itemService;
    private final LegacyAuthService legacyAuthService;

    public ItemController(ItemService itemService, LegacyAuthService legacyAuthService) {
        this.itemService = itemService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<ItemRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemService.getItems());
    }

    @GetMapping("/group-options")
    public ApiResponse<List<ItemGroupOptionResponse>> groupOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemService.getItemGroupOptions());
    }

    @GetMapping("/measurement-options")
    public ApiResponse<List<ItemMeasurementOptionResponse>> measurementOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemService.getItemMeasurementOptions());
    }

    @GetMapping("/vendor-options")
    public ApiResponse<List<VendorOptionResponse>> vendorOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemService.getVendorOptions());
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody ItemSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        itemService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        itemService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
