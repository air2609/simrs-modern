package com.vone.simrs.satusehat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifikasi manual Fase 3: sinkronisasi Condition + Procedure, lalu finish Encounter.
 *
 * <pre>SATUSEHAT_TEST_CLINICAL=true mvn test -Dtest=SatuSehatClinicalSyncManualTest</pre>
 */
@SpringBootTest
class SatuSehatClinicalSyncManualTest {

    private static final String REG_CODE = "J-KAN-20260825-616";

    @Autowired
    private SatuSehatClinicalService satuSehatClinicalService;

    @Autowired
    private SatuSehatEncounterService satuSehatEncounterService;

    @Test
    void syncClinicalAndFinishEncounter() {
        assumeTrue(Boolean.parseBoolean(System.getenv("SATUSEHAT_TEST_CLINICAL")),
                "Set SATUSEHAT_TEST_CLINICAL=true untuk menjalankan verifikasi ini");

        SatuSehatClinicalSyncResponse condition = satuSehatClinicalService.syncConditions(REG_CODE);
        System.out.println("CONDITION => created=" + condition.getCreated()
                + ", skipped=" + condition.getSkipped() + ", encounter=" + condition.getEncounterId());

        SatuSehatClinicalSyncResponse procedure = satuSehatClinicalService.syncProcedures(REG_CODE);
        System.out.println("PROCEDURE => created=" + procedure.getCreated()
                + ", skipped=" + procedure.getSkipped());

        SatuSehatEncounterSyncResponse finished = satuSehatEncounterService.finishEncounter(REG_CODE);
        System.out.println("ENCOUNTER FINISH => status=" + finished.getStatus()
                + ", action=" + finished.getAction());
        assertNotNull(finished.getEncounterId());
    }
}
