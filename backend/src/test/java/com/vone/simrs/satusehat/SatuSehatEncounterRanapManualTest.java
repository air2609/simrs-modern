package com.vone.simrs.satusehat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifikasi manual Encounter rawat inap (class IMP).
 *
 * <pre>SATUSEHAT_TEST_RANAP=true mvn test -Dtest=SatuSehatEncounterRanapManualTest</pre>
 */
@SpringBootTest
class SatuSehatEncounterRanapManualTest {

    @Autowired
    private SatuSehatEncounterService satuSehatEncounterService;

    @Test
    void syncRanapEncounter() {
        assumeTrue(Boolean.parseBoolean(System.getenv("SATUSEHAT_TEST_RANAP")),
                "Set SATUSEHAT_TEST_RANAP=true untuk menjalankan verifikasi ini");

        SatuSehatEncounterSyncResponse response =
                satuSehatEncounterService.syncEncounter("I-TEST-001");
        System.out.println("RANAP ENCOUNTER => action=" + response.getAction()
                + ", id=" + response.getEncounterId() + ", msg=" + response.getMessage());

        assertNotNull(response.getEncounterId());
        assertTrue(response.getMessage().contains("IMP"),
                "Encounter rawat inap harus class IMP. Pesan: " + response.getMessage());
    }
}
