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
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Generator PDF "MANUAL JOURNAL" untuk tombol CETAK screen SC0199. Migrasi
 * dari legacy {@code JournalEntryController.printToPdf()} yang mencetak report
 * {@code jasper/manual_jurnal.jrxml}.
 */
@Service
public class JournalEntryPrintPdfService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    public byte[] generateManualJournalPdf(JournalEntryPrintData data) throws Exception {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Judul: "Rumah Sakit Tiara Sella Bengkulu" + "MANUAL JOURNAL"
        Font fontBold = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font fontTitle = new Font(Font.HELVETICA, 20);
        PdfPTable title = new PdfPTable(2);
        title.setWidthPercentage(100);
        title.addCell(noBorderCell(new Phrase("Rumah Sakit Tiara Sella Bengkulu", fontBold), Element.ALIGN_LEFT));
        title.addCell(noBorderCell(new Phrase("MANUAL JOURNAL", fontTitle), Element.ALIGN_CENTER));
        document.add(title);

        // Header: Voucher No, Input by, Input Date
        PdfPTable header = new PdfPTable(6);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 2f, 3f, 2f, 3f, 2f, 3f });
        Font fontNormal = new Font(Font.HELVETICA, 12);
        header.addCell(noBorderCell(new Phrase("Voucher No :", fontNormal), Element.ALIGN_LEFT));
        header.addCell(noBorderCell(new Phrase(data.getVoucherNo(), fontNormal), Element.ALIGN_LEFT));
        header.addCell(noBorderCell(new Phrase("Input by :", fontNormal), Element.ALIGN_LEFT));
        header.addCell(noBorderCell(new Phrase(data.getInputBy(), fontNormal), Element.ALIGN_LEFT));
        header.addCell(noBorderCell(new Phrase("Input Date :", fontNormal), Element.ALIGN_LEFT));
        header.addCell(noBorderCell(new Phrase(data.getInputDate(), fontNormal), Element.ALIGN_LEFT));
        document.add(header);

        // Kolom header
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 100f, 195f, 100f, 100f });
        table.addCell(columnHeaderCell(new Phrase("Description", fontBold)));
        table.addCell(columnHeaderCell(new Phrase("Account", fontBold)));
        table.addCell(columnHeaderCell(new Phrase("Debet", fontBold)));
        table.addCell(columnHeaderCell(new Phrase("Credit", fontBold)));

        // Detail baris
        for (JournalEntryPrintData.Line line : data.getLines()) {
            table.addCell(detailCell(new Phrase(line.getDescription(), fontNormal), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase(line.getAccount(), fontNormal), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase(MONEY.format(line.getDebit()), fontNormal), Element.ALIGN_RIGHT));
            table.addCell(detailCell(new Phrase(MONEY.format(line.getCredit()), fontNormal), Element.ALIGN_RIGHT));
        }
        if (data.getLines().isEmpty()) {
            table.addCell(detailCell(new Phrase("Tidak ada data.", fontNormal), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontNormal), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontNormal), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontNormal), Element.ALIGN_LEFT));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private PdfPCell noBorderCell(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPaddingBottom(4);
        return cell;
    }

    private PdfPCell columnHeaderCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setPaddingBottom(4);
        return cell;
    }

    private PdfPCell detailCell(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setPaddingTop(2);
        cell.setPaddingBottom(4);
        return cell;
    }
}
