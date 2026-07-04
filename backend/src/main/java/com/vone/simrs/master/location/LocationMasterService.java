package com.vone.simrs.master.location;

import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationMasterService {

    private final JdbcTemplate jdbcTemplate;

    public LocationMasterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProvinceRowResponse> getProvinces() {
        return jdbcTemplate.query(
            "select n_province_id, v_province_id, v_province_name from ms_province order by v_province_id",
            (resultSet, rowNum) -> new ProvinceRowResponse(
                resultSet.getInt("n_province_id"),
                resultSet.getString("v_province_id"),
                resultSet.getString("v_province_name")
            )
        );
    }

    public List<RegencyRowResponse> getRegencies(Integer provinceId) {
        return jdbcTemplate.query(
            "select reg.n_regency_id, reg.v_regency_id, reg.v_regency_name, prov.n_province_id, prov.v_province_id, prov.v_province_name "
                + "from ms_regency reg "
                + "join ms_province prov on prov.n_province_id = reg.n_province_id "
                + "where reg.n_province_id = ? "
                + "order by reg.v_regency_id",
            (resultSet, rowNum) -> new RegencyRowResponse(
                resultSet.getInt("n_regency_id"),
                resultSet.getString("v_regency_id"),
                resultSet.getString("v_regency_name"),
                resultSet.getInt("n_province_id"),
                resultSet.getString("v_province_id"),
                resultSet.getString("v_province_name")
            ),
            provinceId
        );
    }

    public List<DistrictRowResponse> getDistricts(Integer regencyId) {
        return jdbcTemplate.query(
            "select dist.n_subdistrict_id, dist.v_sub_district_id, dist.v_sub_district_name, "
                + "reg.n_regency_id, reg.v_regency_id, reg.v_regency_name "
                + "from ms_sub_district dist "
                + "join ms_regency reg on reg.n_regency_id = dist.n_regency_id "
                + "where dist.n_regency_id = ? "
                + "order by dist.v_sub_district_id",
            (resultSet, rowNum) -> new DistrictRowResponse(
                resultSet.getInt("n_subdistrict_id"),
                resultSet.getString("v_sub_district_id"),
                resultSet.getString("v_sub_district_name"),
                resultSet.getInt("n_regency_id"),
                resultSet.getString("v_regency_id"),
                resultSet.getString("v_regency_name")
            ),
            regencyId
        );
    }

    public List<VillageRowResponse> getVillages(Integer districtId) {
        return jdbcTemplate.query(
            "select vil.n_village_id, vil.v_village_code, vil.v_village_name, "
                + "dist.n_subdistrict_id, dist.v_sub_district_id, dist.v_sub_district_name "
                + "from ms_village vil "
                + "join ms_sub_district dist on dist.n_subdistrict_id = vil.n_subdistrict_id "
                + "where vil.n_subdistrict_id = ? "
                + "order by vil.v_village_code",
            (resultSet, rowNum) -> new VillageRowResponse(
                resultSet.getInt("n_village_id"),
                resultSet.getString("v_village_code"),
                resultSet.getString("v_village_name"),
                resultSet.getInt("n_subdistrict_id"),
                resultSet.getString("v_sub_district_id"),
                resultSet.getString("v_sub_district_name")
            ),
            districtId
        );
    }

    @Transactional
    public ProvinceRowResponse createProvince(LocationEntrySaveRequest request, String username) {
        String code = normalizeRequired(request.getCode(), "Kode propinsi wajib diisi.");
        ensureCodeAvailable("ms_province", "v_province_id", code, null, "n_province_id");
        Integer provinceId = nextSequenceValue("ms_province_n_province_id_seq");
        jdbcTemplate.update(
            "insert into ms_province (n_province_id, v_province_id, v_province_name, v_who_create, d_whn_create) "
                + "values (?, ?, ?, ?, now())",
            provinceId,
            code,
            normalizeRequired(request.getName(), "Nama propinsi wajib diisi."),
            normalizeActor(username)
        );
        return getProvince(provinceId);
    }

    @Transactional
    public ProvinceRowResponse updateProvince(Integer provinceId, LocationEntrySaveRequest request, String username) {
        getProvince(provinceId);
        String code = normalizeRequired(request.getCode(), "Kode propinsi wajib diisi.");
        ensureCodeAvailable("ms_province", "v_province_id", code, provinceId, "n_province_id");
        jdbcTemplate.update(
            "update ms_province set v_province_id = ?, v_province_name = ?, v_who_change = ?, d_whn_change = now() "
                + "where n_province_id = ?",
            code,
            normalizeRequired(request.getName(), "Nama propinsi wajib diisi."),
            normalizeActor(username),
            provinceId
        );
        return getProvince(provinceId);
    }

    @Transactional
    public void deleteProvince(Integer provinceId) {
        getProvince(provinceId);
        ensureNoChildren("select count(*) from ms_regency where n_province_id = ?", provinceId, "Propinsi masih memiliki data kabupaten.");
        jdbcTemplate.update("delete from ms_province where n_province_id = ?", provinceId);
    }

    @Transactional
    public RegencyRowResponse createRegency(LocationEntrySaveRequest request, String username) {
        Integer provinceId = requireParentId(request.getParentId(), "Propinsi wajib dipilih.");
        getProvince(provinceId);
        String code = normalizeRequired(request.getCode(), "Kode kabupaten wajib diisi.");
        ensureCodeAvailable("ms_regency", "v_regency_id", code, null, "n_regency_id");
        Integer regencyId = nextSequenceValue("ms_regency_n_regency_id_seq");
        jdbcTemplate.update(
            "insert into ms_regency (n_regency_id, n_province_id, v_regency_id, v_regency_name, v_who_create, d_whn_create) "
                + "values (?, ?, ?, ?, ?, now())",
            regencyId,
            provinceId,
            code,
            normalizeRequired(request.getName(), "Nama kabupaten wajib diisi."),
            normalizeActor(username)
        );
        return getRegency(regencyId);
    }

    @Transactional
    public RegencyRowResponse updateRegency(Integer regencyId, LocationEntrySaveRequest request, String username) {
        getRegency(regencyId);
        Integer provinceId = requireParentId(request.getParentId(), "Propinsi wajib dipilih.");
        getProvince(provinceId);
        String code = normalizeRequired(request.getCode(), "Kode kabupaten wajib diisi.");
        ensureCodeAvailable("ms_regency", "v_regency_id", code, regencyId, "n_regency_id");
        jdbcTemplate.update(
            "update ms_regency set n_province_id = ?, v_regency_id = ?, v_regency_name = ?, v_who_change = ?, d_whn_change = now() "
                + "where n_regency_id = ?",
            provinceId,
            code,
            normalizeRequired(request.getName(), "Nama kabupaten wajib diisi."),
            normalizeActor(username),
            regencyId
        );
        return getRegency(regencyId);
    }

    @Transactional
    public void deleteRegency(Integer regencyId) {
        getRegency(regencyId);
        ensureNoChildren("select count(*) from ms_sub_district where n_regency_id = ?", regencyId, "Kabupaten masih memiliki data kecamatan.");
        jdbcTemplate.update("delete from ms_regency where n_regency_id = ?", regencyId);
    }

    @Transactional
    public DistrictRowResponse createDistrict(LocationEntrySaveRequest request, String username) {
        Integer regencyId = requireParentId(request.getParentId(), "Kabupaten wajib dipilih.");
        getRegency(regencyId);
        String code = normalizeRequired(request.getCode(), "Kode kecamatan wajib diisi.");
        ensureCodeAvailable("ms_sub_district", "v_sub_district_id", code, null, "n_subdistrict_id");
        Integer districtId = nextSequenceValue("ms_sub_district_n_subdistrict_id_seq");
        jdbcTemplate.update(
            "insert into ms_sub_district (n_subdistrict_id, n_regency_id, v_sub_district_id, v_sub_district_name, v_who_create, d_whn_create) "
                + "values (?, ?, ?, ?, ?, now())",
            districtId,
            regencyId,
            code,
            normalizeRequired(request.getName(), "Nama kecamatan wajib diisi."),
            normalizeActor(username)
        );
        return getDistrict(districtId);
    }

    @Transactional
    public DistrictRowResponse updateDistrict(Integer districtId, LocationEntrySaveRequest request, String username) {
        getDistrict(districtId);
        Integer regencyId = requireParentId(request.getParentId(), "Kabupaten wajib dipilih.");
        getRegency(regencyId);
        String code = normalizeRequired(request.getCode(), "Kode kecamatan wajib diisi.");
        ensureCodeAvailable("ms_sub_district", "v_sub_district_id", code, districtId, "n_subdistrict_id");
        jdbcTemplate.update(
            "update ms_sub_district set n_regency_id = ?, v_sub_district_id = ?, v_sub_district_name = ?, v_who_change = ?, d_whn_change = now() "
                + "where n_subdistrict_id = ?",
            regencyId,
            code,
            normalizeRequired(request.getName(), "Nama kecamatan wajib diisi."),
            normalizeActor(username),
            districtId
        );
        return getDistrict(districtId);
    }

    @Transactional
    public void deleteDistrict(Integer districtId) {
        getDistrict(districtId);
        ensureNoChildren("select count(*) from ms_village where n_subdistrict_id = ?", districtId, "Kecamatan masih memiliki data kelurahan.");
        jdbcTemplate.update("delete from ms_sub_district where n_subdistrict_id = ?", districtId);
    }

    @Transactional
    public VillageRowResponse createVillage(LocationEntrySaveRequest request, String username) {
        Integer districtId = requireParentId(request.getParentId(), "Kecamatan wajib dipilih.");
        getDistrict(districtId);
        String code = normalizeRequired(request.getCode(), "Kode kelurahan wajib diisi.");
        ensureCodeAvailable("ms_village", "v_village_code", code, null, "n_village_id");
        Integer villageId = nextSequenceValue("ms_village_n_village_id_seq");
        jdbcTemplate.update(
            "insert into ms_village (n_village_id, n_subdistrict_id, v_village_code, v_village_name, v_who_create, d_whn_create) "
                + "values (?, ?, ?, ?, ?, now())",
            villageId,
            districtId,
            code,
            normalizeRequired(request.getName(), "Nama kelurahan wajib diisi."),
            normalizeActor(username)
        );
        return getVillage(villageId);
    }

    @Transactional
    public VillageRowResponse updateVillage(Integer villageId, LocationEntrySaveRequest request, String username) {
        getVillage(villageId);
        Integer districtId = requireParentId(request.getParentId(), "Kecamatan wajib dipilih.");
        getDistrict(districtId);
        String code = normalizeRequired(request.getCode(), "Kode kelurahan wajib diisi.");
        ensureCodeAvailable("ms_village", "v_village_code", code, villageId, "n_village_id");
        jdbcTemplate.update(
            "update ms_village set n_subdistrict_id = ?, v_village_code = ?, v_village_name = ?, v_who_change = ?, d_whn_change = now() "
                + "where n_village_id = ?",
            districtId,
            code,
            normalizeRequired(request.getName(), "Nama kelurahan wajib diisi."),
            normalizeActor(username),
            villageId
        );
        return getVillage(villageId);
    }

    @Transactional
    public void deleteVillage(Integer villageId) {
        getVillage(villageId);
        jdbcTemplate.update("delete from ms_village where n_village_id = ?", villageId);
    }

    private ProvinceRowResponse getProvince(Integer provinceId) {
        try {
            return jdbcTemplate.queryForObject(
                "select n_province_id, v_province_id, v_province_name from ms_province where n_province_id = ?",
                (resultSet, rowNum) -> new ProvinceRowResponse(
                    resultSet.getInt("n_province_id"),
                    resultSet.getString("v_province_id"),
                    resultSet.getString("v_province_name")
                ),
                provinceId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data propinsi tidak ditemukan.");
        }
    }

    private RegencyRowResponse getRegency(Integer regencyId) {
        try {
            return jdbcTemplate.queryForObject(
                "select reg.n_regency_id, reg.v_regency_id, reg.v_regency_name, prov.n_province_id, prov.v_province_id, prov.v_province_name "
                    + "from ms_regency reg "
                    + "join ms_province prov on prov.n_province_id = reg.n_province_id "
                    + "where reg.n_regency_id = ?",
                (resultSet, rowNum) -> new RegencyRowResponse(
                    resultSet.getInt("n_regency_id"),
                    resultSet.getString("v_regency_id"),
                    resultSet.getString("v_regency_name"),
                    resultSet.getInt("n_province_id"),
                    resultSet.getString("v_province_id"),
                    resultSet.getString("v_province_name")
                ),
                regencyId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data kabupaten tidak ditemukan.");
        }
    }

    private DistrictRowResponse getDistrict(Integer districtId) {
        try {
            return jdbcTemplate.queryForObject(
                "select dist.n_subdistrict_id, dist.v_sub_district_id, dist.v_sub_district_name, "
                    + "reg.n_regency_id, reg.v_regency_id, reg.v_regency_name "
                    + "from ms_sub_district dist "
                    + "join ms_regency reg on reg.n_regency_id = dist.n_regency_id "
                    + "where dist.n_subdistrict_id = ?",
                (resultSet, rowNum) -> new DistrictRowResponse(
                    resultSet.getInt("n_subdistrict_id"),
                    resultSet.getString("v_sub_district_id"),
                    resultSet.getString("v_sub_district_name"),
                    resultSet.getInt("n_regency_id"),
                    resultSet.getString("v_regency_id"),
                    resultSet.getString("v_regency_name")
                ),
                districtId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data kecamatan tidak ditemukan.");
        }
    }

    private VillageRowResponse getVillage(Integer villageId) {
        try {
            return jdbcTemplate.queryForObject(
                "select vil.n_village_id, vil.v_village_code, vil.v_village_name, "
                    + "dist.n_subdistrict_id, dist.v_sub_district_id, dist.v_sub_district_name "
                    + "from ms_village vil "
                    + "join ms_sub_district dist on dist.n_subdistrict_id = vil.n_subdistrict_id "
                    + "where vil.n_village_id = ?",
                (resultSet, rowNum) -> new VillageRowResponse(
                    resultSet.getInt("n_village_id"),
                    resultSet.getString("v_village_code"),
                    resultSet.getString("v_village_name"),
                    resultSet.getInt("n_subdistrict_id"),
                    resultSet.getString("v_sub_district_id"),
                    resultSet.getString("v_sub_district_name")
                ),
                villageId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data kelurahan tidak ditemukan.");
        }
    }

    private Integer requireParentId(Integer parentId, String message) {
        if (parentId == null) {
            throw new IllegalArgumentException(message);
        }
        return parentId;
    }

    private void ensureCodeAvailable(String tableName, String codeColumn, String code, Integer currentId, String idColumn) {
        Number total = jdbcTemplate.queryForObject(
            "select count(*) from " + tableName + " where upper(" + codeColumn + ") = ?"
                + (currentId == null ? "" : " and " + idColumn + " <> ?"),
            Number.class,
            currentId == null ? new Object[] {code} : new Object[] {code, currentId}
        );
        if (total != null && total.intValue() > 0) {
            throw new IllegalArgumentException("Kode sudah dipakai.");
        }
    }

    private void ensureNoChildren(String sql, Integer id, String message) {
        Number total = jdbcTemplate.queryForObject(sql, Number.class, id);
        if (total != null && total.intValue() > 0) {
            throw new IllegalStateException(message);
        }
    }

    private Integer nextSequenceValue(String sequenceName) {
        Number number = jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Number.class);
        return number == null ? null : number.intValue();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
