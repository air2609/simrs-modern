package com.vone.simrs.satusehat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Menyimpan jejak request/response SATUSEHAT ke tabel {@code ss_request_log}.
 *
 * <p>Bersifat best-effort: kegagalan menulis log tidak boleh menggagalkan
 * proses integrasi utama.
 */
@Service
public class SatuSehatAuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SatuSehatAuditService.class);

    private final JdbcTemplate jdbcTemplate;

    public SatuSehatAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void logSuccess(String resourceType, String localRef, String method, String url,
            String requestBody, String responseBody) {
        write(resourceType, localRef, method, url, requestBody, true, responseBody, null);
    }

    public void logFailure(String resourceType, String localRef, String method, String url,
            String requestBody, String errorMessage) {
        write(resourceType, localRef, method, url, requestBody, false, null, errorMessage);
    }

    private void write(String resourceType, String localRef, String method, String url,
            String requestBody, boolean success, String responseBody, String errorMessage) {
        try {
            jdbcTemplate.update(
                    "insert into ss_request_log (v_resource_type, v_local_ref, v_method, v_url, "
                            + "t_request_body, v_status, t_response_body, v_error_message) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                    resourceType, localRef, method, url, requestBody,
                    success ? "SUCCESS" : "FAILED", responseBody, errorMessage);
        } catch (RuntimeException exception) {
            LOGGER.warn("Gagal menulis audit log SATUSEHAT: {}", exception.getMessage());
        }
    }
}
