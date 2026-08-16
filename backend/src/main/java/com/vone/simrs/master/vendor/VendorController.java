package com.vone.simrs.master.vendor;

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
 * REST controller untuk screen SCM0043 (VENDOR MASTER / FORM MASTER SUPPLIER).
 */
@RestController
@RequestMapping("/api/master/vendor")
public class VendorController {

    private final VendorService vendorService;
    private final LegacyAuthService legacyAuthService;

    public VendorController(VendorService vendorService, LegacyAuthService legacyAuthService) {
        this.vendorService = vendorService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<VendorRowResponse>> list(
            @RequestParam(required = false) String search, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(vendorService.getVendors(search));
    }

    @GetMapping("/masters")
    public ApiResponse<VendorMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(vendorService.getMasters());
    }

    @GetMapping("/coa-search")
    public ApiResponse<List<CoaOptionResponse>> coaSearch(@RequestParam String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(vendorService.searchCoa(keyword));
    }

    @PostMapping("/save")

    public ApiResponse<Void> save(@RequestBody VendorSaveRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        vendorService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        vendorService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
