package com.vone.simrs.ward;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0004 (FORM TRANSAKSI BED / bedTransaction.zul).
 */
@RestController
@RequestMapping("/api/ward/bed-transaction")
public class BedTransactionController {

    private final BedTransactionService bedTransactionService;
    private final WardService wardService;

    public BedTransactionController(BedTransactionService bedTransactionService,
            WardService wardService) {
        this.bedTransactionService = bedTransactionService;
        this.wardService = wardService;
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

    @GetMapping("/bed-history")
    public ApiResponse<List<BedOccupancyResponse>> bedHistory(
            @RequestParam Integer registrationId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedTransactionService.getBedHistory(registrationId));
    }

    @PostMapping("/notes")
    public ApiResponse<BedNoteCreateResultResponse> createNote(
            @RequestBody BedNoteCreateRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(bedTransactionService.createNote(requestBody, username));
    }

    private String ensureAuthenticated(HttpSession session) {
        return bedTransactionService.requireUsername(session);
    }
}
