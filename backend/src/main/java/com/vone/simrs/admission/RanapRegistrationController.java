package com.vone.simrs.admission;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk pendaftaran pasien RAWAT INAP (SC0001 tab 2 / PasienRanap.zul).
 */
@RestController
@RequestMapping("/api/admission/ranap")
public class RanapRegistrationController {

    private final RanapRegistrationService ranapRegistrationService;

    public RanapRegistrationController(RanapRegistrationService ranapRegistrationService) {
        this.ranapRegistrationService = ranapRegistrationService;
    }

    @GetMapping("/masters")
    public ApiResponse<RanapMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(ranapRegistrationService.getMasters());
    }

    @GetMapping("/patients")
    public ApiResponse<List<RanapPatientOptionResponse>> patients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String nik,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(ranapRegistrationService.searchPatients(
                mrCode, patientName, nik, birthDate, address));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<RanapPatientDetailResponse> patientDetail(
            @PathVariable String mrCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(ranapRegistrationService.getPatientDetail(mrCode));
    }

    @GetMapping("/halls")
    public ApiResponse<List<RanapHallResponse>> halls(
            @RequestParam Integer classId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(ranapRegistrationService.getHallsByClass(classId));
    }

    @GetMapping("/halls/{hallId}/beds")
    public ApiResponse<List<RanapBedResponse>> beds(
            @PathVariable Integer hallId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(ranapRegistrationService.getBedsByHall(hallId));
    }

    @PostMapping("/registrations")
    public ApiResponse<RanapSaveResultResponse> save(
            @RequestBody RanapSaveRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(ranapRegistrationService.save(requestBody, username));
    }

    @PostMapping("/registrations/cancel")
    public ApiResponse<RanapActionResultResponse> cancel(
            @RequestBody RanapCancelRequest requestBody, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(ranapRegistrationService.cancel(requestBody));
    }

    private String ensureAuthenticated(HttpSession session) {
        return ranapRegistrationService.requireUsername(session);
    }
}
