package com.vone.simrs.ward;

import com.vone.simrs.common.api.ApiResponse;
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
 * REST controller untuk screen SC0031 (TRANSAKSI BANGSAL / ward.zul).
 */
@RestController
@RequestMapping("/api/ward")
public class WardController {

    private final WardService wardService;
    private final WardNotePrintPdfService wardNotePrintPdfService;

    public WardController(WardService wardService,
            WardNotePrintPdfService wardNotePrintPdfService) {
        this.wardService = wardService;
        this.wardNotePrintPdfService = wardNotePrintPdfService;
    }

    @GetMapping("/masters")
    public ApiResponse<WardMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.getMasters(username));
    }

    @GetMapping("/patients/ranap")
    public ApiResponse<List<WardPatientOptionResponse>> ranapPatients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.searchRanapPatients(mrCode, patientName, address));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<WardPatientDetailResponse> patientDetail(
            @PathVariable String mrCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.getPatientDetail(mrCode));
    }

    @GetMapping("/doctors")
    public ApiResponse<List<WardDoctorOptionResponse>> doctors(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.searchDoctors(code, name));
    }

    @GetMapping("/treatments")
    public ApiResponse<List<WardTreatmentOptionResponse>> treatments(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String tariffClass,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.searchTreatments(code, name, tariffClass));
    }

    @GetMapping("/items")
    public ApiResponse<List<WardItemOptionResponse>> items(
            @RequestParam Integer warehouseId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.searchItems(warehouseId, code, name));
    }

    @GetMapping("/notes")
    public ApiResponse<List<WardNoteSummaryResponse>> notes(
            @RequestParam Integer unitId,
            @RequestParam(required = false) String noteNo,
            @RequestParam(required = false) String patientName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.searchNotes(unitId, noteNo, patientName));
    }

    @GetMapping("/notes/{noteId}")
    public ApiResponse<WardNoteDetailResponse> noteDetail(
            @PathVariable Integer noteId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.getNoteDetail(noteId));
    }

    @PostMapping("/notes")
    public ApiResponse<WardActionResultResponse> save(
            @RequestBody WardNoteSaveRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.saveNote(requestBody, username));
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<WardActionResultResponse> update(
            @PathVariable Integer noteId,
            @RequestBody WardNoteSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.updateNote(noteId, requestBody, username));
    }

    @PostMapping("/notes/{noteId}/validate")
    public ApiResponse<WardActionResultResponse> validate(
            @PathVariable Integer noteId, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.validateNote(noteId, username));
    }

    @PostMapping("/notes/{noteId}/cancel")
    public ApiResponse<WardActionResultResponse> cancel(
            @PathVariable Integer noteId,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.cancelNote(noteId, reason, username));
    }

    @GetMapping("/notes/{noteId}/print")
    public ResponseEntity<byte[]> print(
            @PathVariable Integer noteId, HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        byte[] pdf = wardNotePrintPdfService.generateNotePdf(noteId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=nota-bangsal.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/registrations/{regId}/doctor")
    public ApiResponse<WardActionResultResponse> setDoctor(
            @PathVariable Integer regId,
            @RequestParam(required = false) Integer doctorId,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.setMainDoctor(regId, doctorId, username));
    }

    @GetMapping("/patient-inventory")
    public ApiResponse<WardPatientInventoryResponse> patientInventory(
            @RequestParam Integer registrationId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.getPatientInventory(registrationId));
    }

    @PostMapping("/patient-inventory")
    public ApiResponse<WardActionResultResponse> savePatientInventory(
            @RequestBody WardPatientInventorySaveRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.savePatientInventory(requestBody, username));
    }

    @PostMapping("/patient-inventory/{piId}/delete")
    public ApiResponse<WardActionResultResponse> deletePatientInventory(
            @PathVariable Integer piId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.deletePatientInventory(piId));
    }

    @GetMapping("/history")
    public ApiResponse<WardHistoryResponse> history(
            @RequestParam String mrCode,
            @RequestParam(required = false) String mode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(wardService.getHistory(mrCode, mode));
    }

    private String ensureAuthenticated(HttpSession session) {
        return wardService.requireUsername(session);
    }
}
