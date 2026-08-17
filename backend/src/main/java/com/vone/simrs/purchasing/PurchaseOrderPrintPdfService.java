package com.vone.simrs.purchasing;

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
 * Generator PDF "PURCHASE ORDER" untuk tombol CETAK screen SC0193.
 *
 * <p>
 * Migrasi dari legacy {@code POController.cetakPO()} yang mencetak report
 * {@code jasper/orderPembelian.jrxml} (landscape): judul PURCHASE ORDER,
 * header NO PO + SUPPLIER, tabel NAMA ITEM/OBAT | SATUAN REQ | JUMLAH REQ |
 * SATUAN BELI | JUMLAH BELI | HARGA SATUAN | SUBTOTAL, dan footer REQUESTOR.
 */
@Service
public class PurchaseOrderPrintPdfService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    /**
     * Bangun dokumen PDF PURCHASE ORDER dalam memori.
     */
    public byte[] generatePoPdf(PurchaseOrderPrintData data) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Judul PURCHASE ORDER (band title jrxml: y=24, height 28, font 20)
        Font fontTitle = new Font(Font.HELVETICA, 20);
        document.add(new Paragraph("PURCHASE ORDER", fontTitle));

        // Header NO PO + SUPPLIER (pageHeader band: y=15)
        Font fontBold = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font fontNormal = new Font(Font.HELVETICA, 12);
        PdfPTable header = new PdfPTable(4);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 1f, 3f, 1f, 5f });
        header.addCell(noBorderCell(new Phrase("NO PO : ", fontBold), Element.ALIGN_LEFT));
        header.addCell(noBorderCell(new Phrase(data.getPoCode(), fontNormal), Element.ALIGN_LEFT));
        header.addCell(noBorderCell(new Phrase("SUPPLIER :", fontBold), Element.ALIGN_LEFT));
        header.addCell(noBorderCell(new Phrase(data.getSupplier(), fontNormal), Element.ALIGN_LEFT));
        document.add(header);

        // Kolom header tabel
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 100f, 93f, 50f, 100f, 48f, 100f, 50f });

        Font fontColumn = new Font(Font.HELVETICA, 12, Font.BOLD);
        table.addCell(columnHeaderCell(new Phrase("NAMA ITEM/OBAT", fontColumn)));
        table.addCell(columnHeaderCell(new Phrase("SATUAN REQ", fontColumn)));
        table.addCell(columnHeaderCell(new Phrase("JUMLAH REQ", fontColumn)));
        table.addCell(columnHeaderCell(new Phrase("SATUAN BELI", fontColumn)));
        table.addCell(columnHeaderCell(new Phrase("JUMLAH BELI", fontColumn)));
        table.addCell(columnHeaderCell(new Phrase("HARGA SATUAN", fontColumn)));
        table.addCell(columnHeaderCell(new Phrase("SUBTOTAL", fontColumn)));

        // Detail baris
        Font fontDetail = new Font(Font.HELVETICA, 12);
        for (PurchaseOrderPrintData.Line line : data.getLines()) {
            table.addCell(detailCell(new Phrase(line.getItem(), fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase(line.getSatuanRequest(), fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase(String.valueOf(line.getQuantityRequest()), fontDetail), Element.ALIGN_CENTER));
            table.addCell(detailCell(new Phrase(line.getSatuanRealisasi(), fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase(String.valueOf(line.getQuantityRealisasi()), fontDetail), Element.ALIGN_CENTER));
            table.addCell(detailCell(new Phrase(MONEY.format(line.getHargaSatuan()), fontDetail), Element.ALIGN_CENTER));
            table.addCell(detailCell(new Phrase(MONEY.format(line.getSubtotal()), fontDetail), Element.ALIGN_CENTER));
        }

        if (data.getLines().isEmpty()) {
            table.addCell(detailCell(new Phrase("Tidak ada data.", fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontDetail), Element.ALIGN_LEFT));
            table.addCell(detailCell(new Phrase("", fontDetail), Element.ALIGN_LEFT));
        }

        document.add(table);

        // Footer REQUESTOR (columnFooter band)
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        PdfPCell footerCell = new PdfPCell(new Phrase("REQUESTOR", fontBold));
        footerCell.setBorder(PdfPCell.NO_BORDER);
        footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footer.addCell(footerCell);
        PdfPCell requestorCell = new PdfPCell(new Phrase(data.getRequestor(), fontNormal));
        requestorCell.setBorder(PdfPCell.NO_BORDER);
        requestorCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footer.addCell(requestorCell);
        document.add(footer);

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
