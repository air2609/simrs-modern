package com.vone.simrs.emergency;

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
 * REST controller untuk screen SC0061 (TRANSAKSI UGD / emergency.zul).
 */
@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    private final EmergencyService emergencyService;
    private final EmergencyNotePrintPdfService emergencyNotePrintPdfService;

    public EmergencyController(EmergencyService emergencyService,
            EmergencyNotePrintPdfService emergencyNotePrintPdfService) {
        this.emergencyService = emergencyService;
        this.emergencyNotePrintPdfService = emergencyNotePrintPdfService;
    }

    @GetMapping("/masters")
    public ApiResponse<EmergencyMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.getMasters());
    }

    @GetMapping("/patients")
    public ApiResponse<List<EmergencyPatientOptionResponse>> patients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String birthDate,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.searchPatients(mrCode, patientName, address, birthDate));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<EmergencyPatientDetailResponse> patientDetail(
            @PathVariable String mrCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.getPatientDetail(mrCode));
    }

    @GetMapping("/doctors")
    public ApiResponse<List<EmergencyDoctorOptionResponse>> doctors(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.searchDoctors(code, name));
    }

    @GetMapping("/treatments")
    public ApiResponse<List<EmergencyTreatmentOptionResponse>> treatments(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String tariffClass,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.searchTreatments(code, name, tariffClass));
    }

    @GetMapping("/items")
    public ApiResponse<List<EmergencyItemOptionResponse>> items(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.searchItems(code, name));
    }

    @GetMapping("/notes")
    public ApiResponse<List<EmergencyNoteSummaryResponse>> notes(
            @RequestParam(required = false) String noteNo,
            @RequestParam(required = false) String patientName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.searchNotes(noteNo, patientName));
    }

    @GetMapping("/notes/{noteId}")
    public ApiResponse<EmergencyNoteDetailResponse> noteDetail(
            @PathVariable Integer noteId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.getNoteDetail(noteId));
    }

    @PostMapping("/notes")
    public ApiResponse<EmergencyActionResultResponse> save(
            @RequestBody EmergencyNoteSaveRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.saveNote(requestBody, username));
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<EmergencyActionResultResponse> update(
            @PathVariable Integer noteId,
            @RequestBody EmergencyNoteSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.updateNote(noteId, requestBody, username));
    }

    @PostMapping("/notes/{noteId}/validate")
    public ApiResponse<EmergencyActionResultResponse> validate(
            @PathVariable Integer noteId, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.validateNote(noteId, username));
    }

    @PostMapping("/notes/{noteId}/cancel")
    public ApiResponse<EmergencyActionResultResponse> cancel(
            @PathVariable Integer noteId,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.cancelNote(noteId, reason, username));
    }

    @GetMapping("/notes/{noteId}/print")
    public ResponseEntity<byte[]> print(
            @PathVariable Integer noteId, HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        byte[] pdf = emergencyNotePrintPdfService.generateNotePdf(noteId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=nota-ugd.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/history")
    public ApiResponse<EmergencyHistoryResponse> history(
            @RequestParam String mrCode,
            @RequestParam(required = false) String mode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(emergencyService.getHistory(mrCode, mode));
    }

    private String ensureAuthenticated(HttpSession session) {
        return emergencyService.requireUsername(session);
    }
}
