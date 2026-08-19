package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.report.LaporanHarianBangsalService.PatientRegistrationDetail;
import com.vone.simrs.ward.WardPatientOptionResponse;
import com.vone.simrs.ward.WardUnitResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen RPT0006 (LAPORAN HARIAN BANGSAL / laporanHarianBangsal.zul).
 */
@RestController
@RequestMapping("/api/report/harian-bangsal")
public class LaporanHarianBangsalController {

    private final LaporanHarianBangsalService laporanHarianBangsalService;
    private final LaporanHarianBangsalPrintService laporanHarianBangsalPrintService;

    public LaporanHarianBangsalController(LaporanHarianBangsalService laporanHarianBangsalService,
            LaporanHarianBangsalPrintService laporanHarianBangsalPrintService) {
        this.laporanHarianBangsalService = laporanHarianBangsalService;
        this.laporanHarianBangsalPrintService = laporanHarianBangsalPrintService;
    }

    @GetMapping("/masters")
    public ApiResponse<List<WardUnitResponse>> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanHarianBangsalService.getUnits(username));
    }

    @GetMapping("/patients")
    public ApiResponse<List<WardPatientOptionResponse>> patients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanHarianBangsalService.searchPatients(mrCode, patientName, address));
    }

    @GetMapping("/registration")
    public ApiResponse<PatientRegistrationDetail> registration(
            @RequestParam String mrCode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanHarianBangsalService.getRegistration(mrCode));
    }

    @GetMapping("/report")
    public ApiResponse<LaporanHarianBangsalResponse> report(
            @RequestParam Integer regId,
            @RequestParam String unitCode,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanHarianBangsalService.getReport(regId, unitCode, from, to));
    }

    @GetMapping("/print")
    public ResponseEntity<byte[]> print(
            @RequestParam Integer regId,
            @RequestParam String unitCode,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        LaporanHarianBangsalResponse data =
                laporanHarianBangsalService.getReport(regId, unitCode, from, to);
        byte[] pdf = laporanHarianBangsalPrintService.generatePdf(data, toDisplayDate(from),
                toDisplayDate(to));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan-harian-bangsal.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return laporanHarianBangsalService.requireUsername(session);
    }

    private String toDisplayDate(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return "";
        }
        String[] parts = iso.split("-");
        return parts.length == 3 ? parts[2] + "-" + parts[1] + "-" + parts[0] : iso;
    }
}
