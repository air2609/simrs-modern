package com.vone.simrs.vitalsign;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller data tanda-tanda vital (TTV).
 */
@RestController
@RequestMapping("/api/vital-sign")
public class VitalSignController {

    private final VitalSignService vitalSignService;

    public VitalSignController(VitalSignService vitalSignService) {
        this.vitalSignService = vitalSignService;
    }

    @GetMapping
    public ApiResponse<List<VitalSignResponse>> list(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        vitalSignService.requireUsername(request.getSession(false));
        return ApiResponse.ok(vitalSignService.listByRegistration(registrationCode));
    }

    @PostMapping
    public ApiResponse<VitalSignResponse> save(
            @RequestBody VitalSignSaveRequest body,
            HttpServletRequest request) {
        String username = vitalSignService.requireUsername(request.getSession(false));
        return ApiResponse.ok(vitalSignService.save(body, username));
    }
}
