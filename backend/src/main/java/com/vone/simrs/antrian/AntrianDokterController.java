package com.vone.simrs.antrian;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0019 (ANTRIAN DOKTER / antrianDokter.zul).
 */
@RestController
@RequestMapping("/api/antrian/display")
public class AntrianDokterController {

    private final AntrianDisplayService antrianDisplayService;

    public AntrianDokterController(AntrianDisplayService antrianDisplayService) {
        this.antrianDisplayService = antrianDisplayService;
    }

    @GetMapping
    public ApiResponse<AntrianDisplayResponse> display(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(antrianDisplayService.getDisplay());
    }

    private String ensureAuthenticated(HttpSession session) {
        return antrianDisplayService.requireUsername(session);
    }
}
