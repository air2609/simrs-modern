package com.vone.simrs.satusehat;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifikasi manual sinkronisasi tanda vital (TTV) menjadi Observation.
 *
 * <pre>SATUSEHAT_TEST_VITAL=true mvn test -Dtest=SatuSehatVitalSignSyncManualTest</pre>
 */
@SpringBootTest
class SatuSehatVitalSignSyncManualTest {

    private static final String REG_CODE = "J-KAN-20260825-616";

    @Autowired
    private SatuSehatObservationService satuSehatObservationService;

    @Test
    void syncVitalSigns() {
        assumeTrue(Boolean.parseBoolean(System.getenv("SATUSEHAT_TEST_VITAL")),
                "Set SATUSEHAT_TEST_VITAL=true untuk menjalankan verifikasi ini");

        SatuSehatClinicalSyncResponse response =
                satuSehatObservationService.syncVitalSigns(REG_CODE);
        System.out.println("VITAL => created=" + response.getCreated()
                + ", skipped=" + response.getSkipped() + ", msg=" + response.getMessage());

        assertTrue(response.getCreated() >= 1 || response.getSkipped() >= 1,
                "Seharusnya ada minimal 1 Observation tanda vital dibuat/di-skip");
    }
}
