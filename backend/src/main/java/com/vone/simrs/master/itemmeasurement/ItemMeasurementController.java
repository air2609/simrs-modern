package com.vone.simrs.master.itemmeasurement;

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
 * REST controller untuk screen SCM0040 (ITEM MEASUREMENT MASTER).
 */
@RestController
@RequestMapping("/api/master/item-measurement")
public class ItemMeasurementController {

    private final ItemMeasurementService itemMeasurementService;
    private final LegacyAuthService legacyAuthService;

    public ItemMeasurementController(ItemMeasurementService itemMeasurementService,
            LegacyAuthService legacyAuthService) {
        this.itemMeasurementService = itemMeasurementService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<ItemMeasurementRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemMeasurementService.getItemMeasurements());
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody ItemMeasurementSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        itemMeasurementService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        itemMeasurementService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
