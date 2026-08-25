package com.vone.simrs.cashier;

import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.ward.WardPatientOptionResponse;
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
 * REST controller untuk screen SC0021 (TRANSAKSI KASIR / kasir.zul).
 */
@RestController
@RequestMapping("/api/cashier")
public class CashierController {

    private final CashierService cashierService;
    private final CashierPrintService cashierPrintService;

    public CashierController(CashierService cashierService, CashierPrintService cashierPrintService) {
        this.cashierService = cashierService;
        this.cashierPrintService = cashierPrintService;
    }

    @GetMapping("/masters")
    public ApiResponse<CashierMastersResponse> masters(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.getMasters(username));
    }

    @GetMapping("/patients/registered")
    public ApiResponse<List<WardPatientOptionResponse>> registeredPatients(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String birthDate,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.searchRegisteredPatients(
                mrCode, patientName, address, birthDate));
    }

    @GetMapping("/patients/{mrCode}")
    public ApiResponse<CashierPatientDetailResponse> patientDetail(
            @PathVariable String mrCode, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.getPatientDetail(mrCode));
    }

    @GetMapping("/notes")
    public ApiResponse<List<CashierNoteResponse>> notes(
            @RequestParam(required = false) Integer registrationId,
            @RequestParam(required = false) String noteNo,
            @RequestParam(required = false) String patientName,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.searchNotes(registrationId, noteNo, patientName));
    }

    @GetMapping("/notes/{noteId}/lines")
    public ApiResponse<List<CashierNoteLineResponse>> noteLines(
            @PathVariable Integer noteId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.getNoteLines(noteId));
    }

    @GetMapping("/bills")
    public ApiResponse<List<CashierBillSearchResponse>> bills(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String nameOnBill,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.searchBills(code, nameOnBill));
    }

    @GetMapping("/bills/{billId}")
    public ApiResponse<CashierBillDetailResponse> billDetail(
            @PathVariable Integer billId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.getBillDetail(billId));
    }

    @PostMapping("/pay")
    public ApiResponse<CashierPayResultResponse> pay(
            @RequestBody CashierPayRequest requestBody, HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.pay(requestBody, username));
    }

    @PostMapping("/deposit")
    public ApiResponse<CashierPayResultResponse> deposit(
            @RequestBody CashierDepositRequest requestBody,
            @RequestParam(defaultValue = "false") boolean retur,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cashierService.deposit(requestBody, retur, username));
    }

    @GetMapping("/bill/{code}/print")
    public org.springframework.http.ResponseEntity<byte[]> printBill(
            @PathVariable String code, HttpServletRequest request) throws Exception {
        ensureAuthenticated(request.getSession(false));
        byte[] pdf = cashierPrintService.generateKwitansiPdf(code);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=kwitansi.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String ensureAuthenticated(HttpSession session) {
        return cashierService.requireUsername(session);
    }
}
