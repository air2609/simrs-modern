package com.vone.simrs.mr;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0082 (DAFTAR PEMINJAMAN BERKAS REKAM MEDIS).
 */
@RestController
@RequestMapping("/api/mr/loan-list")
public class MrLoanListController {

    private final MrLoanListService mrLoanListService;

    public MrLoanListController(MrLoanListService mrLoanListService) {
        this.mrLoanListService = mrLoanListService;
    }

    @GetMapping
    public ApiResponse<List<MrLoanListItemResponse>> data(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrLoanListService.getLoanList());
    }

    @PostMapping("/status")
    public ApiResponse<MrLoanUpdateResultResponse> updateStatus(
            @RequestBody MrLoanUpdateRequestBody body,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(mrLoanListService.updateStatus(body.getMrCodes(), body.getAction()));
    }

    private String ensureAuthenticated(HttpSession session) {
        return mrLoanListService.requireUsername(session);
    }
}
