package com.vone.simrs.report;

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
import java.util.Date;
import org.springframework.stereotype.Service;

/**
 * Generator PDF LAPORAN PENJUALAN PASIEN untuk tombol LIHAT PDF screen RPT0001.
 * Migrasi dari legacy {@code RajalReportingController.createRepport()} /
 * {@code createRanapRepport()} (template Jasper apotik_rajal.jrxml / apotik_ranap.jrxml).
 */
@Service
public class PenjualanReportPrintService {

    private static final String HOSPITAL = "RS. TIARA SELLA";

    public byte[] generatePdf(PenjualanReportResponse data) throws Exception {
        boolean ranap = "RANAP".equalsIgnoreCase(data.getTipe());
        Document document = new Document(ranap ? PageSize.A4.rotate() : PageSize.A4,
                20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        String title = ranap
                ? HOSPITAL + "\nLAPORAN PENJUALAN RAWAT INAP - REKAP"
                : HOSPITAL + "\nLAPORAN PENJUALAN RAWAT JALAN - REKAP";
        document.add(new Paragraph(title, titleFont));
        document.add(new Paragraph("Periode : " + data.getUnitName() + "  Sudah Validasi  "
                + data.getShiftLabel(),
                new Font(Font.HELVETICA, 9)));
        document.add(new Paragraph("Dicetak : "
                + new java.text.SimpleDateFormat("dd-MM-yyyy h:mm a").format(new Date()),
                new Font(Font.HELVETICA, 9)));
        document.add(new Paragraph(" "));

        String[] columns;
        float[] widths;
        if (ranap) {
            columns = new String[] { "NO", "NO. TRANSAKSI", "NO. REGISTRASI", "NO. RESEP",
                    "NAMA PASIEN", "BED", "RUANGAN", "R", "TOTAL", "DISKON", "TOTAL AKHIR" };
            widths = new float[] { 25f, 90f, 70f, 60f, 100f, 70f, 70f, 25f, 55f, 45f, 60f };
        } else {
            columns = new String[] { "NO", "NOMOR NOTA", "NO. RESEP", "NAMA PASIEN",
                    "TOTAL", "DISKON", "PPN", "TOTAL AKHIR" };
            widths = new float[] { 25f, 110f, 60f, 120f, 60f, 45f, 40f, 60f };
        }

        PdfPTable table = new PdfPTable(columns.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        Font columnFont = new Font(Font.HELVETICA, 8, Font.BOLD);
        for (String column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(column, columnFont));
            cell.setBorder(PdfPCell.BOTTOM);
            cell.setPaddingBottom(4);
            table.addCell(cell);
        }

        Font normal = new Font(Font.HELVETICA, 8);
        for (PenjualanReportRowResponse row : data.getRows()) {
            if (ranap) {
                table.addCell(cell(new Phrase(nvl(row.getNo()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getNota()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getReg()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getNoResep()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getPasien()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getBed()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getRuangan()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getR()), normal), Element.ALIGN_RIGHT));
                table.addCell(cell(new Phrase(fmt(row.getTotal()), normal), Element.ALIGN_RIGHT));
                table.addCell(cell(new Phrase(fmt(row.getDiskon()), normal), Element.ALIGN_RIGHT));
                table.addCell(cell(new Phrase(fmt(row.getTotalAkhir()), normal), Element.ALIGN_RIGHT));
            } else {
                table.addCell(cell(new Phrase(nvl(row.getNo()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getNota()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getNoResep()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(nvl(row.getPasien()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(fmt(row.getTotal()), normal), Element.ALIGN_RIGHT));
                table.addCell(cell(new Phrase(fmt(row.getDiskon()), normal), Element.ALIGN_RIGHT));
                table.addCell(cell(new Phrase(fmt(row.getPpn()), normal), Element.ALIGN_RIGHT));
                table.addCell(cell(new Phrase(fmt(row.getTotalAkhir()), normal), Element.ALIGN_RIGHT));
            }
        }
        document.add(table);

        document.close();
        return out.toByteArray();
    }

    private PdfPCell cell(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setPaddingTop(1);
        cell.setPaddingBottom(2);
        return cell;
    }

    private String nvl(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String fmt(Double value) {
        return value == null ? "" : String.format("%.2f", value);
    }
}
