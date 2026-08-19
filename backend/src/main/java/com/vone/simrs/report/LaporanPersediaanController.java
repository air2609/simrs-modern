package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
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
 * REST controller untuk screen RPT0008 (LAPORAN PERSEDIAAN OBAT-BAHAN MEDIS / laporanPersediaanOBM.zul).
 */
@RestController
@RequestMapping("/api/report/persediaan-obat")
public class LaporanPersediaanController {

    private final LaporanPersediaanService laporanPersediaanService;
    private final LaporanPersediaanPrintService laporanPersediaanPrintService;

    public LaporanPersediaanController(LaporanPersediaanService laporanPersediaanService,
            LaporanPersediaanPrintService laporanPersediaanPrintService) {
        this.laporanPersediaanService = laporanPersediaanService;
        this.laporanPersediaanPrintService = laporanPersediaanPrintService;
    }

    @GetMapping("/masters")
    public ApiResponse<List<WardUnitResponse>> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanPersediaanService.getUnits(username));
    }

    @GetMapping("/report")
    public ApiResponse<LaporanPersediaanResponse> report(
            @RequestParam Integer unitId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanPersediaanService.getReport(unitId));
    }

    @GetMapping("/print")
    public ResponseEntity<byte[]> print(
            @RequestParam Integer unitId,
            HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        LaporanPersediaanResponse data = laporanPersediaanService.getReport(unitId);
        byte[] pdf = laporanPersediaanPrintService.generatePdf(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan-persediaan-obat.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return laporanPersediaanService.requireUsername(session);
    }
}
