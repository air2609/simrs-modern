package com.vone.simrs.master.screen;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master/screens")
public class ScreenMasterController {

    private final ScreenMasterService screenMasterService;
    private final LegacyAuthService legacyAuthService;

    public ScreenMasterController(ScreenMasterService screenMasterService, LegacyAuthService legacyAuthService) {
        this.screenMasterService = screenMasterService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/masters")
    public ApiResponse<ScreenMasterMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(screenMasterService.getMasters());
    }

    @GetMapping
    public ApiResponse<List<ScreenRowResponse>> screens(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(screenMasterService.getScreens(keyword));
    }

    @PostMapping
    public ApiResponse<ScreenRowResponse> createScreen(
            @Valid @RequestBody ScreenMasterSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse
                .ok(screenMasterService.createScreen(requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/{screenId}")
    public ApiResponse<ScreenRowResponse> updateScreen(
            @PathVariable Integer screenId,
            @Valid @RequestBody ScreenMasterSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(screenMasterService.updateScreen(screenId, requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/{screenId}")
    public ApiResponse<String> deleteScreen(@PathVariable Integer screenId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        screenMasterService.deleteScreen(screenId);
        return ApiResponse.ok("OK");
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
