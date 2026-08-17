package com.vone.simrs.mr;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0006 (PERSIAPAN DOKUMEN REKAM MEDIS).
 */
@RestController
@RequestMapping("/api/mr/preparation")
public class MrPreparationController {

    private final MrPreparationService mrPreparationService;

    public MrPreparationController(MrPreparationService mrPreparationService) {
        this.mrPreparationService = mrPreparationService;
    }

    @GetMapping
    public ApiResponse<MrPreparationResponse> data(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrPreparationService.getPreparationData());
    }

    @PostMapping("/{regId}/mark-ready")
    public ApiResponse<Void> markReady(@PathVariable Integer regId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        mrPreparationService.markReady(regId);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return mrPreparationService.requireUsername(session);
    }
}
