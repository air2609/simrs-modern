package com.vone.simrs.satusehat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfigurasi koneksi SATUSEHAT (Kemenkes).
 *
 * <p>Diambil dari blok {@code satusehat.*} pada {@code application.yml}.
 */
@Component
@ConfigurationProperties(prefix = "satusehat")
public class SatuSehatProperties {

    /** Endpoint OAuth2 access token. */
    private String authUrl;

    /** Base URL FHIR R4, tanpa trailing slash. */
    private String baseUrl;

    /** Client ID organisasi (OAuth2). */
    private String clientId;

    /** Client secret organisasi (OAuth2). */
    private String clientSecret;

    /** Organization ID RS di SATUSEHAT (dipakai pada Encounter dsb). */
    private String organizationId;

    /** Location ID default untuk Encounter rawat inap (bila bangsal belum dipetakan). */
    private String inpatientLocationId;

    private int connectTimeoutMs = 10000;

    private int readTimeoutMs = 30000;

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getInpatientLocationId() {
        return inpatientLocationId;
    }

    public void setInpatientLocationId(String inpatientLocationId) {
        this.inpatientLocationId = inpatientLocationId;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
