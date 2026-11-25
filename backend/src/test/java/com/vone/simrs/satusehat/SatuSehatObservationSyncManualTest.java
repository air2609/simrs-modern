package com.vone.simrs.satusehat;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifikasi manual Fase 4: sinkronisasi hasil lab menjadi Observation.
 *
 * <pre>SATUSEHAT_TEST_OBSERVATION=true mvn test -Dtest=SatuSehatObservationSyncManualTest</pre>
 */
@SpringBootTest
class SatuSehatObservationSyncManualTest {

    private static final String REG_CODE = "J-KAN-20260825-616";

    @Autowired
    private SatuSehatObservationService satuSehatObservationService;

    @Test
    void syncObservations() {
        assumeTrue(Boolean.parseBoolean(System.getenv("SATUSEHAT_TEST_OBSERVATION")),
                "Set SATUSEHAT_TEST_OBSERVATION=true untuk menjalankan verifikasi ini");

        SatuSehatClinicalSyncResponse response =
                satuSehatObservationService.syncObservations(REG_CODE);
        System.out.println("OBSERVATION => created=" + response.getCreated()
                + ", skipped=" + response.getSkipped() + ", msg=" + response.getMessage());

        // created=1 saat pertama, atau skipped>=1 bila sudah pernah disinkronkan
        assertTrue(response.getCreated() >= 1 || response.getSkipped() >= 1,
                "Seharusnya ada minimal 1 Observation dibuat/di-skip");
    }
}
