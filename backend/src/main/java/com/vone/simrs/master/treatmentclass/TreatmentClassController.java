package com.vone.simrs.master.treatmentclass;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0021 (KELAS TARIF / TREATMENT CLASS MASTER).
 */
@RestController
@RequestMapping("/api/master/treatment-class")
public class TreatmentClassController {

    private final TreatmentClassService treatmentClassService;
    private final LegacyAuthService legacyAuthService;

    public TreatmentClassController(TreatmentClassService treatmentClassService, LegacyAuthService legacyAuthService) {
        this.treatmentClassService = treatmentClassService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<TreatmentClassRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(treatmentClassService.getTreatmentClasses());
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody TreatmentClassSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        treatmentClassService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        treatmentClassService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
