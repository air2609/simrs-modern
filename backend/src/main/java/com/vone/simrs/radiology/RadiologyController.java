package com.vone.simrs.radiology;

import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.ward.WardActionResultResponse;
import com.vone.simrs.ward.WardDoctorOptionResponse;
import com.vone.simrs.ward.WardHistoryResponse;
import com.vone.simrs.ward.WardItemOptionResponse;
import com.vone.simrs.ward.WardNoteDetailResponse;
import com.vone.simrs.ward.WardNoteSaveRequest;
import com.vone.simrs.ward.WardNoteSummaryResponse;
import com.vone.simrs.ward.WardPatientDetailResponse;
import com.vone.simrs.ward.WardPatientOptionResponse;
import com.vone.simrs.ward.WardTreatmentOptionResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0051 (TRANSAKSI RADIOLOGI / radiology.zul).
 */
@RestController
@RequestMapping("/api/radiology")
public class RadiologyController {

    private final RadiologyService radiologyService;
    private final RadiologyNotePrintPdfService radiologyNotePrintPdfService;

    public RadiologyController(RadiologyService radiologyService,
            RadiologyNotePrintPdfService radiologyNotePrintPdfService) {
        this.radiologyService = radiologyService;
        this.radiologyNotePrintPdfService = radiologyNotePrintPdfService;
    }

    @GetMapping("/masters")
    public ApiResponse<RadiologyMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.getMasters(username));
    }

    @GetMapping("/patients/registered")
    public ApiResponse<List<WardPatientOptionResponse>> registeredPatients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String birthDate,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.searchRegisteredPatients(
                mrCode, patientName, address, birthDate));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<WardPatientDetailResponse> patientDetail(
            @PathVariable String mrCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.getPatientDetail(mrCode));
    }

    @GetMapping("/doctors")
    public ApiResponse<List<WardDoctorOptionResponse>> doctors(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.searchDoctors(code, name));
    }

    @GetMapping("/radiographers")
    public ApiResponse<List<WardDoctorOptionResponse>> radiographers(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.searchRadiographers());
    }

    @GetMapping("/treatments")
    public ApiResponse<List<WardTreatmentOptionResponse>> treatments(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String tariffClass,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.searchTreatments(code, name, tariffClass));
    }

    @GetMapping("/items")
    public ApiResponse<List<WardItemOptionResponse>> items(
            @RequestParam Integer warehouseId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.searchItems(warehouseId, code, name));
    }

    @GetMapping("/notes")
    public ApiResponse<List<WardNoteSummaryResponse>> notes(
            @RequestParam Integer unitId,
            @RequestParam(required = false) String noteNo,
            @RequestParam(required = false) String patientName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.searchNotes(unitId, noteNo, patientName));
    }

    @GetMapping("/notes/{noteId}")
    public ApiResponse<WardNoteDetailResponse> noteDetail(
            @PathVariable Integer noteId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.getNoteDetail(noteId));
    }

    @PostMapping("/notes")
    public ApiResponse<WardActionResultResponse> save(
            @RequestBody WardNoteSaveRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.saveNote(requestBody, username));
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<WardActionResultResponse> update(
            @PathVariable Integer noteId,
            @RequestBody WardNoteSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.updateNote(noteId, requestBody, username));
    }

    @PostMapping("/notes/{noteId}/validate")
    public ApiResponse<WardActionResultResponse> validate(
            @PathVariable Integer noteId, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.validateNote(noteId, username));
    }

    @PostMapping("/notes/{noteId}/cancel")
    public ApiResponse<WardActionResultResponse> cancel(
            @PathVariable Integer noteId,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.cancelNote(noteId, reason, username));
    }

    @GetMapping("/notes/{noteId}/print")
    public ResponseEntity<byte[]> print(
            @PathVariable Integer noteId, HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        byte[] pdf = radiologyNotePrintPdfService.generateNotePdf(noteId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=nota-radiologi.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/history")
    public ApiResponse<WardHistoryResponse> history(
            @RequestParam String mrCode,
            @RequestParam(required = false) String mode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(radiologyService.getHistory(mrCode, mode));
    }

    private String ensureAuthenticated(HttpSession session) {
        return radiologyService.requireUsername(session);
    }
}
