package com.vone.simrs.apotik;

import com.vone.simrs.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/apotik")
public class ApotikController {

    private final ApotikService apotikService;

    public ApotikController(ApotikService apotikService) {
        this.apotikService = apotikService;
    }

    @GetMapping("/masters")
    public ApiResponse<ApotikMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.getMasters(username));
    }

    @GetMapping("/patients/registered")
    public ApiResponse<List<ApotikRegisteredPatientResponse>> registeredPatients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.searchRegisteredPatients(mrCode, patientName, address));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<ApotikPatientDetailResponse> patientDetail(
            @PathVariable String mrCode,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.getRegisteredPatientDetail(mrCode));
    }

    @GetMapping("/units/{unitId}/items")
    public ApiResponse<List<ApotikItemOptionResponse>> items(
            @PathVariable Integer unitId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String tariffClass,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.searchItems(unitId, code, name, tariffClass));
    }

    @GetMapping("/units/{unitId}/notes")
    public ApiResponse<List<ApotikNoteSummaryResponse>> notes(
            @PathVariable Integer unitId,
            @RequestParam(required = false) String noteNumber,
            @RequestParam(required = false) String patientName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.searchNotes(unitId, noteNumber, patientName));
    }

    @GetMapping("/notes/{noteId}")
    public ApiResponse<ApotikNoteDetailResponse> noteDetail(
            @PathVariable Integer noteId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.getNoteDetail(noteId));
    }

    @PostMapping("/notes")
    public ApiResponse<ApotikSaveResultResponse> save(
            @Valid @RequestBody ApotikSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.createNote(requestBody, username));
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<ApotikSaveResultResponse> update(
            @PathVariable Integer noteId,
            @Valid @RequestBody ApotikSaveRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.updateNote(noteId, requestBody, username));
    }

    @PostMapping("/notes/{noteId}/validate")
    public ApiResponse<ApotikActionResultResponse> validate(
            @PathVariable Integer noteId,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.validateNote(noteId, username));
    }

    @PostMapping("/notes/{noteId}/cancel")
    public ApiResponse<ApotikActionResultResponse> cancel(
            @PathVariable Integer noteId,
            @Valid @RequestBody ApotikCancelRequest requestBody,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.cancelNote(noteId, requestBody, username));
    }

    @GetMapping("/returns")
    public ApiResponse<List<ApotikReturnSummaryResponse>> returns(
            @RequestParam(required = false) String returnNumber,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.searchReturns(returnNumber, patientName, startDate, endDate));
    }

    @GetMapping("/returns/{returnId}")
    public ApiResponse<ApotikReturnDetailResponse> returnDetail(
            @PathVariable Integer returnId,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.getReturnDetail(returnId));
    }

    @PostMapping("/returns")
    public ApiResponse<ApotikActionResultResponse> createReturn(
            @RequestParam Integer noteId,
            @Valid @RequestBody List<ApotikLineItemRequest> lines,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.createReturn(noteId, lines, username));
    }

    @PostMapping("/returns/{returnId}/validate")
    public ApiResponse<ApotikActionResultResponse> validateReturn(
            @PathVariable Integer returnId,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.validateReturn(returnId, username));
    }

    @PostMapping("/returns/{returnId}/cancel")
    public ApiResponse<ApotikActionResultResponse> cancelReturn(
            @PathVariable Integer returnId,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(apotikService.cancelReturn(returnId, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return apotikService.requireUsername(session);
    }
}

