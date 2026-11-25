package com.vone.simrs.satusehat;

import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Mengelola access token OAuth2 SATUSEHAT.
 *
 * <p>Token di-cache di memori dan di-refresh otomatis sebelum kedaluwarsa.
 * (Versi sebelumnya memakai {@code @Cacheable} tanpa TTL, sehingga token
 * kedaluwarsa tetap dipakai.)
 */
@Service
public class SatuSehatAuthService {

    /** Refresh token bila sisa masa berlaku kurang dari nilai ini (detik). */
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60L;
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 3600L;

    private final SatuSehatProperties properties;
    private final RestTemplate restTemplate;

    private volatile String cachedToken;
    private volatile long cachedTokenExpiresAtMillis;

    public SatuSehatAuthService(SatuSehatProperties properties, RestTemplate satuSehatRestTemplate) {
        this.properties = properties;
        this.restTemplate = satuSehatRestTemplate;
    }

    /** Ambil access token yang valid (memakai cache bila masih berlaku). */
    public String getAccessToken() {
        String token = cachedToken;
        if (token != null && System.currentTimeMillis() < cachedTokenExpiresAtMillis) {
            return token;
        }
        synchronized (this) {
            if (cachedToken != null && System.currentTimeMillis() < cachedTokenExpiresAtMillis) {
                return cachedToken;
            }
            TokenResult result = requestToken();
            cachedToken = result.token;
            cachedTokenExpiresAtMillis = System.currentTimeMillis()
                    + Math.max(1L, result.expiresInSeconds - EXPIRY_SAFETY_MARGIN_SECONDS) * 1000L;
            return cachedToken;
        }
    }

    /** Paksa ambil token baru pada pemanggilan berikutnya (mis. setelah HTTP 401). */
    public void invalidateToken() {
        synchronized (this) {
            cachedToken = null;
            cachedTokenExpiresAtMillis = 0L;
        }
    }

    private TokenResult requestToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        // Endpoint SATUSEHAT (Apigee) mengharuskan grant_type sebagai query parameter.
        String url = withGrantType(properties.getAuthUrl());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(form, headers), Map.class);
            Map body = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && body != null && body.get("access_token") != null) {
                String token = String.valueOf(body.get("access_token"));
                long expiresIn = parseLong(body.get("expires_in"), DEFAULT_EXPIRES_IN_SECONDS);
                return new TokenResult(token, expiresIn);
            }
            throw new SatuSehatException(
                    "Gagal mendapatkan token SATUSEHAT (HTTP " + response.getStatusCodeValue() + ")");
        } catch (SatuSehatException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SatuSehatException(
                    "Gagal menghubungi server OAuth SATUSEHAT: " + exception.getMessage(), exception);
        }
    }

    private String withGrantType(String authUrl) {
        if (authUrl == null || authUrl.contains("grant_type=")) {
            return authUrl;
        }
        return authUrl + (authUrl.contains("?") ? "&" : "?") + "grant_type=client_credentials";
    }

    private long parseLong(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static final class TokenResult {
        private final String token;
        private final long expiresInSeconds;

        private TokenResult(String token, long expiresInSeconds) {
            this.token = token;
            this.expiresInSeconds = expiresInSeconds;
        }
    }
}
