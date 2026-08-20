package com.vone.simrs.master.patient;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0011 (FORM DATA PASIEN / ms_patient.zul).
 */
@RestController
@RequestMapping("/api/master/patient")
public class PatientMasterController {

    private final PatientMasterService patientMasterService;

    public PatientMasterController(PatientMasterService patientMasterService) {
        this.patientMasterService = patientMasterService;
    }

    @GetMapping("/masters")
    public ApiResponse<PatientMasterMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(patientMasterService.getMasters());
    }

    @GetMapping("/patients")
    public ApiResponse<List<PatientSearchRowResponse>> patients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String dob,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(patientMasterService.searchPatients(mrCode, name, address, dob));
    }

    @GetMapping("/detail")
    public ApiResponse<PatientDetailResponse> detail(
            @RequestParam String mrCode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(patientMasterService.getDetail(mrCode));
    }

    @PostMapping("/save")
    public ApiResponse<PatientSaveResultResponse> save(@RequestBody PatientSaveRequest request,
            HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String username = ensureAuthenticated(session);
        return ApiResponse.ok(patientMasterService.save(request, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return patientMasterService.requireUsername(session);
    }
}
