package com.vone.simrs.satusehat;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sinkronisasi data pasien (master pasien + No. RM) ke resource FHIR
 * {@code Patient} SATUSEHAT, lalu menyimpan IHS number hasilnya ke
 * {@code tb_medical_record.ihs_number}.
 */
@Service
public class SatuSehatPatientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SatuSehatPatientService.class);

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final SatuSehatClient satuSehatClient;

    public SatuSehatPatientService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService,
            SatuSehatClient satuSehatClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.satuSehatClient = satuSehatClient;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Sinkronkan pasien berdasarkan No. MR.
     *
     * <ol>
     *   <li>Bila sudah punya IHS, coba update (PUT).</li>
     *   <li>Bila belum, cari Patient berdasarkan NIK.</li>
     *   <li>Bila tidak ditemukan, buat Patient baru (POST).</li>
     * </ol>
     */
    public SatuSehatPatientSyncResponse syncPatient(String mrCode) {
        if (!StringUtils.hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        LocalPatient patient = loadPatient(mrCode.trim());
        if (patient == null) {
            throw new IllegalArgumentException("ADMISSION MR NOT FOUND");
        }
        String nik = normalizeNik(patient.nik);
        if (nik == null) {
            throw new IllegalArgumentException(
                    "Pasien belum memiliki NIK 16 digit yang valid. Lengkapi NIK terlebih dahulu.");
        }

        if (StringUtils.hasText(patient.ihsNumber)) {
            Patient toUpdate = toFhirPatient(patient, nik);
            toUpdate.setId(patient.ihsNumber);
            try {
                Patient updated = satuSehatClient.update(toUpdate, Patient.class);
                String ihs = extractIhsNumber(updated);
                if (!StringUtils.hasText(ihs)) {
                    ihs = patient.ihsNumber;
                }
                persistIhs(patient.mrId, ihs);
                return new SatuSehatPatientSyncResponse(patient.mrCode, nik, ihs, "UPDATED",
                        "Data pasien diperbarui di SATUSEHAT.");
            } catch (SatuSehatException exception) {
                LOGGER.warn("Gagal update Patient di SATUSEHAT (IHS {}): {}", patient.ihsNumber,
                        exception.getMessage());
            }
        }

        Bundle found = satuSehatClient.searchByIdentifier("Patient",
                SatuSehatConstants.NIK_SYSTEM, nik);
        Patient existing = firstPatient(found);
        if (existing != null) {
            String ihs = extractIhsNumber(existing);
            persistIhs(patient.mrId, ihs);
            return new SatuSehatPatientSyncResponse(patient.mrCode, nik, ihs, "FOUND",
                    "Pasien sudah terdaftar di SATUSEHAT; IHS number disimpan.");
        }

        if (!hasCompleteRegion(patient)) {
            throw new IllegalArgumentException("Kode wilayah pasien (provinsi/kota/kecamatan/kelurahan) "
                    + "belum lengkap. Lengkapi data alamat wilayah terlebih dahulu.");
        }

        Patient created = satuSehatClient.create(toFhirPatient(patient, nik), Patient.class);
        String ihs = extractIhsNumber(created);
        if (!StringUtils.hasText(ihs)) {
            throw new SatuSehatException("SATUSEHAT tidak mengembalikan IHS number.");
        }
        persistIhs(patient.mrId, ihs);
        return new SatuSehatPatientSyncResponse(patient.mrCode, nik, ihs, "CREATED",
                "Pasien berhasil didaftarkan ke SATUSEHAT.");
    }

    // ------------------------------------------------------------------
    // Pemetaan ke FHIR
    // ------------------------------------------------------------------

    private Patient toFhirPatient(LocalPatient local, String nik) {
        Patient patient = new Patient();

        Identifier nikIdentifier = patient.addIdentifier();
        nikIdentifier.setSystem(SatuSehatConstants.NIK_SYSTEM);
        nikIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        nikIdentifier.setValue(nik);

        HumanName name = patient.addName();
        name.setUse(HumanName.NameUse.OFFICIAL);
        name.setText(local.name);

        patient.setGender(mapGender(local.gender));
        if (local.birthDate != null) {
            patient.setBirthDate(Date.valueOf(local.birthDate));
        }
        patient.setMultipleBirth(new BooleanType(false));

        if (StringUtils.hasText(local.phone)) {
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setUse(ContactPoint.ContactPointUse.HOME)
                    .setValue(local.phone.trim());
        }

        String maritalCode = mapMaritalStatus(local.maritalStatus);
        if (maritalCode != null) {
            patient.setMaritalStatus(new CodeableConcept(new Coding(
                    SatuSehatConstants.MARITAL_STATUS_SYSTEM, maritalCode, local.maritalStatus)));
        }

        Address address = patient.addAddress();
        address.setUse(Address.AddressUse.HOME);
        address.setCountry("ID");
        if (StringUtils.hasText(local.address)) {
            address.addLine(local.address.trim());
        }
        if (StringUtils.hasText(local.cityName)) {
            address.setCity(local.cityName.trim());
        }

        Extension administrativeCode = new Extension(SatuSehatConstants.ADMINISTRATIVE_CODE_EXTENSION);
        addRegionComponent(administrativeCode, "province", local.provinceCode);
        addRegionComponent(administrativeCode, "city", local.cityCode);
        addRegionComponent(administrativeCode, "district", local.districtCode);
        addRegionComponent(administrativeCode, "village", local.villageCode);
        addRegionComponent(administrativeCode, "rt", local.rt);
        addRegionComponent(administrativeCode, "rw", local.rw);
        if (!administrativeCode.getExtension().isEmpty()) {
            address.addExtension(administrativeCode);
        }

        return patient;
    }

    private boolean hasCompleteRegion(LocalPatient patient) {
        return StringUtils.hasText(patient.provinceCode)
                && StringUtils.hasText(patient.cityCode)
                && StringUtils.hasText(patient.districtCode)
                && StringUtils.hasText(patient.villageCode);
    }

    private void addRegionComponent(Extension administrativeCode, String name, String value) {
        if (StringUtils.hasText(value)) {
            administrativeCode.addExtension(name, new CodeType(value.trim()));
        }
    }

    private Enumerations.AdministrativeGender mapGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            return Enumerations.AdministrativeGender.UNKNOWN;
        }
        String normalized = gender.trim().toUpperCase();
        if ("M".equals(normalized) || "L".equals(normalized) || "MALE".equals(normalized)) {
            return Enumerations.AdministrativeGender.MALE;
        }
        if ("F".equals(normalized) || "P".equals(normalized) || "FEMALE".equals(normalized)) {
            return Enumerations.AdministrativeGender.FEMALE;
        }
        return Enumerations.AdministrativeGender.UNKNOWN;
    }

    private String mapMaritalStatus(String maritalStatus) {
        if (!StringUtils.hasText(maritalStatus)) {
            return null;
        }
        String normalized = maritalStatus.trim().toLowerCase();
        if (normalized.startsWith("belum")) {
            return "S";
        }
        if (normalized.startsWith("menikah") || normalized.startsWith("kawin")) {
            return "M";
        }
        if (normalized.startsWith("duda") || normalized.startsWith("janda")
                || normalized.startsWith("cerai")) {
            return "W";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Data lokal
    // ------------------------------------------------------------------

    private LocalPatient loadPatient(String mrCode) {
        List<LocalPatient> rows = jdbcTemplate.query(
                "select mr.n_mr_id, mr.v_mr_code, mr.ihs_number, "
                        + "p.n_patient_id, p.v_patient_name, p.v_patient_gender, p.d_patient_dob, "
                        + "p.nik, p.v_patient_main_addr, p.v_patient_main_rt_rw, p.v_patient_main_ph_no, "
                        + "p.v_patient_marital_status, "
                        + "prov.v_province_id as province_raw, "
                        + "reg.v_regency_name as city_name, "
                        + "vil.v_village_code as village_raw "
                        + "from tb_medical_record mr "
                        + "join ms_patient p on p.n_patient_id = mr.n_patient_id "
                        + "left join ms_province prov on prov.n_province_id = p.n_province_id "
                        + "left join ms_regency reg on reg.n_regency_id = p.n_regency_id "
                        + "left join ms_village vil on vil.n_village_id = p.n_village_id "
                        + "where mr.v_mr_code = ?",
                (resultSet, rowNum) -> {
                    String rtRw = resultSet.getString("v_patient_main_rt_rw");
                    String villageRaw = resultSet.getString("village_raw");
                    Region region = Region.from(villageRaw, resultSet.getString("province_raw"));
                    java.sql.Date dob = resultSet.getDate("d_patient_dob");
                    return new LocalPatient(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getInt("n_patient_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getString("ihs_number"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_gender"),
                            dob == null ? null : dob.toLocalDate(),
                            resultSet.getString("nik"),
                            resultSet.getString("v_patient_main_addr"),
                            splitRt(rtRw),
                            splitRw(rtRw),
                            resultSet.getString("v_patient_main_ph_no"),
                            resultSet.getString("v_patient_marital_status"),
                            region.provinceCode,
                            region.cityCode,
                            region.districtCode,
                            region.villageCode,
                            resultSet.getString("city_name"));
                },
                mrCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void persistIhs(int medicalRecordId, String ihsNumber) {
        if (!StringUtils.hasText(ihsNumber)) {
            return;
        }
        jdbcTemplate.update("update tb_medical_record set ihs_number = ? where n_mr_id = ?",
                ihsNumber.trim(), medicalRecordId);
    }

    private Patient firstPatient(Bundle bundle) {
        if (bundle == null || bundle.getEntry() == null) {
            return null;
        }
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Patient) {
                return (Patient) entry.getResource();
            }
        }
        return null;
    }

    private String extractIhsNumber(Patient patient) {
        if (patient == null) {
            return null;
        }
        for (Identifier identifier : patient.getIdentifier()) {
            if (SatuSehatConstants.IHS_SYSTEM.equals(identifier.getSystem())
                    && StringUtils.hasText(identifier.getValue())) {
                return identifier.getValue();
            }
        }
        String id = patient.getIdElement() == null ? null : patient.getIdElement().getIdPart();
        return StringUtils.hasText(id) ? id : null;
    }

    // ------------------------------------------------------------------
    // Util
    // ------------------------------------------------------------------

    private String normalizeNik(String nik) {
        if (!StringUtils.hasText(nik)) {
            return null;
        }
        String digits = nik.replaceAll("[^0-9]", "");
        return digits.length() == 16 ? digits : null;
    }

    private String splitRt(String rtRw) {
        return splitRtRw(rtRw, true);
    }

    private String splitRw(String rtRw) {
        return splitRtRw(rtRw, false);
    }

    private String splitRtRw(String rtRw, boolean rt) {
        if (!StringUtils.hasText(rtRw)) {
            return null;
        }
        String digits = rtRw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (rt) {
            return digits.length() >= 5 ? digits.substring(0, 2) : digits;
        }
        return digits.length() >= 5 ? digits.substring(2) : null;
    }

    /** Turunan kode wilayah BPS tanpa titik dari kode kelurahan ber-titik. */
    private static final class Region {
        private final String provinceCode;
        private final String cityCode;
        private final String districtCode;
        private final String villageCode;

        private Region(String provinceCode, String cityCode, String districtCode, String villageCode) {
            this.provinceCode = provinceCode;
            this.cityCode = cityCode;
            this.districtCode = districtCode;
            this.villageCode = villageCode;
        }

        private static Region from(String villageRaw, String provinceRaw) {
            String digits = villageRaw == null ? null : villageRaw.replace(".", "").trim();
            if (digits != null && digits.length() >= 10) {
                return new Region(
                        digits.substring(0, 2),
                        digits.substring(0, 4),
                        digits.substring(0, 6),
                        digits);
            }
            String province = StringUtils.hasText(provinceRaw) ? provinceRaw.trim() : null;
            return new Region(province, null, null, null);
        }
    }

    private static final class LocalPatient {
        private final int mrId;
        private final int patientId;
        private final String mrCode;
        private final String ihsNumber;
        private final String name;
        private final String gender;
        private final LocalDate birthDate;
        private final String nik;
        private final String address;
        private final String rt;
        private final String rw;
        private final String phone;
        private final String maritalStatus;
        private final String provinceCode;
        private final String cityCode;
        private final String districtCode;
        private final String villageCode;
        private final String cityName;

        private LocalPatient(int mrId, int patientId, String mrCode, String ihsNumber, String name,
                String gender, LocalDate birthDate, String nik, String address, String rt, String rw,
                String phone, String maritalStatus, String provinceCode, String cityCode,
                String districtCode, String villageCode, String cityName) {
            this.mrId = mrId;
            this.patientId = patientId;
            this.mrCode = mrCode;
            this.ihsNumber = ihsNumber;
            this.name = name;
            this.gender = gender;
            this.birthDate = birthDate;
            this.nik = nik;
            this.address = address;
            this.rt = rt;
            this.rw = rw;
            this.phone = phone;
            this.maritalStatus = maritalStatus;
            this.provinceCode = provinceCode;
            this.cityCode = cityCode;
            this.districtCode = districtCode;
            this.villageCode = villageCode;
            this.cityName = cityName;
        }
    }
}
