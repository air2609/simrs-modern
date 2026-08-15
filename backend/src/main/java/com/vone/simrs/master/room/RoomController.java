package com.vone.simrs.master.room;

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
 * REST controller untuk screen SCM0019 (FORM KAMAR / ROOM MASTER).
 */
@RestController
@RequestMapping("/api/master/room")
public class RoomController {

    private final RoomService roomService;
    private final LegacyAuthService legacyAuthService;

    public RoomController(RoomService roomService, LegacyAuthService legacyAuthService) {
        this.roomService = roomService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<RoomRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(roomService.getRooms());
    }

    @GetMapping("/halls")
    public ApiResponse<List<HallOptionResponse>> halls(@RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(roomService.searchHalls(name));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody RoomSaveRequest body, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        roomService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        roomService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
