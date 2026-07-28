package com.vone.simrs.master.treatment;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master/lab-treatment")
public class LabTreatmentMasterController {

    private final LabTreatmentMasterService labTreatmentMasterService;

    public LabTreatmentMasterController(LabTreatmentMasterService labTreatmentMasterService) {
        this.labTreatmentMasterService = labTreatmentMasterService;
    }

    @GetMapping("/groups")
    public ApiResponse<List<LabTreatmentGroupResponse>> groups(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(labTreatmentMasterService.getLabGroups());
    }

    @GetMapping("/groups/{groupId}/treatments")
    public ApiResponse<List<LabTreatmentResponse>> treatmentsByGroup(
            @PathVariable Integer groupId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(labTreatmentMasterService.getTreatmentsByGroup(groupId));
    }

    @GetMapping("/treatments/{treatmentId}/details")
    public ApiResponse<List<LabTreatmentDetailResponse>> detailsByTreatment(
            @PathVariable Integer treatmentId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(labTreatmentMasterService.getDetailsByTreatment(treatmentId));
    }

    @PostMapping("/details")
    public ApiResponse<LabTreatmentDetailResponse> createDetail(
            @Valid @RequestBody LabTreatmentDetailSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(labTreatmentMasterService.createDetail(requestBody, username));
    }

    @PutMapping("/details/{detailId}")
    public ApiResponse<LabTreatmentDetailResponse> updateDetail(
            @PathVariable Integer detailId,
            @Valid @RequestBody LabTreatmentDetailSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(labTreatmentMasterService.updateDetail(detailId, requestBody, username));
    }

    @DeleteMapping("/details/{detailId}")
    public ApiResponse<String> deleteDetail(
            @PathVariable Integer detailId,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        labTreatmentMasterService.deleteDetail(detailId, username);
        return ApiResponse.ok("OK");
    }

    private String ensureAuthenticated(HttpSession session) {
        return labTreatmentMasterService.requireUsername(session);
    }
}
