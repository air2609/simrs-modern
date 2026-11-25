package com.vone.simrs.satusehat;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifikasi manual outbox/retry SATUSEHAT.
 *
 * <pre>SATUSEHAT_TEST_OUTBOX=true mvn test -Dtest=SatuSehatOutboxManualTest</pre>
 */
@SpringBootTest
class SatuSehatOutboxManualTest {

    @Autowired
    private SatuSehatOutboxService satuSehatOutboxService;

    @Test
    void enqueueAndProcess() {
        assumeTrue(Boolean.parseBoolean(System.getenv("SATUSEHAT_TEST_OUTBOX")),
                "Set SATUSEHAT_TEST_OUTBOX=true untuk menjalankan verifikasi ini");

        satuSehatOutboxService.enqueue(SatuSehatOutboxService.JOB_PATIENT, "14-02-70", "TEST");
        satuSehatOutboxService.enqueue(SatuSehatOutboxService.JOB_ENCOUNTER, "J-KAN-20260825-616", "TEST");

        SatuSehatOutboxProcessResult result = satuSehatOutboxService.processPending(10);
        System.out.println("OUTBOX => processed=" + result.getProcessed()
                + ", sent=" + result.getSent() + ", failed=" + result.getFailed());

        assertTrue(result.getProcessed() >= 2, "Minimal 2 job harus diproses");
        assertTrue(result.getSent() >= 2, "Minimal 2 job harus berhasil dikirim");
    }
}
