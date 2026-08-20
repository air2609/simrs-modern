package com.vone.simrs.admin.user;

import com.vone.simrs.auth.LegacyPasswordEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserMaintenanceService {

    private static final String LIST_KOSONG = "LISTKOSONG";

    private final JdbcTemplate jdbcTemplate;
    private final LegacyPasswordEncoder legacyPasswordEncoder;

    public UserMaintenanceService(JdbcTemplate jdbcTemplate, LegacyPasswordEncoder legacyPasswordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyPasswordEncoder = legacyPasswordEncoder;
    }

    public UserMastersResponse getMasters() {
        List<GroupOptionResponse> groups = jdbcTemplate.query(
                "select n_group_id, v_group_name, coalesce(v_desc, v_group_name) as v_desc "
                        + "from ms_group order by v_group_name",
                (resultSet, rowNum) -> new GroupOptionResponse(
                        resultSet.getInt("n_group_id"),
                        resultSet.getString("v_group_name"),
                        resultSet.getString("v_desc")));

        List<BranchOptionResponse> branches = jdbcTemplate.query(
                "select n_branch_id, v_branch_name from ms_branch order by v_branch_name",
                (resultSet, rowNum) -> new BranchOptionResponse(
                        resultSet.getInt("n_branch_id"),
                        resultSet.getString("v_branch_name")));

        return new UserMastersResponse(groups, branches);
    }

    public List<UserRowResponse> getUsers(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toUpperCase(Locale.ROOT);

        return jdbcTemplate.query(
                "select usr.n_user_id, usr.v_user_name, usr.v_user_full_name, "
                        + "grup.n_group_id, grup.v_group_name, "
                        + "staff.n_staff_id, staff.v_staff_code, "
                        + "brc.n_branch_id, brc.v_branch_name "
                        + "from ms_user usr "
                        + "join ms_group grup on grup.n_group_id = usr.n_group_id "
                        + "join ms_staff staff on staff.n_staff_id = usr.n_staff_id "
                        + "left join ms_branch brc on brc.n_branch_id = usr.n_branch_id "
                        + "where (? = '' or upper(usr.v_user_name) like ? "
                        + "  or upper(grup.v_group_name) like ? "
                        + "  or upper(staff.v_staff_code) like ?) "
                        + "order by usr.v_user_name",
                (resultSet, rowNum) -> new UserRowResponse(
                        resultSet.getInt("n_user_id"),
                        resultSet.getString("v_user_name"),
                        resultSet.getString("v_user_full_name"),
                        resultSet.getInt("n_group_id"),
                        resultSet.getString("v_group_name"),
                        resultSet.getInt("n_staff_id"),
                        resultSet.getString("v_staff_code"),
                        getNullableInteger(resultSet, "n_branch_id"),
                        resultSet.getString("v_branch_name")),
                normalized,
                "%" + normalized + "%",
                "%" + normalized + "%",
                "%" + normalized + "%");
    }

    public List<StaffOptionResponse> searchStaff(String code, String name) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        String normalizedName = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);

        return jdbcTemplate.query(
                "select staff.n_staff_id, staff.v_staff_code, staff.v_staff_name "
                        + "from ms_staff staff "
                        + "where upper(staff.v_staff_code) like ? "
                        + "  and upper(staff.v_staff_name) like ? "
                        + "order by staff.v_staff_code",
                (resultSet, rowNum) -> new StaffOptionResponse(
                        resultSet.getInt("n_staff_id"),
                        resultSet.getString("v_staff_code"),
                        resultSet.getString("v_staff_name"),
                        ""),
                "%" + normalizedCode + "%",
                "%" + normalizedName + "%");
    }

    @Transactional
    public UserRowResponse createUser(UserSaveRequest request, String username) {
        String userName = normalizeRequired(request.getUserName(), "User ID wajib diisi.");
        String userFullName = normalizeRequired(request.getUserFullName(), "User name wajib diisi.");
        Integer groupId = requireGroup(request.getGroupId());
        Integer staffId = requireStaff(request.getStaffId());
        ensureUserNameAvailable(userName, null);

        Integer userId = nextSequenceValue("ms_user_n_user_id_seq");
        String actor = normalizeActor(username);

        jdbcTemplate.update(
                "insert into ms_user (n_user_id, n_group_id, n_staff_id, n_branch_id, "
                        + "v_user_name, v_password, v_user_full_name, v_whocreate, d_whncreate) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, now())",
                userId,
                groupId,
                staffId,
                request.getBranchId(),
                userName,
                legacyPasswordEncoder.encode(userName),
                userFullName,
                actor);

        return getUser(userId);
    }

    @Transactional
    public UserRowResponse updateUser(Integer userId, UserSaveRequest request, String username) {
        getUser(userId);
        String userName = normalizeRequired(request.getUserName(), "User ID wajib diisi.");
        String userFullName = normalizeRequired(request.getUserFullName(), "User name wajib diisi.");
        Integer groupId = requireGroup(request.getGroupId());
        Integer staffId = requireStaff(request.getStaffId());
        ensureUserNameAvailable(userName, userId);

        jdbcTemplate.update(
                "update ms_user set n_group_id = ?, n_staff_id = ?, n_branch_id = ?, "
                        + "v_user_name = ?, v_user_full_name = ?, v_whochange = ?, d_whnchange = now() "
                        + "where n_user_id = ?",
                groupId,
                staffId,
                request.getBranchId(),
                userName,
                userFullName,
                normalizeActor(username),
                userId);

        return getUser(userId);
    }

    @Transactional
    public void deleteUser(Integer userId) {
        getUser(userId);
        jdbcTemplate.update("delete from tb_user_privilege where n_user_id = ?", userId);
        jdbcTemplate.update("delete from ms_user where n_user_id = ?", userId);
    }

    public List<UserPrivilegeRowResponse> getPrivileges(String userName) {
        String normalized = userName == null ? "" : userName.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return new ArrayList<UserPrivilegeRowResponse>();
        }

        return jdbcTemplate.query(
                "select scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_desc, "
                        + "upr.v_access_type "
                        + "from tb_user_privilege upr "
                        + "join ms_user usr on usr.n_user_id = upr.n_user_id "
                        + "join ms_screen scr on scr.n_screen_id = upr.n_screen_id "
                        + "where upper(usr.v_user_name) = ? "
                        + "order by scr.v_screen_code",
                (resultSet, rowNum) -> new UserPrivilegeRowResponse(
                        resultSet.getInt("n_screen_id"),
                        resultSet.getString("v_screen_code"),
                        resultSet.getString("v_desc"),
                        resultSet.getString("v_access_type")),
                normalized);
    }

    @Transactional
    public UserPrivilegeRowResponse createPrivilege(UserPrivilegeSaveRequest request, String username) {
        String userName = normalizeRequired(request.getUserName(), "User ID wajib diisi.");
        Integer screenId = requireScreen(request.getScreenId());
        String accessType = normalizeAccessType(request.getAccessType());
        Integer userId = requireUser(userName);
        ensurePrivilegeAvailable(userId, screenId);

        jdbcTemplate.update(
                "insert into tb_user_privilege (n_user_id, n_screen_id, v_access_type, v_whocreate, d_whncreate) "
                        + "values (?, ?, ?, ?, now())",
                userId,
                screenId,
                accessType,
                normalizeActor(username));

        return getPrivilege(userId, screenId);
    }

    @Transactional
    public UserPrivilegeRowResponse updatePrivilege(UserPrivilegeSaveRequest request, String username) {
        String userName = normalizeRequired(request.getUserName(), "User ID wajib diisi.");
        Integer screenId = requireScreen(request.getScreenId());
        String accessType = normalizeAccessType(request.getAccessType());
        Integer userId = requireUser(userName);
        getPrivilege(userId, screenId);

        jdbcTemplate.update(
                "update tb_user_privilege set v_access_type = ?, v_whochange = ?, d_whnchange = now() "
                        + "where n_user_id = ? and n_screen_id = ?",
                accessType,
                normalizeActor(username),
                userId,
                screenId);

        return getPrivilege(userId, screenId);
    }

    @Transactional
    public void deletePrivilege(String userName, Integer screenId) {
        String normalized = normalizeRequired(userName, "User ID wajib diisi.");
        Integer userId = requireUser(normalized);
        requireScreen(screenId);
        getPrivilege(userId, screenId);

        jdbcTemplate.update(
                "delete from tb_user_privilege where n_user_id = ? and n_screen_id = ?",
                userId,
                screenId);
    }

    public List<ScreenOptionResponse> searchScreens(String code, String name) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        String normalizedName = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);

        return jdbcTemplate.query(
                "select scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_desc "
                        + "from ms_screen scr "
                        + "where upper(scr.v_screen_code) like ? "
                        + "  and upper(coalesce(scr.v_desc, scr.v_screen_code)) like ? "
                        + "order by scr.v_screen_code",
                (resultSet, rowNum) -> new ScreenOptionResponse(
                        resultSet.getInt("n_screen_id"),
                        resultSet.getString("v_screen_code"),
                        resultSet.getString("v_desc")),
                "%" + normalizedCode + "%",
                "%" + normalizedName + "%");
    }

    private UserRowResponse getUser(Integer userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select usr.n_user_id, usr.v_user_name, usr.v_user_full_name, "
                            + "grup.n_group_id, grup.v_group_name, "
                            + "staff.n_staff_id, staff.v_staff_code, "
                            + "brc.n_branch_id, brc.v_branch_name "
                            + "from ms_user usr "
                            + "join ms_group grup on grup.n_group_id = usr.n_group_id "
                            + "join ms_staff staff on staff.n_staff_id = usr.n_staff_id "
                            + "left join ms_branch brc on brc.n_branch_id = usr.n_branch_id "
                            + "where usr.n_user_id = ?",
                    (resultSet, rowNum) -> new UserRowResponse(
                            resultSet.getInt("n_user_id"),
                            resultSet.getString("v_user_name"),
                            resultSet.getString("v_user_full_name"),
                            resultSet.getInt("n_group_id"),
                            resultSet.getString("v_group_name"),
                            resultSet.getInt("n_staff_id"),
                            resultSet.getString("v_staff_code"),
                            getNullableInteger(resultSet, "n_branch_id"),
                            resultSet.getString("v_branch_name")),
                    userId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data user tidak ditemukan.");
        }
    }

    private UserPrivilegeRowResponse getPrivilege(Integer userId, Integer screenId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_desc, "
                            + "upr.v_access_type "
                            + "from tb_user_privilege upr "
                            + "join ms_screen scr on scr.n_screen_id = upr.n_screen_id "
                            + "where upr.n_user_id = ? and upr.n_screen_id = ?",
                    (resultSet, rowNum) -> new UserPrivilegeRowResponse(
                            resultSet.getInt("n_screen_id"),
                            resultSet.getString("v_screen_code"),
                            resultSet.getString("v_desc"),
                            resultSet.getString("v_access_type")),
                    userId,
                    screenId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data privilege tidak ditemukan.");
        }
    }

    private Integer requireGroup(Integer groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group wajib dipilih.");
        }
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from ms_group where n_group_id = ?", Number.class, groupId);
        if (total == null || total.intValue() == 0) {
            throw new IllegalArgumentException("Group tidak ditemukan.");
        }
        return groupId;
    }

    private Integer requireStaff(Integer staffId) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff wajib dipilih.");
        }
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from ms_staff where n_staff_id = ?", Number.class, staffId);
        if (total == null || total.intValue() == 0) {
            throw new IllegalArgumentException("Staff tidak ditemukan.");
        }
        return staffId;
    }

    private Integer requireScreen(Integer screenId) {
        if (screenId == null) {
            throw new IllegalArgumentException("Screen wajib dipilih.");
        }
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from ms_screen where n_screen_id = ?", Number.class, screenId);
        if (total == null || total.intValue() == 0) {
            throw new IllegalArgumentException("Screen tidak ditemukan.");
        }
        return screenId;
    }

    private Integer requireUser(String userName) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_user_id from ms_user where upper(v_user_name) = ?",
                    Integer.class,
                    userName);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("User tidak ditemukan.");
        }
    }

    private void ensureUserNameAvailable(String userName, Integer currentId) {
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from ms_user where upper(v_user_name) = ?"
                        + (currentId == null ? "" : " and n_user_id <> ?"),
                Number.class,
                currentId == null ? new Object[] { userName } : new Object[] { userName, currentId });
        if (total != null && total.intValue() > 0) {
            throw new IllegalArgumentException("User ID sudah dipakai.");
        }
    }

    private void ensurePrivilegeAvailable(Integer userId, Integer screenId) {
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from tb_user_privilege where n_user_id = ? and n_screen_id = ?",
                Number.class,
                userId,
                screenId);
        if (total != null && total.intValue() > 0) {
            throw new IllegalArgumentException("Privilege untuk screen ini sudah ada.");
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

    private String normalizeAccessType(String value) {
        String normalized = normalizeRequired(value, "Access type wajib diisi.");
        if (!"RW".equals(normalized) && !"R".equals(normalized) && !"SPV".equals(normalized)) {
            throw new IllegalArgumentException("Access type tidak valid.");
        }
        return normalized;
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName) throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }
}
