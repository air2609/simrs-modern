package com.vone.simrs.antrian.apotikdisplay;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0020 (papan display OBAT PASIEN SUDAH JADI).
 */
@RestController
@RequestMapping("/api/antrian/apotik-display")
public class AntrianApotikDisplayController {

    private final AntrianApotikDisplayService antrianApotikDisplayService;
    private final LegacyAuthService legacyAuthService;

    public AntrianApotikDisplayController(AntrianApotikDisplayService antrianApotikDisplayService,
            LegacyAuthService legacyAuthService) {
        this.antrianApotikDisplayService = antrianApotikDisplayService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<AntrianApotikDisplayResponse> data(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(antrianApotikDisplayService.getDisplayData());
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
