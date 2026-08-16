package com.vone.simrs.master.cardtype;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller untuk screen SCM0048 (MASTER CARD TYPE / FORM TIPE KARTU
 * BANK).
 */
@RestController
@RequestMapping("/api/master/cardtype")
public class CardTypeController {

    private final CardTypeService cardTypeService;
    private final LegacyAuthService legacyAuthService;

    public CardTypeController(CardTypeService cardTypeService, LegacyAuthService legacyAuthService) {
        this.cardTypeService = cardTypeService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<CardTypeRowResponse>> list(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cardTypeService.getCardTypes());
    }

    @GetMapping("/masters")
    public ApiResponse<CardTypeMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cardTypeService.getMasters());
    }

    @GetMapping("/coa-search")
    public ApiResponse<List<CoaOptionResponse>> coaSearch(@RequestParam String keyword,
            HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(cardTypeService.searchCoa(keyword));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody CardTypeSaveRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        cardTypeService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        cardTypeService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
