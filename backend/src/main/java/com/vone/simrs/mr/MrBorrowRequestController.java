package com.vone.simrs.mr;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0175 (FORM PEMINJAMAN BERKAS REKAM MEDIS).
 */
@RestController
@RequestMapping("/api/mr/borrow-request")
public class MrBorrowRequestController {

    private final MrBorrowRequestService mrBorrowRequestService;

    public MrBorrowRequestController(MrBorrowRequestService mrBorrowRequestService) {
        this.mrBorrowRequestService = mrBorrowRequestService;
    }

    @GetMapping("/units")
    public ApiResponse<List<MrBorrowUnitResponse>> units(HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrBorrowRequestService.getUnits(username));
    }

    @GetMapping("/search")
    public ApiResponse<List<MrBorrowSearchResultResponse>> search(
            @RequestParam(required = false) String mrCode,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String nik,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrBorrowRequestService.searchPatients(mrCode, patientName, nik, birthDate, address));
    }

    @GetMapping("/lookup")
    public ApiResponse<MrBorrowSearchResultResponse> lookup(
            @RequestParam String code,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrBorrowRequestService.lookupByCode(code));
    }

    @PostMapping
    public ApiResponse<MrBorrowRequestResultResponse> requestBorrow(
            @RequestBody MrBorrowRequestBody body,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrBorrowRequestService.requestBorrow(body.getUnitId(), body.getMrCodes()));
    }

    private String ensureAuthenticated(HttpSession session) {
        return mrBorrowRequestService.requireUsername(session);
    }
}
