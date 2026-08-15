package com.vone.simrs.master.icd;

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
 * REST controller untuk screen SCM0027 (ICD MASTER).
 */
@RestController
@RequestMapping("/api/master/icd")
public class IcdController {

    private final IcdService icdService;
    private final LegacyAuthService legacyAuthService;

    public IcdController(IcdService icdService, LegacyAuthService legacyAuthService) {
        this.icdService = icdService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<IcdRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(icdService.getIcds());
    }

    @GetMapping("/search")
    public ApiResponse<List<IcdRowResponse>> search(@RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(icdService.searchIcds(code, name));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody IcdSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        icdService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        icdService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
