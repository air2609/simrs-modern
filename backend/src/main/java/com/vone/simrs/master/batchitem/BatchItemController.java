package com.vone.simrs.master.batchitem;

import com.vone.simrs.auth.LegacyAuthService;
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
 * REST controller untuk screen SCM0055 (UPDATE BATCH ITEM).
 */
@RestController
@RequestMapping("/api/master/batchitem")
public class BatchItemController {

    private final BatchItemService batchItemService;
    private final LegacyAuthService legacyAuthService;

    public BatchItemController(BatchItemService batchItemService, LegacyAuthService legacyAuthService) {
        this.batchItemService = batchItemService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<BatchItemRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(batchItemService.getItems());
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody BatchItemSaveRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        batchItemService.save(body, username);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
