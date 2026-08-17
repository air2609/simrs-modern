package com.vone.simrs.mr;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0082 (DAFTAR PEMINJAMAN BERKAS REKAM MEDIS).
 *
 * <p>
 * Migrasi dari legacy {@code MrStatusController} +
 * {@code MedicalRecordManagerImpl}:
 * <ul>
 * <li>{@code getMROut()} → {@link #getLoanList()}</li>
 * <li>{@code updateStatus(win, status)} (MR_OUT / MR_BACK / MR_CANCEL) →
 * {@link #updateStatus(List, String)}</li>
 * </ul>
 */
@Service
public class MrLoanListService {

    private static final String TERSEDIA = "10";
    private static final String SEDANG_DIPINJAM = "8";
    private static final String AKAN_DIPINJAM = "9";

    public static final String MR_OUT = "MR_OUT";
    public static final String MR_BACK = "MR_BACK";
    public static final String MR_CANCEL = "MR_CANCEL";

    private static final String MSG_CANNOT_GO_OUT = "Berkas Sedang Dipinjam..!";
    private static final String MSG_CANNOT_BE_BACK = "Pasien Belum Keluar..!";
    private static final String MSG_CANNOT_BE_CANCEL = "Berkas Sedang Dipinjam..! Pembatalan Tidak Bisa Dilakukan..!";
    private static final String MSG_UPDATE_SUCCESS = "Status Berhasil Di-Update..!";

    private final JdbcTemplate jdbcTemplate;
    private final MrBorrowRequestService mrBorrowRequestService;

    public MrLoanListService(JdbcTemplate jdbcTemplate, MrBorrowRequestService mrBorrowRequestService) {
        this.jdbcTemplate = jdbcTemplate;
        this.mrBorrowRequestService = mrBorrowRequestService;
    }

    public String requireUsername(HttpSession session) {
        return mrBorrowRequestService.requireUsername(session);
    }

    /**
     * Sama persis dengan legacy {@code TbMedicalRecordDAO.getMROut()}: berkas yang
     * statusnya
     * bukan TERSEDIA (yaitu SEDANG DIPINJAM atau AKAN DIPINJAM).
     */
    public List<MrLoanListItemResponse> getLoanList() {
        return jdbcTemplate.query(
                "select mr.v_mr_code, mr.v_mr_status, patient.v_patient_name, unit.v_unit_name "
                        + "from tb_medical_record mr "
                        + "join ms_patient patient on patient.n_patient_id = mr.n_patient_id "
                        + "left join ms_unit unit on unit.n_unit_id = mr.n_unit_id "
                        + "where mr.v_mr_status is not null and mr.v_mr_status <> ? "
                        + "order by patient.v_patient_name",
                (resultSet, rowNum) -> new MrLoanListItemResponse(
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("v_patient_name"),
                        statusLabel(resultSet.getString("v_mr_status")),
                        resultSet.getString("v_unit_name")),
                TERSEDIA);
    }

    /**
     * Sama persis dengan legacy {@code MedicalRecordManagerImpl.updateStatus()}.
     */
    @Transactional
    public MrLoanUpdateResultResponse updateStatus(List<String> mrCodes, String action) {
        if (mrCodes == null || mrCodes.isEmpty()) {
            throw new IllegalArgumentException("Pilih Data Rekam Medis Terlebih Dahulu..!");
        }
        if (!MR_OUT.equals(action) && !MR_BACK.equals(action) && !MR_CANCEL.equals(action)) {
            throw new IllegalArgumentException("Aksi tidak dikenali.");
        }

        List<MrLoanUpdateItemResult> results = new ArrayList<MrLoanUpdateItemResult>();
        for (String mrCode : mrCodes) {
            results.add(processOne(mrCode, action));
        }

        return new MrLoanUpdateResultResponse(results);
    }

    private MrLoanUpdateItemResult processOne(String mrCode, String action) {
        String currentStatus;
        try {
            currentStatus = jdbcTemplate.queryForObject(
                    "select v_mr_status from tb_medical_record where v_mr_code = ?", String.class, mrCode);
        } catch (EmptyResultDataAccessException exception) {
            return new MrLoanUpdateItemResult(mrCode, false, "No.MR " + mrCode + " tidak ditemukan.");
        }

        String newStatus;

        if (MR_OUT.equals(action)) {
            if (SEDANG_DIPINJAM.equals(currentStatus)) {
                return new MrLoanUpdateItemResult(mrCode, false, MSG_CANNOT_GO_OUT);
            }
            newStatus = SEDANG_DIPINJAM;
        } else if (MR_BACK.equals(action)) {
            if (AKAN_DIPINJAM.equals(currentStatus)) {
                return new MrLoanUpdateItemResult(mrCode, false, MSG_CANNOT_BE_BACK);
            }
            newStatus = TERSEDIA;
        } else {
            if (!AKAN_DIPINJAM.equals(currentStatus)) {
                return new MrLoanUpdateItemResult(mrCode, false, MSG_CANNOT_BE_CANCEL);
            }
            newStatus = TERSEDIA;
        }

        jdbcTemplate.update("update tb_medical_record set v_mr_status = ? where v_mr_code = ?", newStatus, mrCode);
        return new MrLoanUpdateItemResult(mrCode, true, MSG_UPDATE_SUCCESS);
    }

    private String statusLabel(String status) {
        if (status == null || TERSEDIA.equals(status)) {
            return "TERSEDIA";
        }
        if (SEDANG_DIPINJAM.equals(status)) {
            return "SEDANG DIPINJAM";
        }
        return "AKAN DIPINJAM";
    }
}
