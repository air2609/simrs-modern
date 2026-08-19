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
import org.springframework.stereotype.Service;

/**
 * Generator PDF LAPORAN TRANSAKSI PASIEN untuk screen RPT0004.
 * Migrasi dari legacy {@code LaporanPoliUgd.createRepport()}
 * (template Jasper poli_ugd.jrxml).
 */
@Service
public class LaporanTransaksiPrintService {

    public byte[] generatePdf(LaporanTransaksiResponse data, String fromDisplay, String toDisplay,
            String username) throws Exception {
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        document.add(new Paragraph("LAPORAN TRANSAKSI " + nvl(data.getUnitName()), titleFont));
        document.add(new Paragraph("PERIODE " + nvl(fromDisplay) + " s.d " + nvl(toDisplay),
                new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph("SUDAH VALIDASI", new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph(nvl(data.getShiftLabel()), new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph(" "));

        String[] columns = { "No.", "NOMOR TRANSAKSI", "NAMA PASIEN", "BIAYA PERIKSA",
                "DOKTER UTAMA", "OBAT & BM", "BIAYA TINDAKAN" };
        float[] widths = { 25f, 90f, 110f, 60f, 110f, 55f, 60f };

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
        for (LaporanTransaksiRowResponse row : data.getRows()) {
            table.addCell(cell(new Phrase(nvl(row.getNomor()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getNomorNota()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getNamaPasien()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(fmt(row.getBiayaPeriksa()), normal), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(nvl(row.getDokterUtama()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(fmt(row.getObatBm()), normal), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(fmt(row.getBiayaTindakan()), normal), Element.ALIGN_RIGHT));
        }
        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Petugas\n \n \n \n ( " + nvl(username) + " )",
                new Font(Font.HELVETICA, 9)));

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
