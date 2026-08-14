package com.vone.simrs.admin.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupMasterService {

    private final JdbcTemplate jdbcTemplate;

    public GroupMasterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GroupRowResponse> getGroups(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toUpperCase(Locale.ROOT);

        return jdbcTemplate.query(
                "select n_group_id, v_group_name, coalesce(v_desc, v_group_name) as v_desc "
                        + "from ms_group "
                        + "where (? = '' or upper(v_group_name) like ? or upper(coalesce(v_desc, v_group_name)) like ?) "
                        + "order by v_group_name",
                (resultSet, rowNum) -> new GroupRowResponse(
                        resultSet.getInt("n_group_id"),
                        resultSet.getString("v_group_name"),
                        resultSet.getString("v_desc")),
                normalized,
                "%" + normalized + "%",
                "%" + normalized + "%");
    }

    @Transactional
    public GroupRowResponse createGroup(GroupSaveRequest request, String username) {
        String groupCode = normalizeRequired(request.getGroupCode(), "Group ID wajib diisi.");
        String groupName = normalizeRequired(request.getGroupName(), "Group name wajib diisi.");
        ensureCodeAvailable(groupCode, null);

        Integer groupId = nextSequenceValue("ms_group_n_group_id_seq");
        jdbcTemplate.update(
                "insert into ms_group (n_group_id, v_group_name, v_desc, v_whocreate, d_whncreate) "
                        + "values (?, ?, ?, ?, now())",
                groupId,
                groupCode,
                groupName,
                normalizeActor(username));

        return getGroup(groupId);
    }

    @Transactional
    public GroupRowResponse updateGroup(Integer groupId, GroupSaveRequest request, String username) {
        getGroup(groupId);
        String groupCode = normalizeRequired(request.getGroupCode(), "Group ID wajib diisi.");
        String groupName = normalizeRequired(request.getGroupName(), "Group name wajib diisi.");
        ensureCodeAvailable(groupCode, groupId);

        jdbcTemplate.update(
                "update ms_group set v_group_name = ?, v_desc = ?, v_whochange = ?, d_whnchg = now() "
                        + "where n_group_id = ?",
                groupCode,
                groupName,
                normalizeActor(username),
                groupId);

        return getGroup(groupId);
    }

    @Transactional
    public void deleteGroup(Integer groupId) {
        getGroup(groupId);
        jdbcTemplate.update("delete from tb_group_privilege where n_group_id = ?", groupId);
        jdbcTemplate.update("delete from ms_group where n_group_id = ?", groupId);
    }

    public List<GroupPrivilegeRowResponse> getPrivileges(String groupCode) {
        String normalized = groupCode == null ? "" : groupCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return new ArrayList<GroupPrivilegeRowResponse>();
        }

        return jdbcTemplate.query(
                "select scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_desc, "
                        + "gpr.v_access_type "
                        + "from tb_group_privilege gpr "
                        + "join ms_group grp on grp.n_group_id = gpr.n_group_id "
                        + "join ms_screen scr on scr.n_screen_id = gpr.n_screen_id "
                        + "where upper(grp.v_group_name) = ? "
                        + "order by scr.v_screen_code",
                (resultSet, rowNum) -> new GroupPrivilegeRowResponse(
                        resultSet.getInt("n_screen_id"),
                        resultSet.getString("v_screen_code"),
                        resultSet.getString("v_desc"),
                        resultSet.getString("v_access_type")),
                normalized);
    }

    @Transactional
    public GroupPrivilegeRowResponse createPrivilege(GroupPrivilegeSaveRequest request, String username) {
        String groupCode = normalizeRequired(request.getGroupCode(), "Group ID wajib diisi.");
        Integer screenId = requireScreen(request.getScreenId());
        String accessType = normalizeAccessType(request.getAccessType());
        Integer groupId = requireGroup(groupCode);
        ensurePrivilegeAvailable(groupId, screenId);

        jdbcTemplate.update(
                "insert into tb_group_privilege (n_group_id, n_screen_id, v_access_type, v_whocreate, d_whncreate) "
                        + "values (?, ?, ?, ?, now())",
                groupId,
                screenId,
                accessType,
                normalizeActor(username));

        return getPrivilege(groupId, screenId);
    }

    @Transactional
    public GroupPrivilegeRowResponse updatePrivilege(GroupPrivilegeSaveRequest request, String username) {
        String groupCode = normalizeRequired(request.getGroupCode(), "Group ID wajib diisi.");
        Integer screenId = requireScreen(request.getScreenId());
        String accessType = normalizeAccessType(request.getAccessType());
        Integer groupId = requireGroup(groupCode);
        getPrivilege(groupId, screenId);

        jdbcTemplate.update(
                "update tb_group_privilege set v_access_type = ?, v_whochange = ?, d_whnchange = now() "
                        + "where n_group_id = ? and n_screen_id = ?",
                accessType,
                normalizeActor(username),
                groupId,
                screenId);

        return getPrivilege(groupId, screenId);
    }

    @Transactional
    public void deletePrivilege(String groupCode, Integer screenId) {
        String normalized = normalizeRequired(groupCode, "Group ID wajib diisi.");
        Integer groupId = requireGroup(normalized);
        requireScreen(screenId);
        getPrivilege(groupId, screenId);

        jdbcTemplate.update(
                "delete from tb_group_privilege where n_group_id = ? and n_screen_id = ?",
                groupId,
                screenId);
    }

    public List<GroupScreenOptionResponse> searchScreens(String code, String name) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        String normalizedName = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);

        return jdbcTemplate.query(
                "select scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_desc "
                        + "from ms_screen scr "
                        + "where upper(scr.v_screen_code) like ? "
                        + "  and upper(coalesce(scr.v_desc, scr.v_screen_code)) like ? "
                        + "order by scr.v_screen_code",
                (resultSet, rowNum) -> new GroupScreenOptionResponse(
                        resultSet.getInt("n_screen_id"),
                        resultSet.getString("v_screen_code"),
                        resultSet.getString("v_desc")),
                "%" + normalizedCode + "%",
                "%" + normalizedName + "%");
    }

    private GroupRowResponse getGroup(Integer groupId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_group_id, v_group_name, coalesce(v_desc, v_group_name) as v_desc "
                            + "from ms_group where n_group_id = ?",
                    (resultSet, rowNum) -> new GroupRowResponse(
                            resultSet.getInt("n_group_id"),
                            resultSet.getString("v_group_name"),
                            resultSet.getString("v_desc")),
                    groupId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data group tidak ditemukan.");
        }
    }

    private GroupPrivilegeRowResponse getPrivilege(Integer groupId, Integer screenId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select scr.n_screen_id, scr.v_screen_code, coalesce(scr.v_desc, scr.v_screen_code) as v_desc, "
                            + "gpr.v_access_type "
                            + "from tb_group_privilege gpr "
                            + "join ms_screen scr on scr.n_screen_id = gpr.n_screen_id "
                            + "where gpr.n_group_id = ? and gpr.n_screen_id = ?",
                    (resultSet, rowNum) -> new GroupPrivilegeRowResponse(
                            resultSet.getInt("n_screen_id"),
                            resultSet.getString("v_screen_code"),
                            resultSet.getString("v_desc"),
                            resultSet.getString("v_access_type")),
                    groupId,
                    screenId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Data privilege tidak ditemukan.");
        }
    }

    private Integer requireGroup(String groupCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_group_id from ms_group where upper(v_group_name) = ?",
                    Integer.class,
                    groupCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Group tidak ditemukan.");
        }
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

    private void ensureCodeAvailable(String groupCode, Integer currentId) {
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from ms_group where upper(v_group_name) = ?"
                        + (currentId == null ? "" : " and n_group_id <> ?"),
                Number.class,
                currentId == null ? new Object[] { groupCode } : new Object[] { groupCode, currentId });
        if (total != null && total.intValue() > 0) {
            throw new IllegalArgumentException("Group ID sudah dipakai.");
        }
    }

    private void ensurePrivilegeAvailable(Integer groupId, Integer screenId) {
        Number total = jdbcTemplate.queryForObject(
                "select count(*) from tb_group_privilege where n_group_id = ? and n_screen_id = ?",
                Number.class,
                groupId,
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
}
