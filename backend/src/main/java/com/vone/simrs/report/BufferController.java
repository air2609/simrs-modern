package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.ward.WardUnitResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0018 (BUFFER MONITORING / buffer.zul).
 */
@RestController
@RequestMapping("/api/report/buffer")
public class BufferController {

    private final BufferService bufferService;

    public BufferController(BufferService bufferService) {
        this.bufferService = bufferService;
    }

    @GetMapping("/masters")
    public ApiResponse<List<WardUnitResponse>> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bufferService.getUnits(username));
    }

    @GetMapping("/report")
    public ApiResponse<BufferResponse> report(
            @RequestParam Integer warehouseId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bufferService.getReport(warehouseId));
    }

    private String ensureAuthenticated(HttpSession session) {
        return bufferService.requireUsername(session);
    }
}
