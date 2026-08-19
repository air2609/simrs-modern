package com.vone.simrs.report;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SC0073 (BOR REPORT / borReport.zul).
 */
@RestController
@RequestMapping("/api/report/bor-report")
public class BorReportController {

    private final BorReportService borReportService;

    public BorReportController(BorReportService borReportService) {
        this.borReportService = borReportService;
    }

    @GetMapping("/report")
    public ApiResponse<BorReportResponse> report(
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(borReportService.getReport(from, to));
    }

    private String ensureAuthenticated(HttpSession session) {
        return borReportService.requireUsername(session);
    }
}
