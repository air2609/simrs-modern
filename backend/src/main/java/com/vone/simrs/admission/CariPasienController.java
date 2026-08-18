package com.vone.simrs.admission;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0005 (PENCARIAN PASIEN RAWAT INAP / CariPasien.zul).
 */
@RestController
@RequestMapping("/api/admission/cari-pasien")
public class CariPasienController {

    private final CariPasienService cariPasienService;

    public CariPasienController(CariPasienService cariPasienService) {
        this.cariPasienService = cariPasienService;
    }

    @GetMapping("/halls")
    public ApiResponse<List<CariPasienHallResponse>> halls(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cariPasienService.searchHalls(code, name));
    }

    @GetMapping("/patients")
    public ApiResponse<List<CariPasienPatientResponse>> patients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String hall,
            @RequestParam(required = false) String doctor,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cariPasienService.searchRanapPatients(
                mrCode, patientName, address, hall, doctor));
    }

    private String ensureAuthenticated(HttpSession session) {
        return cariPasienService.requireUsername(session);
    }
}
