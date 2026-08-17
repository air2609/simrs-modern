package com.vone.simrs.antrian.apotik;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0054 (KONTROL ANTRIAN APOTIK).
 */
@RestController
@RequestMapping("/api/antrian/apotik")
public class AntrianApotikController {

    private final AntrianApotikService antrianApotikService;

    public AntrianApotikController(AntrianApotikService antrianApotikService) {
        this.antrianApotikService = antrianApotikService;
    }

    @GetMapping
    public ApiResponse<AntrianApotikResponse> data(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(antrianApotikService.getAntrianData());
    }

    @PostMapping("/notes/{noteId}/move-to-ready")
    public ApiResponse<Void> moveToReady(@PathVariable Integer noteId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        antrianApotikService.moveToReady(noteId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/notes/{noteId}/take-out")
    public ApiResponse<Void> takeOut(@PathVariable Integer noteId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        antrianApotikService.takeOut(noteId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/text")
    public ApiResponse<Void> saveText(@RequestBody AntrianApotikSaveRequest body, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        antrianApotikService.saveAntrianText(body.getAntrianText());
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return antrianApotikService.requireUsername(session);
    }
}
