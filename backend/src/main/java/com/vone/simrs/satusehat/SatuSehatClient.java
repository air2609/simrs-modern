package com.vone.simrs.satusehat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client FHIR R4 generik untuk SATUSEHAT.
 *
 * <p>Menangani penyisipan bearer token, retry sekali saat HTTP 401, serta
 * menerjemahkan {@code OperationOutcome} menjadi pesan error yang terbaca.
 * Operasi create/update/read dicatat ke {@code ss_request_log} (best-effort).
 */
@Component
public class SatuSehatClient {

    private final SatuSehatProperties properties;
    private final SatuSehatAuthService authService;
    private final SatuSehatAuditService auditService;
    private final RestTemplate restTemplate;
    private final IParser parser;

    public SatuSehatClient(SatuSehatProperties properties, SatuSehatAuthService authService,
            SatuSehatAuditService auditService, RestTemplate satuSehatRestTemplate) {
        this.properties = properties;
        this.authService = authService;
        this.auditService = auditService;
        this.restTemplate = satuSehatRestTemplate;
        this.parser = FhirContext.forR4().newJsonParser().setPrettyPrint(false);
    }

    /** POST membuat resource baru, mis. {@code Patient}. */
    public <T extends IBaseResource> T create(T resource, Class<T> type) {
        String body = parser.encodeResourceToString(resource);
        String url = resourceUrl("/" + resource.fhirType());
        try {
            String response = send(HttpMethod.POST, url, body, true);
            auditService.logSuccess(resource.fhirType(), null, "POST", url, body, response);
            return parse(type, response);
        } catch (SatuSehatException exception) {
            auditService.logFailure(resource.fhirType(), null, "POST", url, body, exception.getMessage());
            throw exception;
        }
    }

    /** PUT memperbarui resource berdasarkan {@code resource.id}. */
    public <T extends IBaseResource> T update(T resource, Class<T> type) {
        String id = resource.getIdElement() == null ? null : resource.getIdElement().getIdPart();
        if (id == null || id.isEmpty()) {
            throw new SatuSehatException(
                    "Resource " + resource.fhirType() + " tidak memiliki id untuk di-update.");
        }
        String body = parser.encodeResourceToString(resource);
        String url = resourceUrl("/" + resource.fhirType() + "/" + id);
        try {
            String response = send(HttpMethod.PUT, url, body, true);
            auditService.logSuccess(resource.fhirType(), id, "PUT", url, body, response);
            return parse(type, response);
        } catch (SatuSehatException exception) {
            auditService.logFailure(resource.fhirType(), id, "PUT", url, body, exception.getMessage());
            throw exception;
        }
    }

    /** GET satu resource berdasarkan id. */
    public <T extends IBaseResource> T read(String resourceType, String id, Class<T> type) {
        String url = resourceUrl("/" + resourceType + "/" + id);
        String response = send(HttpMethod.GET, url, null, true);
        return parse(type, response);
    }

    /** GET pencarian resource, mis. {@code Patient?identifier=...}. */
    public Bundle search(String resourceType, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder(resourceUrl("/" + resourceType));
        if (queryParams != null) {
            char separator = '?';
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                url.append(separator).append(entry.getKey()).append('=')
                        .append(encodeQueryValue(entry.getValue()));
                separator = '&';
            }
        }
        String response = send(HttpMethod.GET, url.toString(), null, true);
        return parse(Bundle.class, response);
    }

    public Bundle searchByIdentifier(String resourceType, String system, String value) {
        return search(resourceType,
                Collections.singletonMap("identifier", system + "|" + value));
    }

    /**
     * SATUSEHAT mengharapkan identifier dalam bentuk {@code system|value} dengan
     * karakter {@code |} di-encode ({@code %7C}), tetapi karakter pada system
     * (mis. {@code ://}) dibiarkan apa adanya.
     */
    private String encodeQueryValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("%", "%25")
                .replace("|", "%7C")
                .replace(" ", "%20")
                .replace("&", "%26")
                .replace("+", "%2B")
                .replace("#", "%23");
    }

    private String baseUrl() {
        String base = properties.getBaseUrl();
        if (base == null || base.isEmpty()) {
            throw new SatuSehatException("satusehat.base-url belum dikonfigurasi.");
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private String resourceUrl(String path) {
        return baseUrl() + path;
    }

    private String send(HttpMethod method, String url, String body, boolean retryOnUnauthorized) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(authService.getAccessToken());
        HttpEntity<String> entity = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);

        URI uri;
        try {
            // Gunakan overload URI agar RestTemplate tidak meng-encode ulang (%7C dsb).
            uri = new URI(url);
        } catch (URISyntaxException exception) {
            throw new SatuSehatException("URL SATUSEHAT tidak valid: " + url, exception);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, method, entity, String.class);
            return response.getBody();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED && retryOnUnauthorized) {
                authService.invalidateToken();
                return send(method, url, body, false);
            }
            throw new SatuSehatException("SATUSEHAT error (HTTP " + exception.getRawStatusCode() + "): "
                    + extractError(exception.getResponseBodyAsString()), exception);
        } catch (RestClientException exception) {
            throw new SatuSehatException("Gagal menghubungi SATUSEHAT: " + exception.getMessage(), exception);
        }
    }

    private <T extends IBaseResource> T parse(Class<T> type, String body) {
        if (body == null || body.isEmpty()) {
            throw new SatuSehatException("SATUSEHAT mengembalikan respons kosong.");
        }
        try {
            return parser.parseResource(type, body);
        } catch (RuntimeException exception) {
            throw new SatuSehatException("Gagal membaca respons SATUSEHAT: " + exception.getMessage(), exception);
        }
    }

    private String extractError(String body) {
        if (body == null || body.isEmpty()) {
            return "tanpa detail";
        }
        try {
            IBaseResource resource = parser.parseResource(body);
            if (resource instanceof OperationOutcome) {
                StringBuilder builder = new StringBuilder();
                for (OperationOutcome.OperationOutcomeIssueComponent issue
                        : ((OperationOutcome) resource).getIssue()) {
                    if (builder.length() > 0) {
                        builder.append("; ");
                    }
                    String detail;
                    if (issue.getDetails() != null && issue.getDetails().getText() != null) {
                        detail = issue.getDetails().getText();
                    } else if (issue.getCode() != null) {
                        detail = issue.getCode().toCode();
                    } else {
                        detail = "(tanpa detail)";
                    }
                    if (issue.getDiagnostics() != null && !issue.getDiagnostics().isEmpty()) {
                        detail = detail + ": " + issue.getDiagnostics();
                    }
                    builder.append(detail);
                }
                if (builder.length() > 0) {
                    return builder.toString();
                }
            }
        } catch (RuntimeException ignored) {
            // kembalikan body mentah bila bukan OperationOutcome
        }
        return body;
    }
}
