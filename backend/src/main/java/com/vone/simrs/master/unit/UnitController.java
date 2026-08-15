package com.vone.simrs.master.unit;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.master.treatment.CoaOptionResponse;
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
 * REST controller untuk screen SCM0024 (UNIT MASTER).
 */
@RestController
@RequestMapping("/api/master/unit")
public class UnitController {

    private final UnitService unitService;
    private final LegacyAuthService legacyAuthService;

    public UnitController(UnitService unitService, LegacyAuthService legacyAuthService) {
        this.unitService = unitService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<UnitRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(unitService.getUnits());
    }

    @GetMapping("/division-options")
    public ApiResponse<List<DivisionOptionResponse>> divisionOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(unitService.getDivisionOptions());
    }

    @GetMapping("/warehouse-options")
    public ApiResponse<List<WarehouseOptionResponse>> warehouseOptions(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(unitService.getWarehouseOptions());
    }

    @GetMapping("/coa-search")
    public ApiResponse<List<CoaOptionResponse>> searchCoa(@RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(unitService.searchCoa(keyword));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody UnitSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        unitService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        unitService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
