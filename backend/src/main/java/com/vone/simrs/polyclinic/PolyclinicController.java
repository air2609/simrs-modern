package com.vone.simrs.polyclinic;

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
@RequestMapping("/api/polyclinic")
public class PolyclinicController {

    private final PolyclinicService polyclinicService;

    public PolyclinicController(PolyclinicService polyclinicService) {
        this.polyclinicService = polyclinicService;
    }

    @GetMapping("/masters")
    public ApiResponse<PolyclinicMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.getMasters(username));
    }

    @GetMapping("/patients/registered")
    public ApiResponse<List<PolyclinicRegisteredPatientResponse>> registeredPatients(
        @RequestParam Integer unitId,
        @RequestParam(required = false) String mrCode,
        @RequestParam(required = false) String patientName,
        @RequestParam(required = false) String address,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.searchRegisteredPatients(unitId, mrCode, patientName, address));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<PolyclinicPatientDetailResponse> patientDetail(
        @PathVariable String mrCode,
        @RequestParam Integer unitId,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.getRegisteredPatientDetail(unitId, mrCode));
    }

    @GetMapping("/units/{unitId}/doctors")
    public ApiResponse<List<PolyclinicDoctorResponse>> doctors(
        @PathVariable Integer unitId,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String name,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.searchDoctors(unitId, code, name));
    }

    @GetMapping("/units/{unitId}/treatments")
    public ApiResponse<List<PolyclinicTreatmentOptionResponse>> treatments(
        @PathVariable Integer unitId,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String tariffClass,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.searchTreatments(unitId, code, name, tariffClass));
    }

    @GetMapping("/units/{unitId}/items")
    public ApiResponse<List<PolyclinicItemOptionResponse>> items(
        @PathVariable Integer unitId,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String tariffClass,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.searchItems(unitId, code, name, tariffClass));
    }

    @GetMapping("/units/{unitId}/notes")
    public ApiResponse<List<PolyclinicNoteSummaryResponse>> notes(
        @PathVariable Integer unitId,
        @RequestParam(required = false) String noteNumber,
        @RequestParam(required = false) String patientName,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.searchNotes(unitId, noteNumber, patientName));
    }

    @GetMapping("/notes/{noteId}")
    public ApiResponse<PolyclinicNoteDetailResponse> noteDetail(
        @PathVariable Integer noteId,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.getNoteDetail(noteId));
    }

    @PostMapping("/notes")
    public ApiResponse<PolyclinicSaveResultResponse> save(
        @Valid @RequestBody PolyclinicSaveRequest requestBody,
        HttpServletRequest request
    ) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.createNote(requestBody, username));
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<PolyclinicSaveResultResponse> update(
        @PathVariable Integer noteId,
        @Valid @RequestBody PolyclinicSaveRequest requestBody,
        HttpServletRequest request
    ) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.updateNote(noteId, requestBody, username));
    }

    @PostMapping("/notes/{noteId}/validate")
    public ApiResponse<PolyclinicActionResultResponse> validate(
        @PathVariable Integer noteId,
        HttpServletRequest request
    ) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.validateNote(noteId, username));
    }

    @PostMapping("/notes/{noteId}/cancel")
    public ApiResponse<PolyclinicActionResultResponse> cancel(
        @PathVariable Integer noteId,
        @Valid @RequestBody PolyclinicCancelRequest requestBody,
        HttpServletRequest request
    ) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(polyclinicService.cancelNote(noteId, requestBody, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return polyclinicService.requireUsername(session);
    }
}
