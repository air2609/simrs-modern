package com.vone.simrs.cashier;

import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.ward.WardPatientOptionResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0208 (FORM INFORMASI REKAP OBAT / rekapObat.zul).
 */
@RestController
@RequestMapping("/api/cashier/rekap-obat")
public class RekapObatController {

    private final RekapObatService rekapObatService;

    public RekapObatController(RekapObatService rekapObatService) {
        this.rekapObatService = rekapObatService;
    }

    @GetMapping("/patients/ranap")
    public ApiResponse<List<WardPatientOptionResponse>> ranapPatients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(rekapObatService.searchRanapPatients(mrCode, patientName, address));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<CashierPatientDetailResponse> patientDetail(
            @PathVariable String mrCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(rekapObatService.getPatientDetail(mrCode));
    }

    @GetMapping("/rekap")
    public ApiResponse<RekapObatResponse> rekap(
            @RequestParam Integer registrationId,
            @RequestParam(defaultValue = "10") Integer type,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(rekapObatService.getRekap(registrationId, type));
    }

    private String ensureAuthenticated(HttpSession session) {
        return rekapObatService.requireUsername(session);
    }
}
