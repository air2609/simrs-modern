package com.vone.simrs.satusehat;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifikasi manual Fase 5: sinkronisasi resep (MedicationRequest) dan
 * penyerahan obat (MedicationDispense).
 *
 * <pre>SATUSEHAT_TEST_MEDICATION=true mvn test -Dtest=SatuSehatMedicationSyncManualTest</pre>
 */
@SpringBootTest
class SatuSehatMedicationSyncManualTest {

    private static final String REG_CODE = "J-KAN-20260825-616";

    @Autowired
    private SatuSehatMedicationService satuSehatMedicationService;

    @Test
    void syncMedicationRequestsAndDispenses() {
        assumeTrue(Boolean.parseBoolean(System.getenv("SATUSEHAT_TEST_MEDICATION")),
                "Set SATUSEHAT_TEST_MEDICATION=true untuk menjalankan verifikasi ini");

        SatuSehatClinicalSyncResponse request =
                satuSehatMedicationService.syncMedicationRequests(REG_CODE);
        System.out.println("MEDICATION REQUEST => created=" + request.getCreated()
                + ", skipped=" + request.getSkipped() + ", msg=" + request.getMessage());
        assertTrue(request.getCreated() >= 1 || request.getSkipped() >= 1);

        SatuSehatClinicalSyncResponse dispense =
                satuSehatMedicationService.syncMedicationDispenses(REG_CODE);
        System.out.println("MEDICATION DISPENSE => created=" + dispense.getCreated()
                + ", skipped=" + dispense.getSkipped() + ", msg=" + dispense.getMessage());
        assertTrue(dispense.getCreated() >= 1 || dispense.getSkipped() >= 1);
    }
}
