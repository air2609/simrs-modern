package com.vone.simrs.satusehat;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint pengelolaan mapping data master lokal ke ID/kode SATUSEHAT.
 */
@RestController
@RequestMapping("/api/satusehat/mapping")
public class SatuSehatMappingController {

    private final SatuSehatMappingService satuSehatMappingService;

    public SatuSehatMappingController(SatuSehatMappingService satuSehatMappingService) {
        this.satuSehatMappingService = satuSehatMappingService;
    }

    // ===================== Unit -> Location =====================

    @GetMapping("/units")
    public ApiResponse<List<SatuSehatMappingRow>> units(HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatMappingService.listUnits());
    }

    @PostMapping("/unit")
    public ApiResponse<String> setUnitLocation(
            @RequestParam int unitId,
            @RequestParam String locationId,
            HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        satuSehatMappingService.setUnitLocation(unitId, locationId);
        return ApiResponse.ok("Location ID unit tersimpan");
    }

    @GetMapping("/halls")
    public ApiResponse<List<SatuSehatMappingRow>> halls(HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatMappingService.listHalls());
    }

    @PostMapping("/hall")
    public ApiResponse<String> setHallLocation(
            @RequestParam int hallId,
            @RequestParam String locationId,
            HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        satuSehatMappingService.setHallLocation(hallId, locationId);
        return ApiResponse.ok("Location ID bangsal tersimpan");
    }

    // ===================== Dokter -> Practitioner =====================

    @GetMapping("/doctors")
    public ApiResponse<List<SatuSehatMappingRow>> doctors(HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatMappingService.listDoctors());
    }

    @PostMapping("/doctor")
    public ApiResponse<String> setDoctorPractitioner(
            @RequestParam int doctorId,
            @RequestParam String practitionerId,
            HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        satuSehatMappingService.setDoctorPractitioner(doctorId, practitionerId);
        return ApiResponse.ok("Practitioner ID dokter tersimpan");
    }

    @PostMapping("/staff-nik")
    public ApiResponse<String> setStaffNik(
            @RequestParam int staffId,
            @RequestParam String nik,
            HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        satuSehatMappingService.setStaffNik(staffId, nik);
        return ApiResponse.ok("NIK tenaga medis tersimpan");
    }

    // ===================== Item obat -> KFA =====================

    @GetMapping("/items")
    public ApiResponse<List<SatuSehatItemMappingRow>> items(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatMappingService.listItems(keyword));
    }

    @PostMapping("/item")
    public ApiResponse<String> setItemKfa(
            @RequestParam int itemId,
            @RequestParam(required = false) String kfaCode,
            @RequestParam(required = false) String kfaDisplay,
            @RequestParam(required = false) String ucumCode,
            HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        satuSehatMappingService.setItemKfa(itemId, kfaCode, kfaDisplay, ucumCode);
        return ApiResponse.ok("Mapping KFA item tersimpan");
    }

    // ===================== Lab -> LOINC =====================

    @GetMapping("/lab-analytes")
    public ApiResponse<List<SatuSehatLabMappingRow>> labAnalytes(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        return ApiResponse.ok(satuSehatMappingService.listLabAnalytes(keyword));
    }

    @PostMapping("/lab")
    public ApiResponse<String> setLabLoinc(
            @RequestParam int detailId,
            @RequestParam(required = false) String loincCode,
            @RequestParam(required = false) String loincDisplay,
            @RequestParam(required = false) String ucumCode,
            HttpServletRequest request) {
        satuSehatMappingService.requireUsername(request.getSession(false));
        satuSehatMappingService.setLabLoinc(detailId, loincCode, loincDisplay, ucumCode);
        return ApiResponse.ok("Mapping LOINC analyte tersimpan");
    }
}
