package com.vone.simrs.auth;

import com.vone.simrs.common.api.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LegacyAuthService legacyAuthService;
    private final int sessionTimeoutSeconds;

    public AuthController(
        LegacyAuthService legacyAuthService,
        @Value("${app.auth.session-timeout-seconds}") int sessionTimeoutSeconds
    ) {
        this.legacyAuthService = legacyAuthService;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticatedUserResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpServletRequest
    ) {
        AuthenticatedUserResponse authenticatedUser = legacyAuthService.authenticate(
            request.getUsername(),
            request.getPassword()
        );

        HttpSession existingSession = httpServletRequest.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }

        HttpSession session = httpServletRequest.getSession(true);
        session.setMaxInactiveInterval(sessionTimeoutSeconds);
        session.setAttribute(LegacyAuthService.SESSION_USER_ATTRIBUTE, authenticatedUser.getUsername());
        httpServletRequest.changeSessionId();

        return ApiResponse.ok(authenticatedUser);
    }

    @GetMapping("/me")
    public ApiResponse<AuthenticatedUserResponse> me(HttpServletRequest httpServletRequest) {
        HttpSession session = httpServletRequest.getSession(false);
        if (session == null) {
            throw new AuthenticationRequiredException(legacyAuthService.sessionExpiredMessage());
        }

        Object username = session.getAttribute(LegacyAuthService.SESSION_USER_ATTRIBUTE);
        if (!(username instanceof String)) {
            session.invalidate();
            throw new AuthenticationRequiredException(legacyAuthService.sessionExpiredMessage());
        }

        return ApiResponse.ok(legacyAuthService.loadAuthenticatedUser((String) username));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest httpServletRequest) {
        HttpSession session = httpServletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(ApiResponse.ok("Logged out"));
    }
}
