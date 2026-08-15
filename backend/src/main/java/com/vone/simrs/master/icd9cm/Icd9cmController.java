package com.vone.simrs.master.icd9cm;

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
 * REST controller untuk screen SCM0028 (ICD-9-CM MASTER).
 */
@RestController
@RequestMapping("/api/master/icd9cm")
public class Icd9cmController {

    private final Icd9cmService icd9cmService;
    private final LegacyAuthService legacyAuthService;

    public Icd9cmController(Icd9cmService icd9cmService, LegacyAuthService legacyAuthService) {
        this.icd9cmService = icd9cmService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<Icd9cmRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(icd9cmService.getIcd9cms());
    }

    @GetMapping("/search")
    public ApiResponse<List<Icd9cmRowResponse>> search(@RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(icd9cmService.searchIcd9cms(code, name));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody Icd9cmSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        icd9cmService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        icd9cmService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
