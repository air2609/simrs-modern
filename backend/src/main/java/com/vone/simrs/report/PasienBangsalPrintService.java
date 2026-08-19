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
 * Generator PDF LAPORAN PASIEN MASUK RAWAT INAP untuk screen RPT0005.
 * Migrasi dari legacy {@code LaporanPasienBangsalController.createRepport()}
 * (template Jasper laporanPasienBangsal.jrxml).
 */
@Service
public class PasienBangsalPrintService {

    public byte[] generatePdf(PasienBangsalResponse data, String fromDisplay, String toDisplay)
            throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        document.add(new Paragraph("LAPORAN PASIEN MASUK RAWAT INAP", titleFont));
        document.add(new Paragraph("PERIODE : " + fromDisplay + " s.d " + toDisplay,
                new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph(data.getWardName(), new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph(" "));

        String[] columns = { "No.", "No. RM", "No. Registrasi", "Nama Pasien", "Tgl. Daftar",
                "Dirawat", "Jenis Pasien", "Alamat" };
        float[] widths = { 25f, 60f, 90f, 110f, 70f, 80f, 60f, 110f };

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
        for (PasienBangsalRowResponse row : data.getRows()) {
            table.addCell(cell(new Phrase(String.valueOf(row.getNomor()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getNoRm()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getNoRegistrasi()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getNamaPasien()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getTglDaftar()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getNamaBed()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getJenisPasien()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getAlamatPasien()), normal), Element.ALIGN_LEFT));
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
}
