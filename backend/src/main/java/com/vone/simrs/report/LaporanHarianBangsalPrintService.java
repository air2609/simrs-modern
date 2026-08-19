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
 * Generator PDF LAPORAN HARIAN BANGSAL untuk screen RPT0006.
 * Migrasi dari legacy {@code LaporanHarianPasien.createRepport()}
 * (template Jasper laporanHarianBangsal.jrxml).
 */
@Service
public class LaporanHarianBangsalPrintService {

    public byte[] generatePdf(LaporanHarianBangsalResponse data, String fromDisplay,
            String toDisplay) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        document.add(new Paragraph("LAPORAN HARIAN BANGSAL - RINCI", titleFont));
        document.add(new Paragraph("TANGGAL : " + fromDisplay + " s.d. " + toDisplay,
                new Font(Font.HELVETICA, 10)));
        document.add(new Paragraph(" "));

        // header pasien
        PdfPTable header = new PdfPTable(6);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 60f, 130f, 110f, 100f, 80f, 80f });
        Font columnFont = new Font(Font.HELVETICA, 8, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 8);
        addHeaderCell(header, "NO. MR", columnFont);
        addHeaderCell(header, "NAMA", columnFont);
        addHeaderCell(header, "RUANGAN", columnFont);
        addHeaderCell(header, "NO. REGISTRASI", columnFont);
        addHeaderCell(header, "BED", columnFont);
        addHeaderCell(header, "KELAS TARIF", columnFont);
        addValueCell(header, nvl(data.getMrNo()), normal);
        addValueCell(header, nvl(data.getNamaPasien()), normal);
        addValueCell(header, nvl(data.getRuangan()), normal);
        addValueCell(header, nvl(data.getRegNo()), normal);
        addValueCell(header, nvl(data.getBed()), normal);
        addValueCell(header, nvl(data.getKelas()), normal);
        document.add(header);
        document.add(new Paragraph(" "));

        // detail transaksi
        String[] columns = { "NO.", "KODE", "KETERANGAN", "JLH", "HARGA", "NO. NOTA" };
        float[] widths = { 25f, 60f, 200f, 35f, 70f, 100f };
        PdfPTable table = new PdfPTable(columns.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        for (String column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(column, columnFont));
            cell.setBorder(PdfPCell.BOTTOM);
            cell.setPaddingBottom(4);
            table.addCell(cell);
        }
        for (LaporanHarianBangsalRowResponse row : data.getRows()) {
            boolean total = "T  O  T  A  L".equals(row.getKeterangan());
            Font rowFont = total ? columnFont : normal;
            table.addCell(cell(new Phrase(nvl(row.getNomor()), rowFont), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getKodeTransaksi()), rowFont), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getKeterangan()), rowFont), Element.ALIGN_LEFT));
            table.addCell(cell(new Phrase(nvl(row.getJumlah()), rowFont), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(fmt(row.getNilai()), rowFont), Element.ALIGN_RIGHT));
            table.addCell(cell(new Phrase(nvl(row.getNomorTransaksi()), rowFont), Element.ALIGN_LEFT));
        }
        document.add(table);

        document.close();
        return out.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setPaddingBottom(3);
        table.addCell(cell);
    }

    private void addValueCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingBottom(2);
        table.addCell(cell);
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
