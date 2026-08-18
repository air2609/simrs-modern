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
 * REST controller untuk screen SC0023 (FORM INFORMASI TAGIHAN PASIEN / print.zul).
 */
@RestController
@RequestMapping("/api/cashier/info-tagihan")
public class InfoTagihanController {

    private final InfoTagihanService infoTagihanService;
    private final RekapObatService rekapObatService;

    public InfoTagihanController(InfoTagihanService infoTagihanService,
            RekapObatService rekapObatService) {
        this.infoTagihanService = infoTagihanService;
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

    @GetMapping("/transactions")
    public ApiResponse<InfoTagihanResponse> transactions(
            @RequestParam Integer patientId,
            @RequestParam(required = false) Integer registrationId,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(infoTagihanService.getTransactions(patientId, registrationId, from, to));
    }

    private String ensureAuthenticated(HttpSession session) {
        return infoTagihanService.requireUsername(session);
    }
}
