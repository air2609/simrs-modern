package com.vone.simrs.master.treatmentgroup;

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
 * REST controller untuk screen SCM0023 (TREATMENT GROUP MASTER).
 */
@RestController
@RequestMapping("/api/master/treatment-group")
public class TreatmentGroupController {

    private final TreatmentGroupService treatmentGroupService;
    private final LegacyAuthService legacyAuthService;

    public TreatmentGroupController(TreatmentGroupService treatmentGroupService, LegacyAuthService legacyAuthService) {
        this.treatmentGroupService = treatmentGroupService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<TreatmentGroupRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(treatmentGroupService.getTreatmentGroups());
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody TreatmentGroupSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        treatmentGroupService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        treatmentGroupService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
