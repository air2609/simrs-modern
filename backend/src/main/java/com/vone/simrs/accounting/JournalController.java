package com.vone.simrs.accounting;

import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounting/journals")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping
    public ApiResponse<List<JournalEntryResponse>> search(
            @RequestParam(required = false, defaultValue = "") String voucherNo,
            @RequestParam(required = false, defaultValue = "") String dateFrom,
            @RequestParam(required = false, defaultValue = "") String dateTo,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(journalService.searchJournals(voucherNo, dateFrom, dateTo));
    }

    private String ensureAuthenticated(HttpSession session) {
        return journalService.requireUsername(session);
    }
}
