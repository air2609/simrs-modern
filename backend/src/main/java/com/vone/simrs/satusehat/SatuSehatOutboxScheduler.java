package com.vone.simrs.satusehat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Penjadwal yang memproses outbox SATUSEHAT secara berkala.
 *
 * <p>Dapat dimatikan lewat {@code satusehat.outbox.enabled=false}.
 */
@Component
public class SatuSehatOutboxScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SatuSehatOutboxScheduler.class);

    private final SatuSehatOutboxService outboxService;

    @Value("${satusehat.outbox.enabled:true}")
    private boolean enabled;

    @Value("${satusehat.outbox.batch-size:20}")
    private int batchSize;

    public SatuSehatOutboxScheduler(SatuSehatOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(
            fixedDelayString = "${satusehat.outbox.interval-ms:60000}",
            initialDelayString = "${satusehat.outbox.initial-delay-ms:30000}")
    public void process() {
        if (!enabled) {
            return;
        }
        try {
            SatuSehatOutboxProcessResult result = outboxService.processPending(batchSize);
            if (result.getProcessed() > 0) {
                LOGGER.info("Outbox SATUSEHAT: processed={}, sent={}, failed={}",
                        result.getProcessed(), result.getSent(), result.getFailed());
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Gagal memproses outbox SATUSEHAT: {}", exception.getMessage(), exception);
        }
    }
}
