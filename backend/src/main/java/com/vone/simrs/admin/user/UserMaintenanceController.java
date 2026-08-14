package com.vone.simrs.admin.user;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserMaintenanceController {

    private final UserMaintenanceService userMaintenanceService;
    private final LegacyAuthService legacyAuthService;

    public UserMaintenanceController(UserMaintenanceService userMaintenanceService,
            LegacyAuthService legacyAuthService) {
        this.userMaintenanceService = userMaintenanceService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/masters")
    public ApiResponse<UserMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(userMaintenanceService.getMasters());
    }

    @GetMapping
    public ApiResponse<List<UserRowResponse>> users(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(userMaintenanceService.getUsers(keyword));
    }

    @GetMapping("/staff")
    public ApiResponse<List<StaffOptionResponse>> staff(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(userMaintenanceService.searchStaff(code, name));
    }

    @PostMapping
    public ApiResponse<UserRowResponse> createUser(
            @Valid @RequestBody UserSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(userMaintenanceService.createUser(requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserRowResponse> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody UserSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(userMaintenanceService.updateUser(userId, requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable Integer userId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        userMaintenanceService.deleteUser(userId);
        return ApiResponse.ok("OK");
    }

    @GetMapping("/privileges")
    public ApiResponse<List<UserPrivilegeRowResponse>> privileges(
            @RequestParam(required = false) String userName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(userMaintenanceService.getPrivileges(userName));
    }

    @PostMapping("/privileges")
    public ApiResponse<UserPrivilegeRowResponse> createPrivilege(
            @Valid @RequestBody UserPrivilegeSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(userMaintenanceService.createPrivilege(requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/privileges")
    public ApiResponse<UserPrivilegeRowResponse> updatePrivilege(
            @Valid @RequestBody UserPrivilegeSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(userMaintenanceService.updatePrivilege(requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/privileges")
    public ApiResponse<String> deletePrivilege(
            @RequestParam String userName,
            @RequestParam Integer screenId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        userMaintenanceService.deletePrivilege(userName, screenId);
        return ApiResponse.ok("OK");
    }

    @GetMapping("/screens")
    public ApiResponse<List<ScreenOptionResponse>> screens(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(userMaintenanceService.searchScreens(code, name));
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
