package com.vone.simrs.antrian;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0021 (ANTRIAN DOKTER / antrianPerDokter.zul).
 */
@RestController
@RequestMapping("/api/antrian/per-dokter")
public class AntrianPerDokterController {

    private final AntrianPerDokterService antrianPerDokterService;

    public AntrianPerDokterController(AntrianPerDokterService antrianPerDokterService) {
        this.antrianPerDokterService = antrianPerDokterService;
    }

    @GetMapping
    public ApiResponse<AntrianPerDokterResponse> queue(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = antrianPerDokterService.requireUsername(session);
        return ApiResponse.ok(antrianPerDokterService.getQueue(username));
    }
}
