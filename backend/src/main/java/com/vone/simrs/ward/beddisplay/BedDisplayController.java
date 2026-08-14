package com.vone.simrs.ward.beddisplay;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ward/bed-display")
public class BedDisplayController {

    private final BedDisplayService bedDisplayService;
    private final LegacyAuthService legacyAuthService;

    public BedDisplayController(BedDisplayService bedDisplayService, LegacyAuthService legacyAuthService) {
        this.bedDisplayService = bedDisplayService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<BedDisplayRowResponse>> activeBeds(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedDisplayService.getActiveBeds());
    }

    @PostMapping("/save")
    public ApiResponse<String> saveBulk(
            @Valid @RequestBody List<BedDisplaySaveRequest> requestBody,
            HttpServletRequest request) {
        bedDisplayService.saveBulk(requestBody, ensureAuthenticated(request.getSession(false)));
        return ApiResponse.ok("OK");
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
