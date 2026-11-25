package com.vone.simrs.satusehat;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint sinkronisasi data pasien ke SATUSEHAT.
 */
@RestController
@RequestMapping("/api/satusehat")
public class SatuSehatPatientController {

    private final SatuSehatPatientService satuSehatPatientService;

    public SatuSehatPatientController(SatuSehatPatientService satuSehatPatientService) {
        this.satuSehatPatientService = satuSehatPatientService;
    }

    /**
     * Sinkronkan pasien (berdasarkan No. MR) ke SATUSEHAT.
     * Membuat Patient baru bila belum ada, atau memakai IHS yang sudah ada.
     */
    @PostMapping("/patient/sync")
    public ApiResponse<SatuSehatPatientSyncResponse> syncPatient(
            @RequestParam String mrCode,
            HttpServletRequest request) {
        satuSehatPatientService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatPatientService.syncPatient(mrCode));
    }
}
