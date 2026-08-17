package com.vone.simrs.accounting;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0176 (REKAP GL / rekapGl.zul).
 */
@RestController
@RequestMapping("/api/accounting/rekap-gl")
public class RekapGlController {

    private final RekapGlService rekapGlService;
    private final LegacyAuthService legacyAuthService;

    public RekapGlController(RekapGlService rekapGlService, LegacyAuthService legacyAuthService) {
        this.rekapGlService = rekapGlService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<RekapGlRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(rekapGlService.list());
    }

    @PostMapping
    public ApiResponse<Void> save(@RequestBody RekapGlSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        rekapGlService.save(body, username);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        byte[] content = rekapGlService.getFile(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rekap_gl.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
