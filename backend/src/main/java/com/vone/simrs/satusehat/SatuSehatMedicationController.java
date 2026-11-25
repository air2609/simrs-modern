package com.vone.simrs.satusehat;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint sinkronisasi obat (MedicationRequest &amp; MedicationDispense).
 */
@RestController
@RequestMapping("/api/satusehat")
public class SatuSehatMedicationController {

    private final SatuSehatMedicationService satuSehatMedicationService;

    public SatuSehatMedicationController(SatuSehatMedicationService satuSehatMedicationService) {
        this.satuSehatMedicationService = satuSehatMedicationService;
    }

    /** Sinkronkan resep obat menjadi MedicationRequest. */
    @PostMapping("/medication-request/sync")
    public ApiResponse<SatuSehatClinicalSyncResponse> syncMedicationRequests(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        satuSehatMedicationService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatMedicationService.syncMedicationRequests(registrationCode));
    }

    /** Sinkronkan penyerahan obat menjadi MedicationDispense. */
    @PostMapping("/medication-dispense/sync")
    public ApiResponse<SatuSehatClinicalSyncResponse> syncMedicationDispenses(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        satuSehatMedicationService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatMedicationService.syncMedicationDispenses(registrationCode));
    }
}
