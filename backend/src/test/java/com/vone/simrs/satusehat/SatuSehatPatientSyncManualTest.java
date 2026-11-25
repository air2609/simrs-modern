package com.vone.simrs.satusehat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifikasi manual sinkronisasi pasien ke SATUSEHAT staging.
 *
 * <p>Hanya berjalan bila env var {@code SATUSEHAT_TEST_SYNC=true} di-set,
 * agar tidak memanggil jaringan pada build biasa:
 * <pre>SATUSEHAT_TEST_SYNC=true mvn test -Dtest=SatuSehatPatientSyncManualTest</pre>
 */
@SpringBootTest
class SatuSehatPatientSyncManualTest {

    @Autowired
    private SatuSehatPatientService satuSehatPatientService;

    @Test
    void syncTestPatient() {
        assumeTrue(Boolean.parseBoolean(System.getenv("SATUSEHAT_TEST_SYNC")),
                "Set SATUSEHAT_TEST_SYNC=true untuk menjalankan verifikasi ini");

        SatuSehatPatientSyncResponse response = satuSehatPatientService.syncPatient("14-02-70");

        System.out.println("SYNC RESULT => action=" + response.getAction()
                + ", nik=" + response.getNik()
                + ", ihs=" + response.getIhsNumber()
                + ", mr=" + response.getMrCode());
        assertNotNull(response.getIhsNumber());
    }
}
