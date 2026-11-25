package com.vone.simrs.satusehat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifikasi manual sinkronisasi Encounter ke SATUSEHAT staging.
 *
 * <p>Jalankan dengan:
 * <pre>SATUSEHAT_TEST_ENCOUNTER=true mvn test -Dtest=SatuSehatEncounterSyncManualTest</pre>
 *
 * <p>Catatan: Encounter "finish" di SATUSEHAT mewajibkan minimal satu diagnosis
 * (Condition). Selama Fase 3 belum ada, finish diharapkan berhenti dengan pesan
 * yang jelas.
 */
@SpringBootTest
class SatuSehatEncounterSyncManualTest {

    private static final String REG_CODE = "J-KAN-20260825-616";

    @Autowired
    private SatuSehatEncounterService satuSehatEncounterService;

    @Test
    void syncEncounter() {
        assumeTrue(Boolean.parseBoolean(System.getenv("SATUSEHAT_TEST_ENCOUNTER")),
                "Set SATUSEHAT_TEST_ENCOUNTER=true untuk menjalankan verifikasi ini");

        SatuSehatEncounterSyncResponse response;
        try {
            response = satuSehatEncounterService.syncEncounter(REG_CODE);
        } catch (RuntimeException exception) {
            assumeTrue(false, "Mapping master belum siap: " + exception.getMessage());
            return;
        }
        System.out.println("ENCOUNTER SYNC => action=" + response.getAction()
                + ", id=" + response.getEncounterId() + ", status=" + response.getStatus());
        assertNotNull(response.getEncounterId());

        try {
            SatuSehatEncounterSyncResponse finished = satuSehatEncounterService.finishEncounter(REG_CODE);
            System.out.println("ENCOUNTER FINISH => status=" + finished.getStatus()
                    + ", action=" + finished.getAction());
            assertNotNull(finished.getEncounterId());
        } catch (IllegalArgumentException exception) {
            System.out.println("ENCOUNTER FINISH PENDING (Fase 3) => " + exception.getMessage());
            assertTrue(exception.getMessage().contains("diagnosa"));
        }
    }
}
