package com.vone.simrs.master.division;

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
 * REST controller untuk screen SCM0022 (DIVISION MASTER).
 */
@RestController
@RequestMapping("/api/master/division")
public class DivisionController {

    private final DivisionService divisionService;
    private final LegacyAuthService legacyAuthService;

    public DivisionController(DivisionService divisionService, LegacyAuthService legacyAuthService) {
        this.divisionService = divisionService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<DivisionRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(divisionService.getDivisions());
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody DivisionSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        divisionService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        divisionService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
