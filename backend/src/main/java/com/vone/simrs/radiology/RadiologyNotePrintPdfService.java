package com.vone.simrs.radiology;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vone.simrs.ward.WardNoteDetailResponse;
import com.vone.simrs.ward.WardNoteLineResponse;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Generator PDF NOTA RADIOLOGI untuk tombol CETAK screen SC0051. Migrasi dari
 * legacy {@code RadiologyController.cetakClick()} + {@code NoteReport}.
 */
@Service
public class RadiologyNotePrintPdfService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.###");

    private final RadiologyService radiologyService;

    public RadiologyNotePrintPdfService(RadiologyService radiologyService) {
        this.radiologyService = radiologyService;
    }

    public byte[] generateNotePdf(Integer noteId) throws Exception {
        WardNoteDetailResponse note = radiologyService.getNoteDetail(noteId);

        Document document = new Document(PageSize.A4, 25, 25, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("RS. TIARA SELLA", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("NOTA TRANSAKSI RADIOLOGI", new Font(Font.HELVETICA, 11, Font.BOLD)));
        document.add(new Paragraph("NO. NOTA : " + note.getNoteNo(), new Font(Font.HELVETICA, 10, Font.BOLD)));
        document.add(new Paragraph("TGL : " + new java.text.SimpleDateFormat("dd-MM-yyyy").format(new Date()),
                new Font(Font.HELVETICA, 9)));
        document.add(new Paragraph(" "));

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 9);

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setWidths(new float[] { 3f, 7f });
        info.addCell(labelCell(new Phrase("NO. MR", labelFont)));
        info.addCell(valueCell(new Phrase(nvl(note.getMrCode()), normal)));
        info.addCell(labelCell(new Phrase("NAMA PASIEN", labelFont)));
        info.addCell(valueCell(new Phrase(nvl(note.getPatientName()), normal)));
        info.addCell(labelCell(new Phrase("RUANGAN / BED", labelFont)));
        info.addCell(valueCell(new Phrase(nvl(note.getHall()) + " - " + nvl(note.getBed()), normal)));
        info.addCell(labelCell(new Phrase("NO. REGISTRASI", labelFont)));
        info.addCell(valueCell(new Phrase(nvl(note.getRegistrationNumber()), normal)));
        document.add(info);
        document.add(new Paragraph(" "));

        PdfPTable header = new PdfPTable(6);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 26f, 140f, 30f, 40f, 55f, 55f });
        String[] columns = { "KODE", "KETERANGAN", "QTY", "SATUAN", "HARGA", "SUBTOTAL" };
        for (String column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(column, labelFont));
            cell.setBorder(PdfPCell.BOTTOM);
            cell.setPaddingBottom(4);
            header.addCell(cell);
        }
        document.add(header);

        List<WardNoteLineResponse> lines = note.getLines();
        for (WardNoteLineResponse line : lines) {
            PdfPTable row = new PdfPTable(6);
            row.setWidthPercentage(100);
            row.setWidths(new float[] { 26f, 140f, 30f, 40f, 55f, 55f });
            row.addCell(detailCell(new Phrase(nvl(line.getCode()), normal), Element.ALIGN_LEFT));
            row.addCell(detailCell(new Phrase(nvl(line.getName()), normal), Element.ALIGN_LEFT));
            row.addCell(detailCell(new Phrase(line.getQty() == null ? "" : String.valueOf(line.getQty().longValue()), normal), Element.ALIGN_RIGHT));
            row.addCell(detailCell(new Phrase(nvl(line.getUnit()), normal), Element.ALIGN_LEFT));
            row.addCell(detailCell(new Phrase(MONEY.format(line.getPrice() == null ? 0 : line.getPrice()), normal), Element.ALIGN_RIGHT));
            row.addCell(detailCell(new Phrase(MONEY.format(line.getSubtotal() == null ? 0 : line.getSubtotal()), normal), Element.ALIGN_RIGHT));
            document.add(row);
        }

        PdfPTable totalRow = new PdfPTable(6);
        totalRow.setWidthPercentage(100);
        totalRow.setWidths(new float[] { 26f, 140f, 30f, 40f, 55f, 55f });
        totalRow.addCell(labelCell(new Phrase("TOTAL", labelFont)));
        totalRow.addCell(valueCell(new Phrase("")));
        totalRow.addCell(valueCell(new Phrase("")));
        totalRow.addCell(valueCell(new Phrase("")));
        totalRow.addCell(valueCell(new Phrase("")));
        totalRow.addCell(valueCell(new Phrase(MONEY.format(note.getTotal() == null ? 0 : note.getTotal()), labelFont)));
        document.add(totalRow);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("STATUS NOTA : " + note.getStatusLabel(), labelFont));

        document.close();
        return out.toByteArray();
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
}
