package com.vone.simrs.antrian;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0053 (MASTER ANTRIAN DOKTER / delayAntrian.zul).
 */
@RestController
@RequestMapping("/api/antrian/delay")
public class DelayAntrianController {

    private final DelayAntrianService delayAntrianService;

    public DelayAntrianController(DelayAntrianService delayAntrianService) {
        this.delayAntrianService = delayAntrianService;
    }

    @GetMapping
    public ApiResponse<DelayAntrianMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(delayAntrianService.getMasters());
    }

    @PostMapping
    public ApiResponse<DelayAntrianActionResultResponse> save(
            @RequestBody DelayAntrianSaveRequest requestBody, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(delayAntrianService.save(requestBody));
    }

    @GetMapping("/queue")
    public ApiResponse<List<DelayAntrianQueueRowResponse>> queue(
            @RequestParam Integer doctorId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(delayAntrianService.getQueue(doctorId));
    }

    @PostMapping("/queue/take-out")
    public ApiResponse<DelayAntrianActionResultResponse> takeOut(
            @RequestBody DelayAntrianTakeOutRequest requestBody, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(delayAntrianService.takeOut(requestBody.getRegistrationId()));
    }

    private String ensureAuthenticated(HttpSession session) {
        return delayAntrianService.requireUsername(session);
    }
}
