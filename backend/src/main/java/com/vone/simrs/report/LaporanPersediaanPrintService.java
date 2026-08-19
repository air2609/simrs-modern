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
 * Generator PDF LAPORAN PERSEDIAAN OBAT-BAHAN MEDIS untuk screen RPT0008.
 * Migrasi dari legacy {@code PersedianObat.createReport()}
 * (template Jasper persediaan_obat.jrxml).
 */
@Service
public class LaporanPersediaanPrintService {

    public byte[] generatePdf(LaporanPersediaanResponse data) throws Exception {
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        document.add(new Paragraph("RUMAH SAKIT TIARA SELLA", titleFont));
        document.add(new Paragraph("LAPORAN PERSEDIAAN OBAT dan BAHAN MEDIS", titleFont));
        document.add(new Paragraph("BAGIAN : " + nvl(data.getUnitName()),
                new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph("BULAN : " + nvl(data.getPeriodeLabel()),
                new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph(" "));

        String[] columns = { "NO.", "KODE", "NAMA", "HRG STANDAR", "JLH", "SATUAN" };
        float[] widths = { 30f, 70f, 180f, 80f, 50f, 60f };

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
        for (LaporanPersediaanRowResponse row : data.getRows()) {
            table.addCell(cell(new Phrase(nvl(row.getNomor()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getKodeObat()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getNamaObat()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(fmt(row.getHargaStandar()), normal), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(fmt(row.getJumlah()), normal), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(nvl(row.getSatuan()), normal), Element.ALIGN_LEFT));
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
