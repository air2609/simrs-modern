package com.vone.simrs.master.itemsellingprice;

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
 * REST controller untuk screen SCM0041 (ITEM SELLING PRICE / MASTER HARGA
 * JUAL).
 */
@RestController
@RequestMapping("/api/master/item-selling-price")
public class ItemSellingPriceController {

    private final ItemSellingPriceService itemSellingPriceService;
    private final LegacyAuthService legacyAuthService;

    public ItemSellingPriceController(ItemSellingPriceService itemSellingPriceService,
            LegacyAuthService legacyAuthService) {
        this.itemSellingPriceService = itemSellingPriceService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping
    public ApiResponse<List<ItemSellingPriceRowResponse>> list(
            @RequestParam(required = false) String search, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemSellingPriceService.getSellingPrices(search));
    }

    @GetMapping("/masters")
    public ApiResponse<ItemSellingPriceMastersResponse> masters(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(itemSellingPriceService.getMasters());
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody ItemSellingPriceSaveRequest body,
            HttpServletRequest request) {
        String username = ensureAuthenticated(request.getSession(false));
        itemSellingPriceService.save(body, username);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam Integer id, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        itemSellingPriceService.delete(id);
        return ApiResponse.ok(null);
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
