package com.vone.simrs.master.iteminventory;

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
 * REST controller untuk screen SCM0057 (FORM UPDATE ITEM / updateInventory.zul).
 */
@RestController
@RequestMapping("/api/master/update-inventory")
public class UpdateInventoryController {

    private final UpdateInventoryService updateInventoryService;
    private final LegacyAuthService legacyAuthService;

    public UpdateInventoryController(UpdateInventoryService updateInventoryService,
            LegacyAuthService legacyAuthService) {
        this.updateInventoryService = updateInventoryService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/items")
    public ApiResponse<List<UpdateInventoryItemResponse>> items(
            @RequestParam(required = false) String keyword, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(updateInventoryService.getItems(keyword));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody UpdateInventorySaveRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        updateInventoryService.save(body, username);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
