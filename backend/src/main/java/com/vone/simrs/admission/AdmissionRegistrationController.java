package com.vone.simrs.admission;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admission/registration")
public class AdmissionRegistrationController {

    private final AdmissionRegistrationService admissionRegistrationService;

    public AdmissionRegistrationController(AdmissionRegistrationService admissionRegistrationService) {
        this.admissionRegistrationService = admissionRegistrationService;
    }

    @GetMapping("/masters")
    public ApiResponse<AdmissionRegistrationMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(admissionRegistrationService.getMasters());
    }

    @GetMapping("/provinces/{provinceCode}/regencies")
    public ApiResponse<List<OptionResponse>> regencies(
        @PathVariable String provinceCode,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(admissionRegistrationService.getRegencies(provinceCode));
    }

    @GetMapping("/regencies/{regencyCode}/districts")
    public ApiResponse<List<OptionResponse>> districts(
        @PathVariable String regencyCode,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(admissionRegistrationService.getDistricts(regencyCode));
    }

    @GetMapping("/districts/{districtCode}/villages")
    public ApiResponse<List<OptionResponse>> villages(
        @PathVariable String districtCode,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(admissionRegistrationService.getVillages(districtCode));
    }

    @GetMapping("/units/{unitId}/doctors")
    public ApiResponse<List<DoctorOptionResponse>> doctors(
        @PathVariable Integer unitId,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(admissionRegistrationService.getDoctors(unitId));
    }

    @GetMapping("/patients/search")
    public ApiResponse<List<AdmissionPatientSearchResponse>> search(
        @RequestParam(required = false) String mrCode,
        @RequestParam(required = false) String patientName,
        @RequestParam(required = false) String nik,
        @RequestParam(required = false) String birthDate,
        @RequestParam(required = false) String address,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(admissionRegistrationService.searchPatients(mrCode, patientName, nik, birthDate, address));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<AdmissionPatientDetailResponse> patientDetail(
        @PathVariable String mrCode,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(admissionRegistrationService.getPatientDetail(mrCode));
    }

    @PostMapping
    public ApiResponse<AdmissionRegistrationResultResponse> save(
        @Valid @RequestBody AdmissionRegistrationSaveRequest requestBody,
        HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        String username = ensureAuthenticated(session);
        return ApiResponse.ok(admissionRegistrationService.saveRegistration(requestBody, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return admissionRegistrationService.requireUsername(session);
    }
}
