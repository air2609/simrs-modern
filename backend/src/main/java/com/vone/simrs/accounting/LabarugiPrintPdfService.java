package com.vone.simrs.accounting;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Generator PDF "P/L REPORT" untuk tombol CETAK / CETAK BY DATE screen SC0203.
 * Migrasi dari legacy {@code AccountingReport.openCurrentLabarugi()} +
 * {@code LabarugiController.printLabarugi()} + report
 * {@code jasper/laba_rugi.jrxml} (dikelompokkan per {@code n_row} dengan
 * header {@code v_desc}).
 */
@Service
public class LabarugiPrintPdfService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.###");

    private final JdbcTemplate jdbcTemplate;

    public LabarugiPrintPdfService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Data cetak per rentang tanggal. Migrasi dari legacy
     * {@code LabarugiController.printLabarugi()} yang memakai
     * {@code report.profit_loss_bydate(...)}.
     */
    public LabarugiPrintData loadPrintData(String from, String to) {
        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select n_row, v_desc, v_acct_name, n_balance "
                        + "from report.profit_loss_bydate(to_date(?, 'yyyy-MM-dd'), to_date(?, 'yyyy-MM-dd')) "
                        + "order by n_row",
                from.trim(), to.trim());
        return build(rows, "DATE RANGE: " + displayDate(from) + " S/D " + displayDate(to));
    }

    /**
     * Data cetak seluruh periode (semua transaksi). Migrasi dari legacy
     * {@code AccountingReport.openCurrentLabarugi()} yang memakai view
     * {@code report.v_profit_loss}.
     */
    public LabarugiPrintData loadPrintAllData() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select n_row, v_desc, v_acct_name, n_balance from report.v_profit_loss order by n_row");
        return build(rows, "DATE RANGE: ");
    }

    private LabarugiPrintData build(List<Map<String, Object>> rows, String dateParam) {
        Map<Long, List<LabarugiPrintData.Line>> grouped = new LinkedHashMap<>();
        Map<Long, String> captionByRow = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            long nRow = toLong(row.get("n_row"));
            String caption = row.get("v_desc") == null ? "" : row.get("v_desc").toString();
            if (!captionByRow.containsKey(nRow)) {
                captionByRow.put(nRow, caption);
            }
            grouped.computeIfAbsent(nRow, key -> new ArrayList<>())
                    .add(new LabarugiPrintData.Line(
                            row.get("v_acct_name") == null ? "" : row.get("v_acct_name").toString(),
                            toDouble(row.get("n_balance"))));
        }
        LabarugiPrintData data = new LabarugiPrintData(dateParam);
        for (Map.Entry<Long, List<LabarugiPrintData.Line>> entry : grouped.entrySet()) {
            data.addGroup(new LabarugiPrintData.Group(
                    captionByRow.get(entry.getKey()), entry.getValue()));
        }
        return data;
    }

    public byte[] generateLabarugiPdf(LabarugiPrintData data) throws Exception {
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("RS. TIARA SELLA\nP/L REPORT",
                new Font(Font.HELVETICA, 12, Font.BOLD)));
        if (data.getDateParam() != null && !data.getDateParam().isEmpty()) {
            document.add(new Paragraph(data.getDateParam(), new Font(Font.HELVETICA, 10)));
        }
        document.add(new Paragraph("Dicetak : "
                + new java.text.SimpleDateFormat("dd-MM-yyyy h:mm a").format(new Date()),
                new Font(Font.HELVETICA, 9)));

        Font normal = new Font(Font.HELVETICA, 9);
        Font bold = new Font(Font.HELVETICA, 9, Font.BOLD);

        for (LabarugiPrintData.Group group : data.getGroups()) {
            document.add(new Paragraph(group.getCaption(), bold));

            for (LabarugiPrintData.Line line : group.getLines()) {
                PdfPTable row = new PdfPTable(2);
                row.setWidthPercentage(100);
                row.setWidths(new float[] { 6f, 2f });
                row.addCell(leftCell(new Phrase(line.getAcctName(), normal)));
                row.addCell(rightCell(new Phrase(MONEY.format(line.getBalance() == null ? 0 : line.getBalance()), normal)));
                document.add(row);
            }
        }

        document.close();
        return out.toByteArray();
    }

    private PdfPCell leftCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPaddingTop(1);
        cell.setPaddingBottom(2);
        return cell;
    }

    private PdfPCell rightCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPaddingTop(1);
        cell.setPaddingBottom(2);
        return cell;
    }

    private String displayDate(String isoDate) {
        String[] parts = isoDate.split("-");
        return parts.length == 3 ? parts[2] + "/" + parts[1] + "/" + parts[0] : isoDate;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
