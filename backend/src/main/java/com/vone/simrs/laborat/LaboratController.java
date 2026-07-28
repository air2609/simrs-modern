package com.vone.simrs.laborat;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/laborat")
public class LaboratController {

    private final LaboratService laboratService;

    public LaboratController(LaboratService laboratService) {
        this.laboratService = laboratService;
    }

    @GetMapping("/masters")
    public ApiResponse<LaboratMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.getMasters(username));
    }

    @GetMapping("/patients/registered")
    public ApiResponse<List<LaboratRegisteredPatientResponse>> registeredPatients(
        @RequestParam(required = false) String mrCode,
        @RequestParam(required = false) String patientName,
        @RequestParam(required = false) String address,
        HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.searchRegisteredPatients(mrCode, patientName, address));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<LaboratPatientDetailResponse> patientDetail(
        @PathVariable String mrCode,
        HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.getPatientDetail(mrCode));
    }

    @GetMapping("/treatments")
    public ApiResponse<List<LaboratTreatmentOptionResponse>> treatments(
        @RequestParam(required = false) Integer unitId,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String tariffClass,
        HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.searchTreatments(unitId, code, name, tariffClass));
    }

    @GetMapping("/panels")
    public ApiResponse<List<LaboratPanelResponse>> panels(
        @RequestParam(required = false) String tariffClass,
        HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.getPanels(tariffClass));
    }

    @GetMapping("/items")
    public ApiResponse<List<LaboratItemOptionResponse>> items(
        @RequestParam(required = false) Integer unitId,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String tariffClass,
        HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.searchItems(unitId, code, name, tariffClass));
    }

    @GetMapping("/units/{unitId}/notes")
    public ApiResponse<List<LaboratNoteSummaryResponse>> notes(
        @PathVariable Integer unitId,
        @RequestParam(required = false) String noteNumber,
        @RequestParam(required = false) String patientName,
        HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.searchNotes(unitId, noteNumber, patientName));
    }

    @GetMapping("/notes/{noteId}")
    public ApiResponse<LaboratNoteDetailResponse> noteDetail(
        @PathVariable Integer noteId,
        HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.getNoteDetail(noteId));
    }

    @PostMapping("/notes")
    public ApiResponse<LaboratSaveResultResponse> save(
        @Valid @RequestBody LaboratSaveRequest requestBody,
        HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.createNote(requestBody, username));
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<LaboratSaveResultResponse> update(
        @PathVariable Integer noteId,
        @Valid @RequestBody LaboratSaveRequest requestBody,
        HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.updateNote(noteId, requestBody, username));
    }

    @PostMapping("/notes/{noteId}/validate")
    public ApiResponse<LaboratActionResultResponse> validate(
        @PathVariable Integer noteId,
        HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.validateNote(noteId, username));
    }

    @PostMapping("/notes/{noteId}/cancel")
    public ApiResponse<LaboratActionResultResponse> cancel(
        @PathVariable Integer noteId,
        @Valid @RequestBody LaboratCancelRequest requestBody,
        HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.cancelNote(noteId, requestBody, username));
    }

    // ===================== SC0043 — HASIL PEMERIKSAAN LAB =====================

    @GetMapping("/results")
    public ApiResponse<List<LaboratResultSummaryResponse>> searchResults(
            @RequestParam(required = false) String resultCode,
            @RequestParam(required = false) String patientName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.searchLabResults(resultCode, patientName));
    }

    @GetMapping("/results/{resultId}")
    public ApiResponse<LaboratResultDetailResponse> resultDetail(
            @PathVariable Integer resultId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.getLabResultDetail(resultId));
    }

    @GetMapping("/notes/{noteId}/result-items")
    public ApiResponse<List<LaboratResultItemResponse>> resultItems(
            @PathVariable Integer noteId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.getResultItemsForNote(noteId));
    }

    @PostMapping("/results")
    public ApiResponse<LaboratResultSaveResultResponse> createResult(
            @Valid @RequestBody LaboratResultSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.createLabResult(requestBody, username));
    }

    @PutMapping("/results/{resultId}")
    public ApiResponse<LaboratResultSaveResultResponse> updateResult(
            @PathVariable Integer resultId,
            @Valid @RequestBody LaboratResultSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(laboratService.updateLabResult(resultId, requestBody, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return laboratService.requireUsername(session);
    }
}
