package com.vone.simrs.master.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScreenMasterService {

    private final JdbcTemplate jdbcTemplate;

    public ScreenMasterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ScreenMasterMastersResponse getMasters() {
        List<SubsystemOptionResponse> subsystems = jdbcTemplate.query(
                "select n_subsystem_id, v_subsystem_code, coalesce(v_desc, v_subsystem_code) as v_desc "
                        + "from ms_subsystem order by v_subsystem_code",
                (resultSet, rowNum) -> new SubsystemOptionResponse(
                        resultSet.getInt("n_subsystem_id"),
                        resultSet.getString("v_subsystem_code"),
                        resultSet.getString("v_desc")));

        List<UnitOptionResponse> units = jdbcTemplate.query(
                "select n_unit_id, v_unit_code, v_unit_name from ms_unit order by v_unit_code",
                (resultSet, rowNum) -> new UnitOptionResponse(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name")));

        return new ScreenMasterMastersResponse(subsystems, units);
    }

    public List<ScreenRowResponse> getScreens(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toUpperCase(Locale.ROOT);

        List<ScreenRowResponse> rows = jdbcTemplate.query(
                "select scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_desc, "
                        + "sub.n_subsystem_id, sub.v_subsystem_code, coalesce(sub.v_desc, sub.v_subsystem_code) as v_subsystem_name "
                        + "from ms_screen scr "
                        + "join ms_subsystem sub on sub.n_subsystem_id = scr.n_subsystem_id "
                        + "where (? = '' or upper(scr.v_screen_code) like ? or upper(coalesce(scr.v_desc, scr.v_screen_code)) like ?) "
                        + "order by scr.v_screen_code",
                (resultSet, rowNum) -> new ScreenRowResponse(
                        resultSet.getInt("n_screen_id"),
                        resultSet.getString("v_screen_code"),
                        resultSet.getString("v_desc"),
                        resultSet.getInt("n_subsystem_id"),
                        resultSet.getString("v_subsystem_code"),
                        resultSet.getString("v_subsystem_name"),
                        new ArrayList<Integer>()),
                normalized,
                "%" + normalized + "%",
                "%" + normalized + "%");

        Map<Integer, List<Integer>> unitIdsByScreenId = new LinkedHashMap<Integer, List<Integer>>();
        jdbcTemplate.query(
                "select n_screen_id, n_unit_id from ms_screen_in_unit order by n_screen_id",
                (resultSet, rowNum) -> {
                    Integer screenId = resultSet.getInt("n_screen_id");
                    if (!unitIdsByScreenId.containsKey(screenId)) {
                        unitIdsByScreenId.put(screenId, new ArrayList<Integer>());
                    }
                    unitIdsByScreenId.get(screenId).add(resultSet.getInt("n_unit_id"));
                    return null;
                });

        List<ScreenRowResponse> result = new ArrayList<ScreenRowResponse>();
        for (ScreenRowResponse row : rows) {
            List<Integer> unitIds = unitIdsByScreenId.get(row.getScreenId());
            result.add(new ScreenRowResponse(
                    row.getScreenId(),
                    row.getScreenCode(),
                    row.getScreenName(),
                    row.getSubsystemId(),
                    row.getSubsystemCode(),
                    row.getSubsystemName(),
                    unitIds == null ? new ArrayList<Integer>() : unitIds));
        }
        return result;
    }

    @Transactional
    public ScreenRowResponse createScreen(ScreenMasterSaveRequest request, String username) {
        String code = normalizeRequired(request.getScreenCode(), "Kode screen wajib diisi.");
        String name = normalizeRequired(request.getScreenName(), "Nama screen wajib diisi.");
        Integer subsystemId = requireSubsystem(request.getSubsystemId());
        ensureCodeAvailable(code, null);

        Integer screenId = nextSequenceValue("ms_screen_n_screen_id_seq");
        jdbcTemplate.update(
                "insert into ms_screen (n_screen_id, n_subsystem_id, v_screen_code, v_desc, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, now())",
                screenId,
                subsystemId,
                code,
                name,
                normalizeActor(username));

        replaceScreenUnits(screenId, request.getUnitIds(), username);

        return getScreen(screenId);
    }

    @Transactional
    public ScreenRowResponse updateScreen(Integer screenId, ScreenMasterSaveRequest request, String username) {
        getScreen(screenId);
        String code = normalizeRequired(request.getScreenCode(), "Kode screen wajib diisi.");
        String name = normalizeRequired(request.getScreenName(), "Nama screen wajib diisi.");
        Integer subsystemId = requireSubsystem(request.getSubsystemId());
        ensureCodeAvailable(code, screenId);

        jdbcTemplate.update(
                "update ms_screen set n_subsystem_id = ?, v_screen_code = ?, v_desc = ?, v_who_change = ?, d_whn_change = now() "
                        + "where n_screen_id = ?",
                subsystemId,
                code,
                name,
                normalizeActor(username),
                screenId);

        replaceScreenUnits(screenId, request.getUnitIds(), username);

        return getScreen(screenId);
    }

    @Transactional
    public void deleteScreen(Integer screenId) {
        getScreen(screenId);
        jdbcTemplate.update("delete from ms_screen_in_unit where n_screen_id = ?", screenId);
        jdbcTemplate.update("delete from ms_screen where n_screen_id = ?", screenId);
    }

    private void replaceScreenUnits(Integer screenId, List<Integer> unitIds, String username) {
        jdbcTemplate.update("delete from ms_screen_in_unit where n_screen_id = ?", screenId);

        if (unitIds == null) {
            return;
        }

        String actor = normalizeActor(username);
        for (Integer unitId : unitIds) {
            if (unitId == null) {
                continue;
            }
            jdbcTemplate.update(
                    "insert into ms_screen_in_unit (n_screen_id, n_unit_id, v_who_create, d_whn_create) values (?, ?, ?, now())",
                    screenId,
                    unitId,
                    actor);
        }
    }

    private ScreenRowResponse getScreen(Integer screenId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_desc, "
                            + "sub.n_subsystem_id, sub.v_subsystem_code, coalesce(sub.v_desc, sub.v_subsystem_code) as v_subsystem_name "
                            + "from ms_screen scr "
                            + "join ms_subsystem sub on sub.n_subsystem_id = scr.n_subsystem_id "
                            + "where scr.n_screen_id = ?",
                    (resultSet, rowNum) -> new ScreenRowResponse(
                            resultSet.getInt("n_screen_id"),
                            resultSet.getString("v_screen_code"),
                            resultSet.getString("v_desc"),
                            resultSet.getInt("n_subsystem_id"),
                            resultSet.getString("v_subsystem_code"),
                            resultSet.getString("v_subsystem_name"),
                            new ArrayList<Integer>()),
                    screenId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data screen tidak ditemukan.");
        }
    }

    private Integer requireSubsystem(Integer subsystemId) {
        if (subsystemId == null) {
            throw new IllegalArgumentException("Subsystem wajib dipilih.");
        }
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from ms_subsystem where n_subsystem_id = ?",
                Number.class,
                subsystemId);
        if (total == null || total.intValue() == 0) {
            throw new IllegalArgumentException("Subsystem tidak ditemukan.");
        }
        return subsystemId;
    }

    private void ensureCodeAvailable(String code, Integer currentId) {
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from ms_screen where upper(v_screen_code) = ?"
                        + (currentId == null ? "" : " and n_screen_id <> ?"),
                Number.class,
                currentId == null ? new Object[] { code } : new Object[] { code, currentId });
        if (total != null && total.intValue() > 0) {
            throw new IllegalArgumentException("Kode screen sudah dipakai.");
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
