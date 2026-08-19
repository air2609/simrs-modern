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
 * REST controller untuk screen RPT0004 (LAPORAN TRANSAKSI PASIEN / laporanPasienPoliUgd.zul).
 */
@RestController
@RequestMapping("/api/report/poli-ugd")
public class LaporanTransaksiController {

    private final LaporanTransaksiService laporanTransaksiService;
    private final LaporanTransaksiPrintService laporanTransaksiPrintService;

    public LaporanTransaksiController(LaporanTransaksiService laporanTransaksiService,
            LaporanTransaksiPrintService laporanTransaksiPrintService) {
        this.laporanTransaksiService = laporanTransaksiService;
        this.laporanTransaksiPrintService = laporanTransaksiPrintService;
    }

    @GetMapping("/masters")
    public ApiResponse<List<WardUnitResponse>> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanTransaksiService.getUnits(username));
    }

    @GetMapping("/report")
    public ApiResponse<LaporanTransaksiResponse> report(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam Integer unitId,
            @RequestParam(required = false) String shift,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laporanTransaksiService.getReport(from, to, unitId, shift));
    }

    @GetMapping("/print")
    public ResponseEntity<byte[]> print(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam Integer unitId,
            @RequestParam(required = false) String shift,
            HttpServletRequest request) throws Exception {
        String username = ensureAuthenticated(request.getSession(false));
        LaporanTransaksiResponse data = laporanTransaksiService.getReport(from, to, unitId, shift);
        byte[] pdf = laporanTransaksiPrintService.generatePdf(data, toDisplayDate(from),
                toDisplayDate(to), username);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan-transaksi.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return laporanTransaksiService.requireUsername(session);
    }

    private String toDisplayDate(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return "";
        }
        String[] parts = iso.split("-");
        return parts.length == 3 ? parts[2] + "-" + parts[1] + "-" + parts[0] : iso;
    }
}
