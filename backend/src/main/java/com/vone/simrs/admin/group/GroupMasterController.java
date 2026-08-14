package com.vone.simrs.admin.group;

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
@RequestMapping("/api/admin/groups")
public class GroupMasterController {

    private final GroupMasterService groupMasterService;
    private final LegacyAuthService legacyAuthService;

    public GroupMasterController(GroupMasterService groupMasterService, LegacyAuthService legacyAuthService) {
        this.groupMasterService = groupMasterService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<GroupRowResponse>> groups(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(groupMasterService.getGroups(keyword));
    }

    @PostMapping
    public ApiResponse<GroupRowResponse> createGroup(
            @Valid @RequestBody GroupSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(groupMasterService.createGroup(requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/{groupId}")
    public ApiResponse<GroupRowResponse> updateGroup(
            @PathVariable Integer groupId,
            @Valid @RequestBody GroupSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(groupMasterService.updateGroup(groupId, requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<String> deleteGroup(@PathVariable Integer groupId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        groupMasterService.deleteGroup(groupId);
        return ApiResponse.ok("OK");
    }

    @GetMapping("/privileges")
    public ApiResponse<List<GroupPrivilegeRowResponse>> privileges(
            @RequestParam(required = false) String groupCode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(groupMasterService.getPrivileges(groupCode));
    }

    @PostMapping("/privileges")
    public ApiResponse<GroupPrivilegeRowResponse> createPrivilege(
            @Valid @RequestBody GroupPrivilegeSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(groupMasterService.createPrivilege(requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/privileges")
    public ApiResponse<GroupPrivilegeRowResponse> updatePrivilege(
            @Valid @RequestBody GroupPrivilegeSaveRequest requestBody,
            HttpServletRequest request) {
        return ApiResponse.ok(groupMasterService.updatePrivilege(requestBody,
                ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/privileges")
    public ApiResponse<String> deletePrivilege(
            @RequestParam String groupCode,
            @RequestParam Integer screenId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        groupMasterService.deletePrivilege(groupCode, screenId);
        return ApiResponse.ok("OK");
    }

    @GetMapping("/screens")
    public ApiResponse<List<GroupScreenOptionResponse>> screens(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(groupMasterService.searchScreens(code, name));
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
