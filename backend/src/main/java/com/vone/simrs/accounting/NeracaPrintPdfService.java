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
 * Generator PDF "BALANCE SHEET REPORT" untuk tombol CETAK screen SC0202.
 * Migrasi dari legacy {@code AccountingReport.openCurrentNeraca()} + report
 * {@code jasper/balance_sheet.jrxml} yang memakai query
 * {@code select * from report.balance_sheet()}.
 */
@Service
public class NeracaPrintPdfService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.###");

    private final JdbcTemplate jdbcTemplate;

    public NeracaPrintPdfService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Bangun data neraca dari {@code report.balance_sheet()}, dikelompokkan
     * per {@code v_desc} (AKTIVA / HUTANG / EQUITY) sesuai group pada
     * {@code balance_sheet.jrxml}.
     */
    public NeracaPrintData loadPrintData() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select v_desc, v_acct_name, n_balance from report.balance_sheet() order by n_row");

        Map<String, List<NeracaPrintData.Line>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String caption = row.get("v_desc") == null ? "" : row.get("v_desc").toString();
            grouped.computeIfAbsent(caption, key -> new ArrayList<>())
                    .add(new NeracaPrintData.Line(
                            row.get("v_acct_name") == null ? "" : row.get("v_acct_name").toString(),
                            toDouble(row.get("n_balance"))));
        }

        NeracaPrintData data = new NeracaPrintData();
        for (Map.Entry<String, List<NeracaPrintData.Line>> entry : grouped.entrySet()) {
            double total = 0;
            for (NeracaPrintData.Line line : entry.getValue()) {
                total += line.getBalance() == null ? 0 : line.getBalance();
            }
            data.addGroup(new NeracaPrintData.Group(entry.getKey(), total, entry.getValue()));
        }
        return data;
    }

    public byte[] generateNeracaPdf() throws Exception {
        return generateNeracaPdf(loadPrintData());
    }

    public byte[] generateNeracaPdf(NeracaPrintData data) throws Exception {
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("RS. TIARA SELLA\nBALANCE SHEET REPORT",
                new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("Dicetak : "
                + new java.text.SimpleDateFormat("dd-MM-yyyy h:mm a").format(new Date()),
                new Font(Font.HELVETICA, 9)));

        Font normal = new Font(Font.HELVETICA, 9);
        Font bold = new Font(Font.HELVETICA, 9, Font.BOLD);

        for (NeracaPrintData.Group group : data.getGroups()) {
            document.add(new Paragraph(group.getCaption(), bold));

            for (NeracaPrintData.Line line : group.getLines()) {
                PdfPTable row = new PdfPTable(2);
                row.setWidthPercentage(100);
                row.setWidths(new float[] { 6f, 2f });
                row.addCell(leftCell(new Phrase(line.getAcctName(), normal)));
                row.addCell(rightCell(new Phrase(MONEY.format(line.getBalance() == null ? 0 : line.getBalance()), normal)));
                document.add(row);
            }

            PdfPTable footer = new PdfPTable(2);
            footer.setWidthPercentage(100);
            footer.setWidths(new float[] { 6f, 2f });
            footer.addCell(leftCell(new Phrase("TOTAL " + group.getCaption(), bold)));
            footer.addCell(rightCell(new Phrase(MONEY.format(group.getTotal()), bold)));
            document.add(footer);
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
