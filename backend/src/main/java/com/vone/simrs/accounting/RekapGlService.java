package com.vone.simrs.accounting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0176 (REKAP GL / rekapGl.zul).
 *
 * <p>
 * Migrasi dari legacy {@code RekapGlController} + {@code TbGlDAO}:
 * <ul>
 * <li>{@code RekapGlController.getRekapList()} → {@link #list()}</li>
 * <li>{@code RekapGlController.save()} → {@link #save(RekapGlSaveRequest, String)}</li>
 * <li>Download file → {@link #getFile(Integer)} (file xlsx di-generate sesuai
 * kebutuhan dari {@code report.func_rekap_gl_all_bydate(...)})</li>
 * </ul>
 */
@Service
public class RekapGlService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_NAME = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private final JdbcTemplate jdbcTemplate;
    private final String exportDir;

    public RekapGlService(JdbcTemplate jdbcTemplate,
            @Value("${app.rekap-gl-dir:./rekap-gl}") String exportDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.exportDir = exportDir;
    }

    /**
     * Daftar REKAP GL. Migrasi dari legacy {@code RekapGlController.getRekapList()}.
     */
    public List<RekapGlRowResponse> list() {
        return jdbcTemplate.query(
                "select id, d_from, d_to, coalesce(n_status, 0) as n_status, v_file_location "
                        + "from tb_gl order by id desc",
                (resultSet, rowNum) -> new RekapGlRowResponse(
                        resultSet.getInt("id"),
                        toDisplayDate(resultSet.getDate("d_from")),
                        toDisplayDate(resultSet.getDate("d_to")),
                        resultSet.getInt("n_status"),
                        resultSet.getString("v_file_location") != null
                                && !resultSet.getString("v_file_location").trim().isEmpty()));
    }

    /**
     * Simpan REKAP GL baru dengan validasi rentang tanggal (DARI &lt;= SAMPAI,
     * maksimal 3 bulan). Migrasi dari legacy {@code RekapGlController.save()}.
     */
    @Transactional
    public void save(RekapGlSaveRequest request, String username) {
        LocalDate from = parseDate(request.getFrom(), "TANGGAL DARI WAJIB DIISI!");
        LocalDate to = parseDate(request.getTo(), "TANGGAL SAMPAI WAJIB DIISI!");

        long tgl1 = from.toEpochDay();
        long tgl2 = to.toEpochDay();
        if (tgl1 > tgl2) {
            throw new IllegalArgumentException("Rentang tanggal salah, silahkan diperbaiki!");
        }
        long hari = tgl2 - tgl1;
        if (hari > 93) {
            throw new IllegalArgumentException("Rentang waktu yang diizinkan hanya 3 bulan!");
        }

        Integer id = nextVal("tb_gl_id_seq");
        jdbcTemplate.update(
                "insert into tb_gl (id, d_from, d_to, n_status) values (?, ?, ?, ?)",
                id, java.sql.Date.valueOf(from), java.sql.Date.valueOf(to), 0);
    }

    /**
     * Ambil file xlsx REKAP GL. Jika belum ada, file di-generate dari
     * {@code report.func_rekap_gl_all_bydate(dfrom, dto)} lalu disimpan dan
     * {@code v_file_location} diperbarui.
     */
    public byte[] getFile(Integer id) {
        String from;
        String to;
        String fileLocation;
        try {
            from = jdbcTemplate.queryForObject(
                    "select to_char(d_from, 'yyyy-MM-dd') from tb_gl where id = ?",
                    String.class, id);
            to = jdbcTemplate.queryForObject(
                    "select to_char(d_to, 'yyyy-MM-dd') from tb_gl where id = ?",
                    String.class, id);
            fileLocation = jdbcTemplate.queryForObject(
                    "select v_file_location from tb_gl where id = ?",
                    String.class, id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("DATA TIDAK DITEMUKAN.");
        }

        byte[] content = null;
        File file = null;
        if (fileLocation != null && !fileLocation.trim().isEmpty()) {
            file = new File(fileLocation);
            if (file.exists()) {
                try {
                    content = readFile(file);
                } catch (Exception exception) {
                    content = null;
                }
            }
        }

        if (content == null) {
            content = generateXlsx(from, to);
            file = persistFile(id, from, to, content);
        }
        if (file != null) {
            try {
                jdbcTemplate.update(
                        "update tb_gl set v_file_location = ? where id = ?",
                        file.getAbsolutePath(), id);
            } catch (Exception ignored) {
                // update lokasi file bersifat best-effort
            }
        }
        return content;
    }

    private byte[] generateXlsx(String from, String to) {
        String filename = FILE_NAME.format(LocalDate.parse(from)) + "_"
                + FILE_NAME.format(LocalDate.parse(to)) + ".xlsx";

        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
        sheet.append("<sheetData>\n");

        int rowIndex = 1;
        sheet.append(row(rowIndex++, cellString(1, "REKAP GL DARI " + DISPLAY.format(LocalDate.parse(from))
                + " SAMPAI " + DISPLAY.format(LocalDate.parse(to)))));

        String[] headers = { "TANGGAL", "BATCH", "VOUCHER", "KETERANGAN", "DEBIT", "KREDIT", "SALDO" };
        StringBuilder headerCells = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            headerCells.append(cellString(i + 1, headers[i]));
        }
        sheet.append(row(rowIndex++, headerCells.toString()));

        List<RekapGlLine> lines = jdbcTemplate.query(
                "select v_acct_name, d_apl_date, v_journal_batch_id, v_voucher_no, v_desc, "
                        + "n_debit, n_credit, n_balance "
                        + "from report.func_rekap_gl_all_bydate(to_date(?, 'yyyy-MM-dd'), to_date(?, 'yyyy-MM-dd'))",
                (resultSet, rowNum) -> new RekapGlLine(
                        resultSet.getString("v_acct_name"),
                        resultSet.getTimestamp("d_apl_date"),
                        resultSet.getString("v_journal_batch_id"),
                        resultSet.getString("v_voucher_no"),
                        resultSet.getString("v_desc"),
                        resultSet.getObject("n_debit") != null ? resultSet.getDouble("n_debit") : null,
                        resultSet.getObject("n_credit") != null ? resultSet.getDouble("n_credit") : null,
                        resultSet.getObject("n_balance") != null ? resultSet.getDouble("n_balance") : null),
                from, to);

        String currentAccount = null;
        for (RekapGlLine line : lines) {
            if (line.acctName == null) {
                sheet.append(row(rowIndex++, ""));
                continue;
            }
            if (!line.acctName.equals(currentAccount)) {
                currentAccount = line.acctName;
                sheet.append(row(rowIndex++, cellString(1, "AKUN : " + line.acctName)));
            }
            StringBuilder cells = new StringBuilder();
            cells.append(cellString(1, line.aplDate == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(line.aplDate)));
            cells.append(cellString(2, line.batchId == null ? "" : line.batchId));
            cells.append(cellString(3, line.voucherNo == null ? "" : line.voucherNo));
            cells.append(cellString(4, line.desc == null ? "" : line.desc));
            cells.append(cellNumber(5, line.debit));
            cells.append(cellNumber(6, line.credit));
            cells.append(cellNumber(7, line.balance));
            sheet.append(row(rowIndex++, cells.toString()));
        }

        sheet.append("</sheetData>\n</worksheet>\n");

        return buildXlsx(filename, sheet.toString());
    }

    private byte[] buildXlsx(String filename, String sheetXml) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(out);

            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "</Types>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                    + "<sheets><sheet name=\"REKAP GL\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                    + "</workbook>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels"));
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    + "</Relationships>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            zip.write(sheetXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("GAGAL MEMBUAT FILE REKAP GL.", exception);
        }
    }

    private File persistFile(Integer id, String from, String to, byte[] content) {
        try {
            File dir = new File(exportDir);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            String filename = FILE_NAME.format(LocalDate.parse(from)) + "_"
                    + FILE_NAME.format(LocalDate.parse(to)) + ".xlsx";
            File file = new File(dir, id + "_" + filename);
            FileOutputStream out = new FileOutputStream(file);
            out.write(content);
            out.close();
            return file;
        } catch (Exception exception) {
            return null;
        }
    }

    private byte[] readFile(File file) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        return out.toByteArray();
    }

    private String row(int index, String cells) {
        return "<row r=\"" + index + "\">" + cells + "</row>\n";
    }

    private String cellString(int col, String value) {
        String v = value == null ? "" : value;
        return "<c r=\"" + colName(col) + "\" t=\"inlineStr\"><is><t>" + escapeXml(v) + "</t></is></c>";
    }

    private String cellNumber(int col, Double value) {
        if (value == null) {
            return "<c r=\"" + colName(col) + "\"/>";
        }
        return "<c r=\"" + colName(col) + "\"><v>" + value + "</v></c>";
    }

    private String colName(int col) {
        return (char) ('A' + col - 1) + "1";
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private LocalDate parseDate(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        try {
            return LocalDate.parse(value.trim(), ISO);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private String toDisplayDate(java.sql.Date value) {
        return value == null ? "" : DISPLAY.format(value.toLocalDate());
    }

    private Integer nextVal(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private String normalize(String value) {
        return value == null ? "SYSTEM" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static class RekapGlLine {

        final String acctName;
        final Timestamp aplDate;
        final String batchId;
        final String voucherNo;
        final String desc;
        final Double debit;
        final Double credit;
        final Double balance;

        RekapGlLine(String acctName, Timestamp aplDate, String batchId, String voucherNo,
                String desc, Double debit, Double credit, Double balance) {
            this.acctName = acctName;
            this.aplDate = aplDate;
            this.batchId = batchId;
            this.voucherNo = voucherNo;
            this.desc = desc;
            this.debit = debit;
            this.credit = credit;
            this.balance = balance;
        }
    }
}
