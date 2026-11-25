package com.vone.simrs.satusehat;

/**
 * Konstanta sistem/identifier FHIR SATUSEHAT.
 */
public final class SatuSehatConstants {

    private SatuSehatConstants() {
    }

    /** Identifier system untuk NIK. */
    public static final String NIK_SYSTEM = "https://fhir.kemkes.go.id/id/nik";

    /** Identifier system untuk IHS number (hasil dari SATUSEHAT). */
    public static final String IHS_SYSTEM = "https://fhir.kemkes.go.id/id/ihs-number";

    /** Identifier system untuk Kartu Keluarga. */
    public static final String KK_SYSTEM = "https://fhir.kemkes.go.id/id/kk";

    /** Extension administrative code pada Address. */
    public static final String ADMINISTRATIVE_CODE_EXTENSION =
            "https://fhir.kemkes.go.id/r4/StructureDefinition/administrativeCode";

    /** Code system untuk status perkawinan. */
    public static final String MARITAL_STATUS_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus";
}
