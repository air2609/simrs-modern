package com.vone.simrs.cashier;

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
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Generator PDF KWITANSI untuk tombol CETAK screen SC0021. Migrasi dari legacy
 * {@code CashierTransactionController.cetakKwitansi()} (kwitansi pasien).
 */
@Service
public class CashierPrintService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.###");

    private final JdbcTemplate jdbcTemplate;

    public CashierPrintService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] generateKwitansiPdf(String kwitansiCode) throws Exception {
        BillRow bill = findBill(kwitansiCode);
        if (bill == null) {
            throw new IllegalArgumentException("Kwitansi tidak ditemukan!");
        }

        Document document = new Document(PageSize.A4, 25, 25, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font bold = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 9);

        document.add(new Paragraph("RS. TIARA SELLA\nKWITANSI PEMBAYARAN", bold));
        document.add(new Paragraph("NO. KWITANSI : " + kwitansiCode, normal));
        document.add(new Paragraph("TANGGAL : " + (bill.date == null ? "" : bill.date), normal));
        document.add(new Paragraph(" "));

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setWidths(new float[] { 3f, 7f });
        info.addCell(labelCell(new Phrase("NAMA PENANGGUNG", bold)));
        info.addCell(valueCell(new Phrase(nvl(bill.nameOnBill), normal)));
        info.addCell(labelCell(new Phrase("ALAMAT", bold)));
        info.addCell(valueCell(new Phrase(nvl(bill.addrOnBill), normal)));
        document.add(info);
        document.add(new Paragraph(" "));

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);
        totals.setWidths(new float[] { 3f, 7f });
        totals.addCell(labelCell(new Phrase("SUBTOTAL", bold)));
        totals.addCell(valueCell(new Phrase(MONEY.format(bill.subtotal == null ? 0 : bill.subtotal), normal)));
        totals.addCell(labelCell(new Phrase("DISKON", bold)));
        totals.addCell(valueCell(new Phrase(MONEY.format(bill.discount == null ? 0 : bill.discount), normal)));
        totals.addCell(labelCell(new Phrase("PAJAK (PPN)", bold)));
        totals.addCell(valueCell(new Phrase(MONEY.format(bill.tax == null ? 0 : bill.tax), normal)));
        totals.addCell(labelCell(new Phrase("TOTAL DIBAYAR", bold)));
        totals.addCell(valueCell(new Phrase(MONEY.format(bill.totalPaid == null ? 0 : bill.totalPaid), bold)));
        document.add(totals);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("TERIMA KASIH", normal));

        document.close();
        return out.toByteArray();
    }

    private BillRow findBill(String kwitansiCode) {
        List<BillRow> rows = jdbcTemplate.query(
                "select v_pbill_code, v_name_on_bill, v_addr_on_bill, n_pbill_sub_ttl, "
                        + "n_pbill_ttl_paid, n_pbill_disc, n_pbill_tax, d_whn_create "
                        + "from tb_patient_bill where v_pbill_code = ?",
                (resultSet, rowNum) -> new BillRow(
                        resultSet.getString("v_pbill_code"),
                        resultSet.getString("v_name_on_bill"),
                        resultSet.getString("v_addr_on_bill"),
                        toDouble(resultSet.getObject("n_pbill_sub_ttl")),
                        toDouble(resultSet.getObject("n_pbill_ttl_paid")),
                        resultSet.getObject("n_pbill_disc") == null ? null
                                : resultSet.getDouble("n_pbill_disc"),
                        resultSet.getObject("n_pbill_tax") == null ? null
                                : resultSet.getDouble("n_pbill_tax"),
                        resultSet.getTimestamp("d_whn_create") == null ? ""
                                : resultSet.getTimestamp("d_whn_create").toLocalDateTime().toString()),
                kwitansiCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private PdfPCell labelCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPaddingTop(2);
        cell.setPaddingBottom(2);
        return cell;
    }

    private PdfPCell valueCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPaddingTop(2);
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

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private static final class BillRow {
        private final String code;
        private final String nameOnBill;
        private final String addrOnBill;
        private final Double subtotal;
        private final Double totalPaid;
        private final Double discount;
        private final Double tax;
        private final String date;

        private BillRow(String code, String nameOnBill, String addrOnBill, Double subtotal,
                Double totalPaid, Double discount, Double tax, String date) {
            this.code = code;
            this.nameOnBill = nameOnBill;
            this.addrOnBill = addrOnBill;
            this.subtotal = subtotal;
            this.totalPaid = totalPaid;
            this.discount = discount;
            this.tax = tax;
            this.date = date;
        }
    }
}
