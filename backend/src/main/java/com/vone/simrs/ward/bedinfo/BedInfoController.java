package com.vone.simrs.ward.bedinfo;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ward/bed-info")
public class BedInfoController {

    private final BedInfoService bedInfoService;
    private final LegacyAuthService legacyAuthService;

    public BedInfoController(BedInfoService bedInfoService, LegacyAuthService legacyAuthService) {
        this.bedInfoService = bedInfoService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<BedInfoRowResponse>> bedInfo(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedInfoService.getBedInfo());
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
