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
 * Generator PDF LAPORAN PENDAFTARAN untuk tombol cetak screen RPT0010.
 * Migrasi dari legacy {@code LaporanPendaftaran.cetak()/createRekap()}.
 */
@Service
public class LaporanPendaftaranPrintService {

    public byte[] generatePdf(LaporanPendaftaranResponse data) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        document.add(new Paragraph("RS. TIARA SELLA\nLAPORAN PENDAFTARAN", titleFont));
        document.add(new Paragraph("Dicetak : "
                + new java.text.SimpleDateFormat("dd-MM-yyyy h:mm a").format(new Date()),
                new Font(Font.HELVETICA, 9)));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 60f, 160f, 60f, 60f, 50f, 50f, 60f });
        Font columnFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 9);
        String[] columns = { "TANGGAL", "UNIT", "LAKI-LAKI", "PEREMPUAN", "LAMA", "BARU", "TOTAL" };
        for (String column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(column, columnFont));
            cell.setBorder(PdfPCell.BOTTOM);
            cell.setPaddingBottom(4);
            table.addCell(cell);
        }
        for (LaporanPendaftaranRowResponse row : data.getRows()) {
            table.addCell(cell(new Phrase(nvl(row.getTanggal()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getUnit()), normal), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(String.valueOf(row.getLakiLaki() == null ? 0 : row.getLakiLaki()), normal), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(String.valueOf(row.getPerempuan() == null ? 0 : row.getPerempuan()), normal), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(String.valueOf(row.getLama() == null ? 0 : row.getLama()), normal), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(String.valueOf(row.getBaru() == null ? 0 : row.getBaru()), normal), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(String.valueOf(row.getTotal() == null ? 0 : row.getTotal()), normal), Element.ALIGN_RIGHT));
        }
        // baris total
        table.addCell(cell(new Phrase("", normal), Element.ALIGN_LEFT));
        table.addCell(cell(new Phrase("T O T A L", columnFont), Element.ALIGN_LEFT));
        table.addCell(cell(new Phrase(String.valueOf(data.getTotalLakiLaki()), columnFont), Element.ALIGN_RIGHT));
        table.addCell(cell(new Phrase(String.valueOf(data.getTotalPerempuan()), columnFont), Element.ALIGN_RIGHT));
        table.addCell(cell(new Phrase(String.valueOf(data.getTotalLama()), columnFont), Element.ALIGN_RIGHT));
        table.addCell(cell(new Phrase(String.valueOf(data.getTotalBaru()), columnFont), Element.ALIGN_RIGHT));
        table.addCell(cell(new Phrase(String.valueOf(data.getTotalKeseluruhan()), columnFont), Element.ALIGN_RIGHT));
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

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
