package com.vone.simrs.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LegacyAuthService {

    public static final String SESSION_USER_ATTRIBUTE = "USER_INFO";
    private static final String INVALID_LOGIN_MESSAGE = "login.invalid";
    private static final String SESSION_EXPIRED_MESSAGE = "Your session has been expired. You need to login again.";
    private static final String APOTIK_SCREEN_CODE = "SC0011";

    private final JdbcTemplate jdbcTemplate;
    private final LegacyPasswordEncoder legacyPasswordEncoder;

    public LegacyAuthService(JdbcTemplate jdbcTemplate, LegacyPasswordEncoder legacyPasswordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyPasswordEncoder = legacyPasswordEncoder;
    }

    public AuthenticatedUserResponse authenticate(String rawUsername, String password) {
        String username = normalizeUsername(rawUsername);
        LegacyUserRecord userRecord = findActiveUser(username);

        if (userRecord == null || !legacyPasswordEncoder.matches(password, userRecord.getPasswordHash())) {
            throw new InvalidCredentialsException(INVALID_LOGIN_MESSAGE);
        }

        return buildAuthenticatedUser(userRecord);
    }

    public AuthenticatedUserResponse loadAuthenticatedUser(String rawUsername) {
        LegacyUserRecord userRecord = findActiveUser(normalizeUsername(rawUsername));
        if (userRecord == null) {
            throw new AuthenticationRequiredException(SESSION_EXPIRED_MESSAGE);
        }

        return buildAuthenticatedUser(userRecord);
    }

    public String sessionExpiredMessage() {
        return SESSION_EXPIRED_MESSAGE;
    }

    public String requireUsername(HttpSession session) {
        if (session == null) {
            throw new AuthenticationRequiredException(SESSION_EXPIRED_MESSAGE);
        }

        Object username = session.getAttribute(SESSION_USER_ATTRIBUTE);
        if (!(username instanceof String)) {
            session.invalidate();
            throw new AuthenticationRequiredException(SESSION_EXPIRED_MESSAGE);
        }

        return (String) username;
    }

    private AuthenticatedUserResponse buildAuthenticatedUser(LegacyUserRecord userRecord) {
        List<LegacyPrivilegeRecord> privilegeRecords = findPrivileges(userRecord.getUsername());
        List<LegacyScreenUnitRecord> screenUnitRecords = findScreenUnits(userRecord.getUsername());

        Map<Integer, List<AuthUnitResponse>> unitsByScreenId = new LinkedHashMap<Integer, List<AuthUnitResponse>>();
        for (LegacyScreenUnitRecord screenUnitRecord : screenUnitRecords) {
            if (!unitsByScreenId.containsKey(screenUnitRecord.getScreenId())) {
                unitsByScreenId.put(screenUnitRecord.getScreenId(), new ArrayList<AuthUnitResponse>());
            }

            unitsByScreenId.get(screenUnitRecord.getScreenId()).add(new AuthUnitResponse(
                screenUnitRecord.getUnitId(),
                screenUnitRecord.getUnitCode(),
                screenUnitRecord.getUnitName(),
                screenUnitRecord.getWarehouseId()
            ));
        }

        Map<Integer, AuthModuleAccumulator> modulesById = new LinkedHashMap<Integer, AuthModuleAccumulator>();
        Map<Integer, Integer> screenPriorityById = new LinkedHashMap<Integer, Integer>();

        for (LegacyPrivilegeRecord privilegeRecord : privilegeRecords) {
            Integer screenId = privilegeRecord.getScreenId();
            Integer currentPriority = screenPriorityById.get(screenId);

            if (currentPriority != null && currentPriority.intValue() <= privilegeRecord.getPriority()) {
                continue;
            }

            screenPriorityById.put(screenId, privilegeRecord.getPriority());

            AuthModuleAccumulator moduleAccumulator = modulesById.get(privilegeRecord.getModuleId());
            if (moduleAccumulator == null) {
                moduleAccumulator = new AuthModuleAccumulator(
                    privilegeRecord.getModuleId(),
                    privilegeRecord.getModuleCode(),
                    privilegeRecord.getModuleName()
                );
                modulesById.put(privilegeRecord.getModuleId(), moduleAccumulator);
            }

            moduleAccumulator.screens.put(screenId, new AuthScreenResponse(
                screenId,
                privilegeRecord.getScreenCode(),
                privilegeRecord.getScreenName(),
                privilegeRecord.getAccessType(),
                unitsByScreenId.containsKey(screenId)
                    ? unitsByScreenId.get(screenId)
                    : new ArrayList<AuthUnitResponse>()
            ));
        }

        List<AuthModuleResponse> modules = new ArrayList<AuthModuleResponse>();
        for (AuthModuleAccumulator moduleAccumulator : modulesById.values()) {
            modules.add(new AuthModuleResponse(
                moduleAccumulator.moduleId,
                moduleAccumulator.moduleCode,
                moduleAccumulator.moduleName,
                new ArrayList<AuthScreenResponse>(moduleAccumulator.screens.values())
            ));
        }

        return new AuthenticatedUserResponse(
            userRecord.getUserId(),
            userRecord.getUsername(),
            userRecord.getFullName(),
            userRecord.getGroupId(),
            userRecord.getBranchId(),
            modules
        );
    }

    private LegacyUserRecord findActiveUser(String username) {
        try {
            return jdbcTemplate.queryForObject(
                "select usr.n_user_id, usr.v_user_name, usr.v_password, usr.v_user_full_name, usr.n_group_id, usr.n_branch_id "
                    + "from ms_user usr "
                    + "join ms_staff staff on staff.n_staff_id = usr.n_staff_id "
                    + "where upper(usr.v_user_name) = ? and staff.d_staff_fired_date is null",
                (resultSet, rowNum) -> new LegacyUserRecord(
                    resultSet.getInt("n_user_id"),
                    resultSet.getString("v_user_name"),
                    resultSet.getString("v_password"),
                    resultSet.getString("v_user_full_name"),
                    getNullableInteger(resultSet, "n_group_id"),
                    getNullableInteger(resultSet, "n_branch_id")
                ),
                username
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private List<LegacyPrivilegeRecord> findPrivileges(String username) {
        return jdbcTemplate.query(
            "select 0 as priority, sub.n_subsystem_id, sub.v_subsystem_code, coalesce(sub.v_desc, sub.v_subsystem_code) as v_subsystem_name, "
                + "scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_screen_name, upr.v_access_type "
                + "from ms_user usr "
                + "join tb_user_privilege upr on upr.n_user_id = usr.n_user_id "
                + "join ms_screen scr on scr.n_screen_id = upr.n_screen_id "
                + "join ms_subsystem sub on sub.n_subsystem_id = scr.n_subsystem_id "
                + "where upper(usr.v_user_name) = ? "
                + "union all "
                + "select 1 as priority, sub.n_subsystem_id, sub.v_subsystem_code, coalesce(sub.v_desc, sub.v_subsystem_code) as v_subsystem_name, "
                + "scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_screen_name, gpr.v_access_type "
                + "from ms_user usr "
                + "join tb_group_privilege gpr on gpr.n_group_id = usr.n_group_id "
                + "join ms_screen scr on scr.n_screen_id = gpr.n_screen_id "
                + "join ms_subsystem sub on sub.n_subsystem_id = scr.n_subsystem_id "
                + "where upper(usr.v_user_name) = ? "
                + "order by priority, n_subsystem_id, n_screen_id",
            (resultSet, rowNum) -> new LegacyPrivilegeRecord(
                resultSet.getInt("priority"),
                resultSet.getInt("n_subsystem_id"),
                resultSet.getString("v_subsystem_code"),
                resultSet.getString("v_subsystem_name"),
                resultSet.getInt("n_screen_id"),
                resultSet.getString("v_screen_code"),
                resultSet.getString("v_screen_name"),
                resultSet.getString("v_access_type")
            ),
            username,
            username
        );
    }

    private List<LegacyScreenUnitRecord> findScreenUnits(String username) {
        return jdbcTemplate.query(
            "select scr.n_screen_id as n_screen_id, unt.n_unit_id as n_unit_id, "
                + "scr.v_screen_code as v_screen_code, unt.v_unit_code as v_unit_code, unt.v_unit_name as v_unit_name, "
                + "unt.n_whouse_id as n_whouse_id "
                + "from ("
                + "  select distinct scrunit.n_screen_id as n_screen_id, stfunit.n_unit_id as n_unit_id "
                + "  from ms_screen_in_unit scrunit, "
                + "  ("
                + "    select stfunit.* "
                + "    from ms_staff_in_unit stfunit, ms_user usr "
                + "    where stfunit.n_staff_id = usr.n_staff_id "
                + "      and upper(usr.v_user_name) = ?"
                + "  ) stfunit "
                + "  where scrunit.n_unit_id = stfunit.n_unit_id"
                + "  union "
                + "  select distinct scr.n_screen_id as n_screen_id, stfunit.n_unit_id as n_unit_id "
                + "  from ms_screen scr "
                + "  join ms_user usr on upper(usr.v_user_name) = ? "
                + "  join ms_staff_in_unit stfunit on stfunit.n_staff_id = usr.n_staff_id "
                + "  join ms_unit unt on unt.n_unit_id = stfunit.n_unit_id "
                + "  where scr.v_screen_code = ? "
                + "    and unt.n_whouse_id is not null "
                + ") res, ms_screen scr, ms_unit unt "
                + "where res.n_screen_id = scr.n_screen_id "
                + "  and res.n_unit_id = unt.n_unit_id "
                + "order by scr.n_screen_id, unt.v_unit_name",
            (resultSet, rowNum) -> new LegacyScreenUnitRecord(
                resultSet.getInt("n_screen_id"),
                resultSet.getInt("n_unit_id"),
                resultSet.getString("v_screen_code"),
                resultSet.getString("v_unit_code"),
                resultSet.getString("v_unit_name"),
                getNullableInteger(resultSet, "n_whouse_id")
            ),
            username,
            username,
            APOTIK_SCREEN_CODE
        );
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase(Locale.ROOT);
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static class AuthModuleAccumulator {
        private final Integer moduleId;
        private final String moduleCode;
        private final String moduleName;
        private final Map<Integer, AuthScreenResponse> screens = new LinkedHashMap<Integer, AuthScreenResponse>();

        private AuthModuleAccumulator(Integer moduleId, String moduleCode, String moduleName) {
            this.moduleId = moduleId;
            this.moduleCode = moduleCode;
            this.moduleName = moduleName;
        }
    }
}
