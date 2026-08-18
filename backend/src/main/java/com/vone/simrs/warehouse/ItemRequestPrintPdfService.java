package com.vone.simrs.warehouse;

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
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Generator PDF "HISTORY PERMINTAAN O-BM" untuk tombol CETAK tab HISTORY
 * screen SC0174. Migrasi dari legacy {@code HistoryRequestController.cetak()}
 * yang memakai view {@code report.v_history_request}.
 */
@Service
public class ItemRequestPrintPdfService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    private final JdbcTemplate jdbcTemplate;

    public ItemRequestPrintPdfService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] generateHistoryPdf(Integer sourceWarehouseId, String from, String to)
            throws Exception {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        java.sql.Timestamp start = java.sql.Timestamp.valueOf(fromDate.atStartOfDay());
        java.sql.Timestamp end = java.sql.Timestamp.valueOf(toDate.atTime(23, 59, 59));

        List<HistoryRow> rows = jdbcTemplate.query(
                "select v_request_code, v_who_create, whasal, whtarget, n_qty_req, "
                        + "n_qty_sent, v_mitem_end_quantify, v_item_name, d_whn_create "
                        + "from report.v_history_request "
                        + "where n_source_whouse_id = ? and d_whn_create between ? and ? "
                        + "order by v_request_code, d_whn_create",
                (resultSet, rowNum) -> new HistoryRow(
                        resultSet.getString("v_request_code"),
                        resultSet.getString("v_item_name"),
                        resultSet.getString("whasal"),
                        resultSet.getString("whtarget"),
                        resultSet.getString("v_mitem_end_quantify"),
                        resultSet.getInt("n_qty_req"),
                        resultSet.getInt("n_qty_sent"),
                        resultSet.getTimestamp("d_whn_create")),
                sourceWarehouseId, start, end);

        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("RS. TIARA SELLA\nHISTORY PERMINTAAN O-BM",
                new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("TGL: " + from + " S/D " + to,
                new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph("Dicetak : "
                + new java.text.SimpleDateFormat("dd-MM-yyyy h:mm a").format(new Date()),
                new Font(Font.HELVETICA, 9)));
        document.add(new Paragraph(" "));

        PdfPTable header = new PdfPTable(7);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 60f, 120f, 80f, 80f, 60f, 50f, 80f });
        Font columnFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        String[] columns = { "NO. PERMINTAAN", "NAMA ITEM", "GUDANG ASAL",
                "GUDANG TUJUAN", "SATUAN", "ORDER", "TERIMA" };
        for (String column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(column, columnFont));
            cell.setBorder(PdfPCell.BOTTOM);
            cell.setPaddingBottom(4);
            header.addCell(cell);
        }
        document.add(header);

        Font normal = new Font(Font.HELVETICA, 9);
        for (HistoryRow row : rows) {
            PdfPTable detail = new PdfPTable(7);
            detail.setWidthPercentage(100);
            detail.setWidths(new float[] { 60f, 120f, 80f, 80f, 60f, 50f, 80f });
            detail.addCell(detailCell(new Phrase(nvl(row.requestCode), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(nvl(row.itemName), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(nvl(row.sourceName), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(nvl(row.targetName), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(nvl(row.unit), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(String.valueOf(row.qtyReq), normal), Element.ALIGN_RIGHT));
            detail.addCell(detailCell(new Phrase(String.valueOf(row.qtySent), normal), Element.ALIGN_RIGHT));
            document.add(detail);
        }

        document.close();
        return out.toByteArray();
    }

    private PdfPCell detailCell(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setPaddingTop(1);
        cell.setPaddingBottom(2);
        return cell;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private static final class HistoryRow {
        private final String requestCode;
        private final String itemName;
        private final String sourceName;
        private final String targetName;
        private final String unit;
        private final int qtyReq;
        private final int qtySent;

        private HistoryRow(String requestCode, String itemName, String sourceName,
                String targetName, String unit, int qtyReq, int qtySent,
                java.sql.Timestamp createdAt) {
            this.requestCode = requestCode;
            this.itemName = itemName;
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.unit = unit;
            this.qtyReq = qtyReq;
            this.qtySent = qtySent;
        }
    }
}
