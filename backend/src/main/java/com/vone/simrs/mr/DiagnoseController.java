package com.vone.simrs.mr;

import com.vone.simrs.apotik.ApotikItemOptionResponse;
import com.vone.simrs.apotik.ApotikPatientTypeResponse;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0206 (FORM REKAM MEDIS DIAGNOSA).
 */
@RestController
@RequestMapping("/api/mr/diagnose")
public class DiagnoseController {

    private final DiagnoseService diagnoseService;

    public DiagnoseController(DiagnoseService diagnoseService) {
        this.diagnoseService = diagnoseService;
    }

    @GetMapping("/patient-types")
    public ApiResponse<List<ApotikPatientTypeResponse>> patientTypes(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(diagnoseService.getPatientTypes());
    }

    @GetMapping("/registration")
    public ApiResponse<DiagnoseRegistrationResponse> registration(
            @RequestParam String mrCode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(diagnoseService.getRegistration(mrCode));
    }

    @GetMapping("/patients/search")
    public ApiResponse<List<DiagnosePatientSearchResultResponse>> searchPatients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(diagnoseService.searchPatients(mrCode, patientName, birthDate, address));
    }

    @GetMapping("/icd/search")
    public ApiResponse<List<DiagnoseIcdOptionResponse>> searchIcd(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(diagnoseService.searchIcd(code, name));
    }

    @GetMapping("/items/search")
    public ApiResponse<List<ApotikItemOptionResponse>> searchItems(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "false") boolean isRajal,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(diagnoseService.searchItems(code, name, isRajal));
    }

    @GetMapping("/history")
    public ApiResponse<List<DiagnoseHistoryItemResponse>> history(
            @RequestParam String mrCode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(diagnoseService.getHistory(mrCode));
    }

    @PostMapping
    public ApiResponse<DiagnoseSaveResultResponse> save(
            @Valid @RequestBody DiagnoseSaveRequestBody body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(diagnoseService.saveDiagnose(body, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return diagnoseService.requireUsername(session);
    }
}
