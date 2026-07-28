package com.vone.simrs.laborat;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.common.api.ApiResponse;
import com.vone.simrs.accounting.JournalService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LaboratService {

    private static final String SCREEN_LABORAT = "SC0041";
    private static final String DEFAULT_TARIFF_CLASS = "KELAS II";
    private static final short PAYMENT_UNPAID = 0;
    private static final int NOTE_ACTIVE = 1;
    private static final int NOTE_VALIDATED = 2;
    private static final int NOTE_CANCELED = 0;
    private static final DateTimeFormatter NOTE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyMM");
    private static final String[] LAB_PANEL_KEYS = {"HEMATOLOGI", "KIMIA", "IMUNO", "SEROLOGI", "ELEKTROLIT",
        "MIKROBIOLOGI", "URINE", "FECES", "LCS", "NARKOBA", "TRANSFUSI", "LAIN-LAIN"};

    private final JdbcTemplate jdbc;
    private final LegacyAuthService legacyAuthService;
    private final JournalService journalService;

    public LaboratService(JdbcTemplate jdbc, LegacyAuthService legacyAuthService, JournalService journalService) {
        this.jdbc = jdbc;
        this.legacyAuthService = legacyAuthService;
        this.journalService = journalService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    public LaboratMastersResponse getMasters(String username) {
        List<LaboratUnitResponse> units = getUnits(username);
        List<LaboratPatientTypeResponse> patientTypes = jdbc.query(
            "select pt.n_patient_type_id, pt.v_tpatient, pt.v_tpatient_desc from ms_patient_type pt order by pt.v_tpatient",
            (rs, row) -> new LaboratPatientTypeResponse(rs.getInt("n_patient_type_id"), rs.getString("v_tpatient"), rs.getString("v_tpatient_desc"))
        );
        List<LaboratEscortResponse> escorts = jdbc.query(
            "select e.n_escort_primary_id, e.v_escort_type from ms_patient_escort e order by e.v_escort_type",
            (rs, row) -> new LaboratEscortResponse(rs.getInt("n_escort_primary_id"), rs.getString("v_escort_type"))
        );
        return new LaboratMastersResponse(units, patientTypes, escorts);
    }

    private List<LaboratUnitResponse> getUnits(String username) {
        if (!hasLaboratAccess(username)) {
            return new ArrayList<>();
        }
        return jdbc.query(
            "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name "
                + "from ms_user usr "
                + "join ms_staff staff on staff.n_staff_id = usr.n_staff_id "
                + "join ms_staff_in_unit stfunit on stfunit.n_staff_id = staff.n_staff_id "
                + "join ms_unit unt on unt.n_unit_id = stfunit.n_unit_id "
                + "where upper(usr.v_user_name) = ? and staff.d_staff_fired_date is null "
                + "and unt.unit_type = 1 "
                + "order by unt.v_unit_name",
            (rs, row) -> new LaboratUnitResponse(
                rs.getInt("n_unit_id"),
                rs.getString("v_unit_code"),
                rs.getString("v_unit_name")
            ),
            normalizeUpper(username)
        );
    }

    private boolean hasLaboratAccess(String username) {
        Integer total = jdbc.queryForObject(
            "select count(1) from ("
                + "select scr.n_screen_id "
                + "from ms_user usr "
                + "join tb_user_privilege upr on upr.n_user_id = usr.n_user_id "
                + "join ms_screen scr on scr.n_screen_id = upr.n_screen_id "
                + "where upper(usr.v_user_name) = ? and scr.v_screen_code = ? "
                + "union "
                + "select scr.n_screen_id "
                + "from ms_user usr "
                + "join tb_group_privilege gpr on gpr.n_group_id = usr.n_group_id "
                + "join ms_screen scr on scr.n_screen_id = gpr.n_screen_id "
                + "where upper(usr.v_user_name) = ? and scr.v_screen_code = ?"
                + ") screen_access",
            Integer.class,
            normalizeUpper(username), SCREEN_LABORAT,
            normalizeUpper(username), SCREEN_LABORAT
        );
        return total != null && total.intValue() > 0;
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    // ──────────────────────────────────────────────
    //  Panel Lab — treatments grouped by category
    // ──────────────────────────────────────────────

    public List<LaboratPanelResponse> getPanels(String tariffClass) {
        List<LaboratPanelResponse> panels = new ArrayList<>();
        String effectiveTc = (tariffClass != null && !tariffClass.trim().isEmpty())
            ? tariffClass.trim().toUpperCase(Locale.ROOT) : DEFAULT_TARIFF_CLASS;
        for (String key : LAB_PANEL_KEYS) {
            List<LaboratTreatmentOptionResponse> treatments = jdbc.query(
                "select t.n_treatment_id, t.v_treatment_code, t.v_treatment_name, "
                    + "coalesce(tf.n_trtfee_fee, 0) as tariff "
                    + "from ms_treatment t "
                    + "join ms_treatment_group tg on tg.n_tgroup_id = t.n_tgroup_id "
                    + "left join ms_treatment_fee tf on tf.n_treatment_id = t.n_treatment_id "
                    + "left join ms_treatment_class tc on tc.n_tclass_id = tf.n_tclass_id "
                    + "where upper(tg.v_tgroup_name) like ? "
                    + "and tc.v_tclass_desc = ? "
                    + "order by t.v_treatment_code",
                (rs, row) -> new LaboratTreatmentOptionResponse(
                    rs.getInt("n_treatment_id"), rs.getString("v_treatment_code"),
                    rs.getString("v_treatment_name"), rs.getDouble("tariff")),
                "%" + key + "%", effectiveTc
            );
            if (!treatments.isEmpty()) {
                panels.add(new LaboratPanelResponse(key, treatments));
            } else {
                // Fallback: pakai data statis legacy (tanpa filter tariff class)
                List<LaboratTreatmentOptionResponse> fallback = getFallbackPanels(key);
                if (!fallback.isEmpty()) {
                    panels.add(new LaboratPanelResponse(key, fallback));
                }
            }
        }
        return panels;
    }

    private List<LaboratTreatmentOptionResponse> getFallbackPanels(String key) {
        List<LaboratTreatmentOptionResponse> list = new ArrayList<>();
        int id = 9000;
        String[][] data;
        switch (key.toUpperCase()) {
            case "HEMATOLOGI":
                data = new String[][]{
                    {"LAB01","DARAH LENGKAP"},{"LAB02","DARAH RUTIN"},{"LAB03","DHF"},
                    {"LAB04","HB"},{"LAB05","HCT"},{"LAB06","AL"},
                    {"LAB07","HITUNG JENIS LEKOSIT"},{"LAB08","AT"},{"LAB09","AE"},
                    {"LAB10","EOSINOPHIL"},{"LAB11","RETIKULOSIT"},{"LAB12","GOLONGAN DARAH"},
                    {"LAB13","GOLONGAN DARAH + RH"},{"LAB14","LED"},{"LAB15","BE"},
                    {"LAB16","CT"},{"LAB17","MCV"},{"LAB18","MCH"},{"LAB19","MCHC"},
                    {"LAB20","FIBRINOGEN"},{"LAB21","MALARIA"},{"LAB22","FILARIA"}};
                break;
            case "KIMIA":
                data = new String[][]{
                    {"LAB23","GLUKOSA PUASA"},{"LAB24","GLUKOSA 2JPP"},{"LAB25","GLUKOSA SEWAKTU"},
                    {"LAB26","CHOLESTEROL TOTAL"},{"LAB27","TRIGLICERIDE"},{"LAB28","HDL-CHOLESTEROL"},
                    {"LAB29","LDL-CHOLESTEROL"},{"LAB30","BILIRUBIN TOTAL"},{"LAB31","BILIRUBIN DIREK"},
                    {"LAB32","BILIRUBIN INDIREK"},{"LAB33","AST (SGOT)"},{"LAB34","ALT (SGPT)"},
                    {"LAB35","UREUM"},{"LAB36","CREATININ"},{"LAB37","TOTAL PROTEIN"},
                    {"LAB38","ALBUMIN"},{"LAB39","GLOBULIN"},{"LAB40","ASAM URAT"},
                    {"LAB41","ALKALI FOSFATASE"},{"LAB42","CHOLINESTERASE"},{"LAB43","GAMMA GT"}};
                break;
            case "IMUNO":
            case "SEROLOGI":
                data = new String[][]{
                    {"LAB44","HBSAG SLIDE"},{"LAB45","HBSAG ELISA"},{"LAB46","ANTI HBS"},
                    {"LAB47","WIDAL"},{"LAB48","VDRL"},{"LAB61","PLANO TEST"},{"LAB62","RAPID TEST"},
                    {"LAB49","JUMLAH SEL"},{"LAB50","HITUNG JENIS SEL"},{"LAB51","RIVALTA"},
                    {"LAB52","PROTEIN"},{"LAB53","GLUKOSA"}};
                break;
            case "ELEKTROLIT":
                data = new String[][]{
                    {"LAB63","KALIUM"},{"LAB64","NATRIUM"},{"LAB65","CALCIUM"},{"LAB66","CHLORIDA"},
                    {"LAB67","CKMB"},{"LAB68","HBDH"},{"LAB69","LDH"},{"LAB70","CKNAC"},{"LAB71","TROPONIN I"}};
                break;
            case "MIKROBIOLOGI":
                data = new String[][]{
                    {"LAB54","GRAM"},{"LAB55","NEISSER"},{"LAB56","BTA"},
                    {"LAB57","CANDIDA"},{"LAB58","TRICHOMONAS"},{"LAB59","KULTUR"},{"LAB60","SENSITIVITAS TEST"}};
                break;
            case "URINE":
                data = new String[][]{
                    {"LAB91","RUTIN"},{"LAB92","PROTEIN"},{"LAB93","REDUKSI"},{"LAB94","KETON"},{"LAB95","SEDIMEN"}};
                break;
            case "FECES":
                data = new String[][]{{"LAB72","RUTIN"},{"LAB73","DARAH SAMAR"}};
                break;
            case "LCS":
                data = new String[][]{
                    {"LAB79","JUMLAH SEL"},{"LAB80","HITUNG JENIS SEL"},{"LAB81","NONNE"},
                    {"LAB82","PANDY"},{"LAB83","PROTEIN"},{"LAB84","GLUKOSA"}};
                break;
            case "NARKOBA":
                data = new String[][]{
                    {"LAB87","AMPHETAMINE"},{"LAB88","BENZODIAZEPINE"},{"LAB89","COCCAIN"},{"LAB90","MORPHINE"}};
                break;
            case "TRANSFUSI":
                data = new String[][]{{"LAB85","CROSS TEST"},{"LAB86","WHOLE BLOOD"}};
                break;
            case "LAIN-LAIN":
                data = new String[][]{
                    {"LAB74","BGA"},{"LAB75","PA"},{"LAB76","HELICOBACTER"},{"LAB77","PAP SMEAR"},{"LAB78","FNAB"}};
                break;
            default:
                return list;
        }
        for (String[] row : data) {
            list.add(new LaboratTreatmentOptionResponse(++id, row[0], row[1], 0));
        }
        return list;
    }

    public List<LaboratRegisteredPatientResponse> searchRegisteredPatients(String mrCode, String patientName, String address) {
        StringBuilder sql = new StringBuilder(
            "select mr.v_mr_code, p.v_patient_name, p.v_patient_gender, to_char(p.d_patient_dob, 'DD-MM-YYYY') as birth_date, p.v_patient_main_addr, p.n_patient_id "
                + "from ms_patient p join tb_medical_record mr on mr.n_patient_id = p.n_patient_id where 1=1 ");
        List<Object> params = new ArrayList<>();
        if (mrCode != null && !mrCode.trim().isEmpty()) {
            sql.append("and upper(mr.v_mr_code) like ? ");
            params.add("%" + mrCode.trim().toUpperCase() + "%");
        }
        if (patientName != null && !patientName.trim().isEmpty()) {
            sql.append("and upper(p.v_patient_name) like ? ");
            params.add("%" + patientName.trim().toUpperCase() + "%");
        }
        if (address != null && !address.trim().isEmpty()) {
            sql.append("and upper(p.v_patient_main_addr) like ? ");
            params.add("%" + address.trim().toUpperCase() + "%");
        }
        sql.append("order by p.v_patient_name limit 50");
        return jdbc.query(sql.toString(),
            (rs, row) -> new LaboratRegisteredPatientResponse(
                rs.getString("v_mr_code"), rs.getString("v_patient_name"),
                rs.getString("v_patient_gender"), rs.getString("birth_date"), rs.getString("v_patient_main_addr"), rs.getInt("n_patient_id")),
            params.toArray());
    }

    public LaboratPatientDetailResponse getPatientDetail(String mrCode) {
        try {
            return jdbc.queryForObject(
                "select mr.v_mr_code, p.v_patient_name, p.v_patient_gender, "
                    + "to_char(p.d_patient_dob, 'DD-MM-YYYY') as birth_date, "
                    + "p.v_patient_age as age, p.v_patient_main_addr, "
                    + "p.v_patient_religion, coalesce(pt.v_tpatient_desc, '-') as patient_type, "
                    + "coalesce(doc.v_staff_name, '-') as doctor_name, "
                    + "'-' as escort_name, "
                    + "p.n_patient_id, reg.n_reg_id, reg.v_reg_secondary_id "
                    + "from ms_patient p "
                    + "join tb_medical_record mr on mr.n_patient_id = p.n_patient_id "
                    + "left join ms_patient_type pt on pt.n_patient_type_id = p.n_patient_type_id "
                    + "left join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                    + "left join ms_staff doc on doc.n_staff_id = reg.n_staff_id "
                    + "where mr.v_mr_code = ? "
                    + "order by reg.d_registration_date desc limit 1",
                (rs, row) -> {
                    Integer patientId = rs.getInt("n_patient_id");
                    String regCode = rs.getString("v_reg_secondary_id");
                    Integer regId = rs.getObject("n_reg_id", Integer.class);
                    boolean isInpatient = regCode != null && (regCode.startsWith("I-") || regCode.startsWith("I/"));
                    String tariffClass = isInpatient && regId != null
                        ? determineTariffClass(regId)
                        : DEFAULT_TARIFF_CLASS;
                    List<LaboratRegistrationOption> regs = getRegistrations(patientId);
                    return new LaboratPatientDetailResponse(
                        rs.getString("v_mr_code"), rs.getString("v_patient_name"),
                        rs.getString("v_patient_gender"), rs.getString("birth_date"),
                        rs.getInt("age") == 0 && rs.wasNull() ? null : rs.getInt("age"),
                        rs.getString("v_patient_main_addr"),
                        rs.getString("v_patient_religion"),
                        rs.getString("patient_type"),
                        rs.getString("doctor_name"),
                        rs.getString("escort_name"),
                        patientId, isInpatient, tariffClass, regs);
                }, mrCode);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Pasien tidak ditemukan.");
        }
    }

    private String determineTariffClass(Integer regId) {
        try {
            return jdbc.queryForObject(
                "select tclass.v_tclass_desc from tb_bed_occupancy boc "
                    + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                    + "join ms_treatment_class tclass on tclass.n_tclass_id = bed.n_tclass_id "
                    + "where boc.n_reg_primary_id = ? and boc.d_check_out_time is null limit 1",
                String.class, regId);
        } catch (EmptyResultDataAccessException e) {
            return DEFAULT_TARIFF_CLASS;
        }
    }

    private List<LaboratRegistrationOption> getRegistrations(Integer patientId) {
        return jdbc.query(
            "select r.n_reg_id, r.v_reg_secondary_id, u.v_unit_name, "
                + "to_char(r.d_whn_create, 'DD-MM-YYYY') as reg_date "
                + "from tb_registration r join tb_medical_record mr on mr.n_mr_id = r.n_mr_id join ms_unit u on u.n_unit_id = r.n_unit_id "
                + "where mr.n_patient_id = ? order by r.d_whn_create desc limit 20",
            (rs, row) -> new LaboratRegistrationOption(
                rs.getInt("n_reg_id"), rs.getString("v_reg_secondary_id"),
                rs.getString("v_unit_name"), rs.getString("reg_date")),
            patientId);
    }

    public List<LaboratTreatmentOptionResponse> searchTreatments(Integer unitId, String code, String name, String tariffClass) {
        StringBuilder sql = new StringBuilder(
            "select t.n_treatment_id, t.v_treatment_code, t.v_treatment_name, "
                + "coalesce(tf.n_trtfee_fee, 0) as tariff "
                + "from ms_treatment t left join ms_treatment_fee tf on tf.n_treatment_id = t.n_treatment_id ");
        List<Object> params = new ArrayList<>();
        if (tariffClass != null && !tariffClass.trim().isEmpty()) {
            sql.append("and tf.n_tclass_id = (select tc.n_tclass_id from ms_treatment_class tc where tc.v_tclass_desc = ?) ");
            params.add(tariffClass.trim().toUpperCase(Locale.ROOT));
        }
        sql.append("where 1=1 ");
        if (code != null && !code.trim().isEmpty()) {
            sql.append("and upper(t.v_treatment_code) like ? ");
            params.add("%" + code.trim().toUpperCase() + "%");
        }
        if (name != null && !name.trim().isEmpty()) {
            sql.append("and upper(t.v_treatment_name) like ? ");
            params.add("%" + name.trim().toUpperCase() + "%");
        }
        sql.append("order by t.v_treatment_code limit 50");
        return jdbc.query(sql.toString(),
            (rs, row) -> new LaboratTreatmentOptionResponse(
                rs.getInt("n_treatment_id"), rs.getString("v_treatment_code"),
                rs.getString("v_treatment_name"), rs.getDouble("tariff")),
            params.toArray());
    }

    public List<LaboratItemOptionResponse> searchItems(Integer unitId, String code, String name, String tariffClass) {
        StringBuilder sql = new StringBuilder(
            "select i.n_item_id, i.v_item_code, i.v_item_name, coalesce(i.n_price, 0) as price "
                + "from ms_item i where 1=1 ");
        List<Object> params = new ArrayList<>();
        if (code != null && !code.trim().isEmpty()) {
            sql.append("and upper(i.v_item_code) like ? ");
            params.add("%" + code.trim().toUpperCase() + "%");
        }
        if (name != null && !name.trim().isEmpty()) {
            sql.append("and upper(i.v_item_name) like ? ");
            params.add("%" + name.trim().toUpperCase() + "%");
        }
        sql.append("order by i.v_item_code limit 50");
        return jdbc.query(sql.toString(),
            (rs, row) -> new LaboratItemOptionResponse(
                rs.getInt("n_item_id"), rs.getString("v_item_code"),
                rs.getString("v_item_name"), rs.getDouble("price")),
            params.toArray());
    }

    public List<LaboratNoteSummaryResponse> searchNotes(Integer unitId, String noteNumber, String patientName) {
        StringBuilder sql = new StringBuilder(
            "select e.n_exam_id, e.v_note_no, p.v_patient_name, e.n_exam_status, "
                + "case when e.n_exam_status = 0 then 'BARU' when e.n_exam_status = 1 then 'VALID' else 'BATAL' end as status_label, "
                + "to_char(e.d_whn_create, 'DD-MM-YYYY HH24:MI') as created_at "
                + "from tb_examination e join ms_patient p on p.n_patient_id = e.n_patient_id ");
        if (unitId != null) sql.append("and e.n_unit_id = ? ");
        sql.append("where e.n_exam_status >= 0 ");
        List<Object> params = new ArrayList<>();
        if (unitId != null) params.add(unitId);
        if (noteNumber != null && !noteNumber.trim().isEmpty()) {
            sql.append("and upper(e.v_note_no) like ? ");
            params.add("%" + noteNumber.trim().toUpperCase() + "%");
        }
        if (patientName != null && !patientName.trim().isEmpty()) {
            sql.append("and upper(p.v_patient_name) like ? ");
            params.add("%" + patientName.trim().toUpperCase() + "%");
        }
        sql.append("order by e.d_whn_create desc limit 30");
        return jdbc.query(sql.toString(),
            (rs, row) -> new LaboratNoteSummaryResponse(
                rs.getInt("n_exam_id"), rs.getString("v_note_no"),
                rs.getString("v_patient_name"), rs.getInt("n_exam_status"),
                rs.getString("status_label"), rs.getString("created_at")),
            params.toArray());
    }

    public LaboratNoteDetailResponse getNoteDetail(Integer noteId) {
        try {
            return jdbc.queryForObject(
                "select e.n_exam_id, e.v_note_no, e.n_exam_status, "
                    + "case when e.n_exam_status = 0 then 'BARU' when e.n_exam_status = 1 then 'VALID' else 'BATAL' end as status_label, "
                    + "p.v_patient_name, mr.v_mr_code, coalesce(r.v_reg_secondary_id, '-') as reg_code, "
                    + "coalesce(e.n_total_amount, 0) as total_amount "
                    + "from tb_examination e "
                    + "join ms_patient p on p.n_patient_id = e.n_patient_id "
                    + "join tb_medical_record mr on mr.n_patient_id = p.n_patient_id "
                    + "left join tb_registration r on r.n_reg_id = e.n_reg_id "
                    + "where e.n_exam_id = ?",
                (rs, row) -> {
                    List<LaboratNoteLineResponse> lines = getNoteLines(noteId);
                    return new LaboratNoteDetailResponse(
                        rs.getInt("n_exam_id"), rs.getString("v_note_no"),
                        rs.getInt("n_exam_status"), rs.getString("status_label"),
                        rs.getString("v_patient_name"), rs.getString("v_mr_code"),
                        rs.getString("reg_code"), 0,
                        null, rs.getDouble("total_amount"), lines);
                }, noteId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Nota tidak ditemukan.");
        }
    }

    private List<LaboratNoteLineResponse> getNoteLines(Integer noteId) {
        List<LaboratNoteLineResponse> lines = new ArrayList<>();
        // Treatment lines
        lines.addAll(jdbc.query(
            "select tt.n_treatment_fee_id as ref_id, t.v_treatment_code as code, t.v_treatment_name as description, "
                + "coalesce(tt.n_amount_trx, 0) as unit_price, coalesce(tt.n_qty, 1) as qty, "
                + "coalesce(tt.n_amount_trx, 0) as subtotal, coalesce(tt.n_disc_amount, 0) as disc_amount, "
                + "coalesce(tt.v_disc_type, 'RP') as disc_type "
                + "from tb_treatment_trx tt "
                + "join ms_treatment_fee tf on tf.n_treatment_fee_id = tt.n_treatment_fee_id "
                + "join ms_treatment t on t.n_treatment_id = tf.n_treatment_id "
                + "where tt.n_note_id = ? order by tt.n_treatment_id",
            (rs, row) -> new LaboratNoteLineResponse(
                "TREATMENT", rs.getInt("ref_id"), rs.getString("code"),
                rs.getString("description"), rs.getDouble("unit_price"),
                rs.getDouble("qty"), rs.getDouble("subtotal"),
                rs.getDouble("disc_amount"), rs.getString("disc_type")),
            noteId));
        // Item lines
        lines.addAll(jdbc.query(
            "select it.n_item_id as ref_id, i.v_item_code as code, i.v_item_name as description, "
                + "coalesce(it.n_amount_trx, 0) as unit_price, coalesce(it.n_qty, 1) as qty, "
                + "coalesce(it.n_amount_after_disc, 0) as subtotal, 0 as disc_amount, 'RP' as disc_type "
                + "from tb_item_trx it join ms_item i on i.n_item_id = it.n_item_id "
                + "where it.n_note_id = ? order by it.n_item_trx_id",
            (rs, row) -> new LaboratNoteLineResponse(
                "ITEM", rs.getInt("ref_id"), rs.getString("code"),
                rs.getString("description"), rs.getDouble("unit_price"),
                rs.getDouble("qty"), rs.getDouble("subtotal"),
                rs.getDouble("disc_amount"), rs.getString("disc_type")),
            noteId));
        return lines;
    }

    @Transactional
    public LaboratSaveResultResponse createNote(LaboratSaveRequest req, String username) {
        String actor = normalizeActor(username);
        Integer noteId = nextVal("tb_examination_n_exam_id_seq");
        Integer noteSeq = nextVal("nota_rajal_seq");
        String noteNo = "J-LAB-" + NOTE_DATE_FORMAT.format(LocalDateTime.now()) + "-" + String.format("%06d", noteSeq);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        Integer patientId = getPatientIdByMr(req.getMrCode());
        Integer regId = getRegistrationId(req.getRegistrationCode());

        jdbc.update(
            "insert into tb_examination (n_exam_id, v_note_no, n_patient_id, n_reg_id, n_unit_id, "
                + "n_escort_id, n_exam_status, n_total_amount, n_payment_status, v_who_create, d_whn_create) "
                + "values (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?)",
            noteId, noteNo, patientId, regId, req.getUnitId(),
            req.getEscortId(), NOTE_ACTIVE,
            PAYMENT_UNPAID, actor, now);

        double total = saveLines(noteId, req, actor, now);
        jdbc.update("update tb_examination set n_total_amount = ? where n_exam_id = ?", total, noteId);

        return new LaboratSaveResultResponse(noteId, noteNo, "Nota berhasil disimpan.");
    }

    @Transactional
    public LaboratSaveResultResponse updateNote(Integer noteId, LaboratSaveRequest req, String username) {
        String actor = normalizeActor(username);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // Delete existing lines
        jdbc.update("delete from tb_treatment_trx where n_exam_id = ?", noteId);
        jdbc.update("delete from tb_item_trx where n_exam_id = ?", noteId);

        double total = saveLines(noteId, req, actor, now);
        jdbc.update(
            "update tb_examination set n_unit_id = ?, n_doctor_id = ?, n_patient_type_id = ?, n_escort_id = ?, "
                + "n_total_amount = ?, v_who_change = ?, d_whn_change = ? where n_exam_id = ?",
            req.getUnitId(), parseStaffId(req.getDoctorStaffId()), req.getPatientTypeId(),
            req.getEscortId(), total, actor, now, noteId);

        return new LaboratSaveResultResponse(noteId, null, "Nota berhasil diubah.");
    }

    private double saveLines(Integer noteId, LaboratSaveRequest req, String actor, Timestamp now) {
        double total = 0;

        if (req.getTreatments() != null) {
            for (LaboratLineItemRequest line : req.getTreatments()) {
                total += line.getQuantity() * line.getUnitPrice();
                Integer trxId = nextVal("tb_treatment_trx_n_treatment_id_seq");
                Integer feeId = findTreatmentFeeId(line.getRefId());
                if (feeId == null) {
                    throw new IllegalArgumentException("Treatment fee tidak ditemukan untuk treatment ID: " + line.getRefId());
                }
                jdbc.update(
                    "insert into tb_treatment_trx (n_treatment_id, n_note_id, n_treatment_fee_id, n_qty, "
                        + "n_amount_trx, n_disc_amount, n_amount_after_disc, v_disc_type, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    trxId, noteId, feeId, line.getQuantity(),
                    line.getQuantity() * line.getUnitPrice(), line.getDiscountAmount(),
                    line.getQuantity() * line.getUnitPrice() - line.getDiscountAmount(),
                    line.getDiscountType(), actor, now);
            }
        }

        if (req.getItems() != null) {
            for (LaboratLineItemRequest line : req.getItems()) {
                total += line.getQuantity() * line.getUnitPrice();
                Integer trxId = nextVal("tb_item_trx_n_item_trx_id_seq");
                jdbc.update(
                    "insert into tb_item_trx (n_item_trx_id, n_note_id, n_item_id, n_qty, "
                        + "n_amount_trx, n_amount_after_disc, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                    trxId, noteId, line.getRefId(), line.getQuantity(), line.getUnitPrice(),
                    line.getQuantity() * line.getUnitPrice(), actor, now);
            }
        }
        return total;
    }

    @Transactional
    public LaboratActionResultResponse validateNote(Integer noteId, String username) {
        String actor = normalizeActor(username);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        int updated = jdbc.update(
            "update tb_examination set n_exam_status = ?, v_who_change = ?, d_whn_change = current_timestamp "
                + "where n_exam_id = ? and n_exam_status = ?",
            NOTE_VALIDATED, actor, noteId, NOTE_ACTIVE);
        if (updated == 0) throw new IllegalStateException("Hanya nota BARU yang bisa divalidasi.");
        // Auto jurnal setelah validasi
        createLaboratJournal(noteId, now, actor);
        return new LaboratActionResultResponse(true, "Nota berhasil divalidasi.");
    }

    @Transactional
    public LaboratActionResultResponse cancelNote(Integer noteId, LaboratCancelRequest req, String username) {
        String actor = normalizeActor(username);
        int updated = jdbc.update(
            "update tb_examination set n_exam_status = ?, v_cancelation_note = ?, v_who_change = ?, d_whn_change = current_timestamp "
                + "where n_exam_id = ? and n_exam_status = ?",
            NOTE_CANCELED, req.getReason(), actor, noteId, NOTE_ACTIVE);
        if (updated == 0) throw new IllegalStateException("Hanya nota BARU yang bisa dibatalkan.");
        return new LaboratActionResultResponse(true, "Nota berhasil dibatalkan.");
    }

    // ── Auto Journal (dipanggil saat validasi) ──
    private void createLaboratJournal(Integer noteId, Timestamp now, String username) {
        String batchId = journalService.buildJournalBatchId();
        String noteNumber = jdbc.queryForObject("select v_note_no from tb_examination where n_exam_id = ?", String.class, noteId);
        Integer coaArId = journalService.findCoaIdByGimKey("COA_OUTPATIENT_AR");
        if (coaArId == null) coaArId = journalService.findCoaIdByGimKey("COA_INPATIENT_AR");
        Integer coaTreatId = journalService.findCoaIdByGimKey("COA_TREATMENT");
        if (coaTreatId == null) coaTreatId = journalService.findCoaIdByGimKey("COA_MISC_TRX");

        // Treatment lines
        List<Object[]> treatments = jdbc.query(
            "select t.v_treatment_name as description, coalesce(tt.n_amount_trx, 0) as amount "
                + "from tb_treatment_trx tt "
                + "join ms_treatment_fee tf on tf.n_treatment_fee_id = tt.n_treatment_fee_id "
                + "join ms_treatment t on t.n_treatment_id = tf.n_treatment_id "
                + "where tt.n_note_id = ?",
            (rs, row) -> new Object[]{rs.getString("description"), rs.getDouble("amount")},
            noteId);
        for (Object[] row : treatments) {
            String desc = (String) row[0];
            double amt = Math.ceil((Double) row[1]);
            if (amt <= 0) continue;
            journalService.insertJournalEntry(batchId, noteNumber, desc, amt, 0, now, username, coaArId);
            journalService.insertJournalEntry(batchId, noteNumber, desc, 0, amt, now, username, coaTreatId);
        }
    }

    // Helpers
    private Integer getPatientIdByMr(String mrCode) {
        try {
            return jdbc.queryForObject(
                "select mr.n_patient_id from tb_medical_record mr where mr.v_mr_code = ?",
                Integer.class, mrCode);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("No. MR tidak ditemukan.");
        }
    }

    private Integer getRegistrationId(String regCode) {
        if (regCode == null || regCode.trim().isEmpty()) return null;
        try {
            return jdbc.queryForObject(
                "select r.n_reg_id from tb_registration r where r.v_reg_secondary_id = ?",
                Integer.class, regCode);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Integer parseStaffId(String staffId) {
        if (staffId == null || staffId.trim().isEmpty()) return null;
        try {
            return Integer.valueOf(staffId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer findTreatmentFeeId(Integer treatmentId) {
        if (treatmentId == null) return null;
        try {
            return jdbc.queryForObject(
                "select n_treatment_fee_id from ms_treatment_fee where n_treatment_id = ? limit 1",
                Integer.class, treatmentId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Integer nextVal(String seq) {
        Number n = jdbc.queryForObject("select nextval('" + seq + "')", Number.class);
        return n == null ? null : n.intValue();
    }

    private String normalizeActor(String username) {
        return username == null ? "SYSTEM" : username.trim().toUpperCase(Locale.ROOT);
    }
}
