package com.vone.simrs.mr;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0081 (FORM BERKAS REKAM MEDIS).
 */
@RestController
@RequestMapping("/api/mr/file-status")
public class MrFileStatusController {

    private final MrFileStatusService mrFileStatusService;

    public MrFileStatusController(MrFileStatusService mrFileStatusService) {
        this.mrFileStatusService = mrFileStatusService;
    }

    @GetMapping
    public ApiResponse<List<MrFileStatusItemResponse>> byStatus(
            @RequestParam String status,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrFileStatusService.getByStatus(status));
    }

    @GetMapping("/lookup")
    public ApiResponse<MrFileStatusItemResponse> lookup(
            @RequestParam String code,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrFileStatusService.getByCode(code));
    }

    @GetMapping("/search")
    public ApiResponse<List<MrBorrowSearchResultResponse>> search(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String nik,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrFileStatusService.searchPatients(mrCode, patientName, nik, birthDate, address));
    }

    private String ensureAuthenticated(HttpSession session) {
        return mrFileStatusService.requireUsername(session);
    }
}
