package com.vone.simrs.master.location;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master/locations")
public class LocationMasterController {

    private final LocationMasterService locationMasterService;
    private final LegacyAuthService legacyAuthService;

    public LocationMasterController(LocationMasterService locationMasterService, LegacyAuthService legacyAuthService) {
        this.locationMasterService = locationMasterService;
        this.legacyAuthService = legacyAuthService;
    }

    @GetMapping("/provinces")
    public ApiResponse<List<ProvinceRowResponse>> provinces(HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(locationMasterService.getProvinces());
    }

    @GetMapping("/provinces/{provinceId}/regencies")
    public ApiResponse<List<RegencyRowResponse>> regencies(
        @PathVariable Integer provinceId,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(locationMasterService.getRegencies(provinceId));
    }

    @GetMapping("/regencies/{regencyId}/districts")
    public ApiResponse<List<DistrictRowResponse>> districts(
        @PathVariable Integer regencyId,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(locationMasterService.getDistricts(regencyId));
    }

    @GetMapping("/districts/{districtId}/villages")
    public ApiResponse<List<VillageRowResponse>> villages(
        @PathVariable Integer districtId,
        HttpServletRequest request
    ) {
        ensureAuthenticated(request.getSession(false));
        return ApiResponse.ok(locationMasterService.getVillages(districtId));
    }

    @PostMapping("/provinces")
    public ApiResponse<ProvinceRowResponse> createProvince(
        @Valid @RequestBody LocationEntrySaveRequest requestBody,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(locationMasterService.createProvince(requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/provinces/{provinceId}")
    public ApiResponse<ProvinceRowResponse> updateProvince(
        @PathVariable Integer provinceId,
        @Valid @RequestBody LocationEntrySaveRequest requestBody,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(locationMasterService.updateProvince(provinceId, requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/provinces/{provinceId}")
    public ApiResponse<String> deleteProvince(@PathVariable Integer provinceId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        locationMasterService.deleteProvince(provinceId);
        return ApiResponse.ok("OK");
    }

    @PostMapping("/regencies")
    public ApiResponse<RegencyRowResponse> createRegency(
        @Valid @RequestBody LocationEntrySaveRequest requestBody,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(locationMasterService.createRegency(requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/regencies/{regencyId}")
    public ApiResponse<RegencyRowResponse> updateRegency(
        @PathVariable Integer regencyId,
        @Valid @RequestBody LocationEntrySaveRequest requestBody,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(locationMasterService.updateRegency(regencyId, requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/regencies/{regencyId}")
    public ApiResponse<String> deleteRegency(@PathVariable Integer regencyId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        locationMasterService.deleteRegency(regencyId);
        return ApiResponse.ok("OK");
    }

    @PostMapping("/districts")
    public ApiResponse<DistrictRowResponse> createDistrict(
        @Valid @RequestBody LocationEntrySaveRequest requestBody,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(locationMasterService.createDistrict(requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/districts/{districtId}")
    public ApiResponse<DistrictRowResponse> updateDistrict(
        @PathVariable Integer districtId,
        @Valid @RequestBody LocationEntrySaveRequest requestBody,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(locationMasterService.updateDistrict(districtId, requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/districts/{districtId}")
    public ApiResponse<String> deleteDistrict(@PathVariable Integer districtId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        locationMasterService.deleteDistrict(districtId);
        return ApiResponse.ok("OK");
    }

    @PostMapping("/villages")
    public ApiResponse<VillageRowResponse> createVillage(
        @Valid @RequestBody LocationEntrySaveRequest requestBody,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(locationMasterService.createVillage(requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @PutMapping("/villages/{villageId}")
    public ApiResponse<VillageRowResponse> updateVillage(
        @PathVariable Integer villageId,
        @Valid @RequestBody LocationEntrySaveRequest requestBody,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(locationMasterService.updateVillage(villageId, requestBody, ensureAuthenticated(request.getSession(false))));
    }

    @DeleteMapping("/villages/{villageId}")
    public ApiResponse<String> deleteVillage(@PathVariable Integer villageId, HttpServletRequest request) {
        ensureAuthenticated(request.getSession(false));
        locationMasterService.deleteVillage(villageId);
        return ApiResponse.ok("OK");
    }

    private String ensureAuthenticated(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }
}
