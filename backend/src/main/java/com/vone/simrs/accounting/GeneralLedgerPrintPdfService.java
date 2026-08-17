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
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Generator PDF "GENERAL LEDGER REPORT" untuk tombol PRINT / PRINT ALL screen
 * SC0198. Migrasi dari legacy {@code GeneralLedgerController.cetakClick()} /
 * {@code cetakAllClick()} + report {@code jasper/general_ledger.jrxml}
 * (landscape, dikelompokkan per akun).
 */
@Service
public class GeneralLedgerPrintPdfService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.###");

    public byte[] generateGeneralLedgerPdf(GeneralLedgerPrintData data) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Judul: "RS. TIARA SELLA" + "GENERAL LEDGER REPORT" + tanggal cetak
        Font fontTitle = new Font(Font.HELVETICA, 12, Font.BOLD);
        Paragraph title = new Paragraph("RS. TIARA SELLA\nGENERAL LEDGER REPORT", fontTitle);
        document.add(title);
        if (data.getDateParam() != null && !data.getDateParam().isEmpty()) {
            document.add(new Paragraph(data.getDateParam(), new Font(Font.HELVETICA, 10)));
        }
        document.add(new Paragraph("Dicetak : " + new java.text.SimpleDateFormat("dd-MM-yyyy h:mm a").format(new Date()),
                new Font(Font.HELVETICA, 9)));

        String currentAccount = null;
        String previousAccount = null;
        for (GeneralLedgerPrintData.Line line : data.getLines()) {
            currentAccount = line.getAcctName();
            if (!currentAccount.equals(previousAccount)) {
                // Header kelompok akun
                PdfPTable groupHeader = new PdfPTable(1);
                groupHeader.setWidthPercentage(100);
                PdfPCell gh = new PdfPCell(new Phrase("ACCOUNT NAME: " + (line.getAcctName() == null ? "" : line.getAcctName()),
                        new Font(Font.HELVETICA, 10, Font.BOLD)));
                gh.setBorder(PdfPCell.NO_BORDER);
                groupHeader.addCell(gh);
                document.add(groupHeader);

                PdfPTable columns = new PdfPTable(8);
                columns.setWidthPercentage(100);
                columns.setWidths(new float[] { 21f, 108f, 124f, 189f, 73f, 56f, 56f, 77f });
                Font columnFont = new Font(Font.HELVETICA, 9, Font.BOLD);
                columns.addCell(headerCell(new Phrase("NO.", columnFont)));
                columns.addCell(headerCell(new Phrase("BATCH ID.", columnFont)));
                columns.addCell(headerCell(new Phrase("VOUCHER NO.", columnFont)));
                columns.addCell(headerCell(new Phrase("DESCRIPTION", columnFont)));
                columns.addCell(headerCell(new Phrase("DATE", columnFont)));
                columns.addCell(headerCell(new Phrase("DEBIT", columnFont)));
                columns.addCell(headerCell(new Phrase("CREDIT", columnFont)));
                columns.addCell(headerCell(new Phrase("BALANCE", columnFont)));
                document.add(columns);
                previousAccount = currentAccount;
            }

            PdfPTable detail = new PdfPTable(8);
            detail.setWidthPercentage(100);
            detail.setWidths(new float[] { 21f, 108f, 124f, 189f, 73f, 56f, 56f, 77f });
            Font normal = new Font(Font.HELVETICA, 9);
            detail.addCell(detailCell(new Phrase(line.getRow() == null ? "" : String.valueOf(line.getRow()), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(nvl(line.getBatchId()), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(nvl(line.getVoucherNo()), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(nvl(line.getDescription()), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(nvl(line.getAplDate()), normal), Element.ALIGN_LEFT));
            detail.addCell(detailCell(new Phrase(MONEY.format(line.getDebit() == null ? 0 : line.getDebit()), normal), Element.ALIGN_RIGHT));
            detail.addCell(detailCell(new Phrase(MONEY.format(line.getCredit() == null ? 0 : line.getCredit()), normal), Element.ALIGN_RIGHT));
            detail.addCell(detailCell(new Phrase(MONEY.format(line.getBalance() == null ? 0 : line.getBalance()), normal), Element.ALIGN_RIGHT));
            document.add(detail);
        }

        document.close();
        return out.toByteArray();
    }

    private PdfPCell headerCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setPaddingBottom(4);
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
