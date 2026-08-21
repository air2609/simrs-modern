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
 * REST controller untuk screen SC0002 (FORM MUTASI KAMAR / MutasiKamar.zul).
 *
 * <p>
 * Migrasi dari legacy {@code MutasiKamarController} + {@code MutasiKamarManagerImpl}
 * + {@code RanapController.getHallList()/getBedBaseOnHall()}.
 */
@RestController
@RequestMapping("/api/admission/bed-mutations")
public class BedMutationController {

    private final BedMutationService bedMutationService;

    public BedMutationController(BedMutationService bedMutationService) {
        this.bedMutationService = bedMutationService;
    }

    @GetMapping("/patients")
    public ApiResponse<List<BedMutationPatientResponse>> patients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedMutationService.searchRanapPatients(mrCode, patientName, address));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<BedMutationDetailResponse> patientDetail(
            @PathVariable String mrCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedMutationService.getPatientDetail(mrCode));
    }

    @GetMapping("/halls")
    public ApiResponse<List<RanapHallResponse>> halls(
            @RequestParam Integer classId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedMutationService.getHallsByClass(classId));
    }

    @GetMapping("/halls/{hallId}/beds")
    public ApiResponse<List<RanapBedResponse>> beds(
            @PathVariable Integer hallId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedMutationService.getBedsByHall(hallId));
    }

    @PostMapping("/save")
    public ApiResponse<BedMutationSaveResultResponse> save(
            @RequestBody BedMutationSaveRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedMutationService.save(requestBody, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return bedMutationService.requireUsername(session);
    }
}
