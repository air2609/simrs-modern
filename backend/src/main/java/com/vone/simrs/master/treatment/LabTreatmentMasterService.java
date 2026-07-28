package com.vone.simrs.master.treatment;

import com.vone.simrs.auth.LegacyAuthService;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabTreatmentMasterService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public LabTreatmentMasterService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // Treatment Groups (categories)
    public List<LabTreatmentGroupResponse> getLabGroups() {
        return jdbcTemplate.query(
            "select g.n_tgroup_id, g.v_tgroup_code, g.v_tgroup_name "
                + "from ms_treatment_group g "
                + "where upper(g.v_tgroup_name) not in ('PAKET', 'NON_PAKET') "
                + "and g.v_tgroup_code <> '001' "
                + "order by g.v_tgroup_code",
            (rs, rowNum) -> new LabTreatmentGroupResponse(
                rs.getInt("n_tgroup_id"),
                rs.getString("v_tgroup_code"),
                rs.getString("v_tgroup_name")
            )
        );
    }

    // Treatments (items in a group)
    public List<LabTreatmentResponse> getTreatmentsByGroup(Integer groupId) {
        return jdbcTemplate.query(
            "select t.n_treatment_id, t.v_treatment_code, t.v_treatment_name, t.n_tgroup_id "
                + "from ms_treatment t "
                + "where t.n_tgroup_id = ? "
                + "order by t.v_treatment_code",
            (rs, rowNum) -> new LabTreatmentResponse(
                rs.getInt("n_treatment_id"),
                rs.getString("v_treatment_code"),
                rs.getString("v_treatment_name"),
                rs.getInt("n_tgroup_id")
            ),
            groupId
        );
    }

    public LabTreatmentResponse getTreatmentById(Integer treatmentId) {
        try {
            return jdbcTemplate.queryForObject(
                "select t.n_treatment_id, t.v_treatment_code, t.v_treatment_name, t.n_tgroup_id "
                    + "from ms_treatment t where t.n_treatment_id = ?",
                (rs, rowNum) -> new LabTreatmentResponse(
                    rs.getInt("n_treatment_id"),
                    rs.getString("v_treatment_code"),
                    rs.getString("v_treatment_name"),
                    rs.getInt("n_tgroup_id")
                ),
                treatmentId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Treatment tidak ditemukan.");
        }
    }

    // Lab Treatment Details (CRUD)
    public List<LabTreatmentDetailResponse> getDetailsByTreatment(Integer treatmentId) {
        return jdbcTemplate.query(
            "select d.n_lab_detil_id, d.n_treatment_id, d.v_detail_name, d.v_quantify, d.v_normal_range "
                + "from ms_lab_treatment_detil d "
                + "where d.n_treatment_id = ? "
                + "order by d.v_detail_name",
            (rs, rowNum) -> new LabTreatmentDetailResponse(
                rs.getInt("n_lab_detil_id"),
                rs.getInt("n_treatment_id"),
                rs.getString("v_detail_name"),
                rs.getString("v_quantify"),
                rs.getString("v_normal_range")
            ),
            treatmentId
        );
    }

    @Transactional
    public LabTreatmentDetailResponse createDetail(LabTreatmentDetailSaveRequest request, String username) {
        Integer treatmentId = request.getTreatmentId();
        String detailName = normalizeRequired(request.getDetailName(), "Nama detail wajib diisi.");
        String quantify = normalizeRequired(request.getQuantify(), "Satuan wajib diisi.");
        String normalRange = normalizeRequired(request.getNormalRange(), "Normal range wajib diisi.");
        getTreatmentById(treatmentId);
        Integer newId = nextSequenceValue("ms_lab_treatment_detil_n_lab_detil_id_seq");
        String actor = normalizeActor(username);
        jdbcTemplate.update(
            "insert into ms_lab_treatment_detil "
                + "(n_lab_detil_id, n_treatment_id, v_detail_name, v_quantify, v_normal_range, "
                + " v_who_create, d_whn_create) "
                + "values (?, ?, ?, ?, ?, ?, current_timestamp)",
            newId, treatmentId, detailName, quantify, normalRange, actor
        );
        return new LabTreatmentDetailResponse(newId, treatmentId, detailName, quantify, normalRange);
    }

    @Transactional
    public LabTreatmentDetailResponse updateDetail(Integer detailId, LabTreatmentDetailSaveRequest request, String username) {
        String detailName = normalizeRequired(request.getDetailName(), "Nama detail wajib diisi.");
        String quantify = normalizeRequired(request.getQuantify(), "Satuan wajib diisi.");
        String normalRange = normalizeRequired(request.getNormalRange(), "Normal range wajib diisi.");
        getDetailById(detailId);
        String actor = normalizeActor(username);
        int updated = jdbcTemplate.update(
            "update ms_lab_treatment_detil set "
                + "v_detail_name = ?, v_quantify = ?, v_normal_range = ?, "
                + "v_who_change = ?, d_whn_change = current_timestamp "
                + "where n_lab_detil_id = ?",
            detailName, quantify, normalRange, actor, detailId
        );
        if (updated == 0) throw new IllegalArgumentException("Detail tidak ditemukan.");
        return new LabTreatmentDetailResponse(detailId, request.getTreatmentId(), detailName, quantify, normalRange);
    }

    @Transactional
    public void deleteDetail(Integer detailId, String username) {
        int deleted = jdbcTemplate.update("delete from ms_lab_treatment_detil where n_lab_detil_id = ?", detailId);
        if (deleted == 0) throw new IllegalArgumentException("Detail tidak ditemukan.");
    }

    // Helpers
    private LabTreatmentDetailResponse getDetailById(Integer detailId) {
        try {
            return jdbcTemplate.queryForObject(
                "select d.n_lab_detil_id, d.n_treatment_id, d.v_detail_name, d.v_quantify, d.v_normal_range "
                    + "from ms_lab_treatment_detil d where d.n_lab_detil_id = ?",
                (rs, rowNum) -> new LabTreatmentDetailResponse(
                    rs.getInt("n_lab_detil_id"),
                    rs.getInt("n_treatment_id"),
                    rs.getString("v_detail_name"),
                    rs.getString("v_quantify"),
                    rs.getString("v_normal_range")
                ),
                detailId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Detail tidak ditemukan.");
        }
    }

    private Integer nextSequenceValue(String sequenceName) {
        Number number = jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Number.class);
        return number == null ? null : number.intValue();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(message);
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
