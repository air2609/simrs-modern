package com.vone.simrs.satusehat;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint sinkronisasi Condition (diagnosa) & Procedure (tindakan).
 */
@RestController
@RequestMapping("/api/satusehat")
public class SatuSehatClinicalController {

    private final SatuSehatClinicalService satuSehatClinicalService;
    private final SatuSehatObservationService satuSehatObservationService;

    public SatuSehatClinicalController(SatuSehatClinicalService satuSehatClinicalService,
            SatuSehatObservationService satuSehatObservationService) {
        this.satuSehatClinicalService = satuSehatClinicalService;
        this.satuSehatObservationService = satuSehatObservationService;
    }

    /** Sinkronkan diagnosa ICD-10 registrasi menjadi Condition. */
    @PostMapping("/condition/sync")
    public ApiResponse<SatuSehatClinicalSyncResponse> syncConditions(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        satuSehatClinicalService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatClinicalService.syncConditions(registrationCode));
    }

    /** Sinkronkan tindakan ICD-9-CM registrasi menjadi Procedure. */
    @PostMapping("/procedure/sync")
    public ApiResponse<SatuSehatClinicalSyncResponse> syncProcedures(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        satuSehatClinicalService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatClinicalService.syncProcedures(registrationCode));
    }

    /** Sinkronkan hasil lab registrasi menjadi Observation. */
    @PostMapping("/observation/sync")
    public ApiResponse<SatuSehatClinicalSyncResponse> syncObservations(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        satuSehatClinicalService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatObservationService.syncObservations(registrationCode));
    }

    /** Sinkronkan tanda vital (TTV) registrasi menjadi Observation. */
    @PostMapping("/observation/ttv-sync")
    public ApiResponse<SatuSehatClinicalSyncResponse> syncVitalSigns(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        satuSehatClinicalService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatObservationService.syncVitalSigns(registrationCode));
    }
}
