package com.vone.simrs.satusehat;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint sinkronisasi Encounter (kunjungan) ke SATUSEHAT.
 */
@RestController
@RequestMapping("/api/satusehat/encounter")
public class SatuSehatEncounterController {

    private final SatuSehatEncounterService satuSehatEncounterService;

    public SatuSehatEncounterController(SatuSehatEncounterService satuSehatEncounterService) {
        this.satuSehatEncounterService = satuSehatEncounterService;
    }

    /** Buat Encounter untuk registrasi (idempotent). */
    @PostMapping("/sync")
    public ApiResponse<SatuSehatEncounterSyncResponse> sync(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        satuSehatEncounterService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatEncounterService.syncEncounter(registrationCode));
    }

    /** Tandai Encounter selesai. */
    @PostMapping("/finish")
    public ApiResponse<SatuSehatEncounterSyncResponse> finish(
            @RequestParam String registrationCode,
            HttpServletRequest request) {
        satuSehatEncounterService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatEncounterService.finishEncounter(registrationCode));
    }
}
