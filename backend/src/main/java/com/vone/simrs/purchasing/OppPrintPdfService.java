package com.vone.simrs.purchasing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Generator PDF "SURAT PEMESANAN" (OPP) untuk tombol PRINT pada screen SC0192.
 *
 * <p>
 * Migrasi 1:1 dari legacy {@code PORApproval.createOppPdf()} (iText/OpenPDF,
 * package {@code com.lowagie.text}). Isi surat mengikuti alur legacy:
 * header (logo + identitas instalasi farmasi), judul SURAT PEMESANAN, pemohon,
 * supplier, tabel item OPP, keperluan, tanggal, kolom mengetahui/menyetujui,
 * dan tanda tangan (Apoteker, Adm. Pengadaan, Direktur).
 */
@Service
public class OppPrintPdfService {

    private final Properties messages = new Properties();

    public OppPrintPdfService() throws IOException {
        try (InputStream in = new ClassPathResource("opp/messages.properties").getInputStream()) {
            messages.load(in);
        }
    }

    /**
     * Bangun dokumen PDF OPP dalam memori. Migrasi dari legacy
     * {@code PORApproval.createOppPdf()}.
     */
    public byte[] generateOppPdf(PurchaseRequestApprovalDetailResponse detail) throws Exception {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(createHeader());

        Paragraph par = new Paragraph();
        addEmptyLine(par, 3);
        document.add(par);

        document.add(createTitle(detail.getPrCode()));

        par = new Paragraph();
        addEmptyLine(par, 2);
        document.add(par);

        par = new Paragraph("Yang bertanda tangan dibawah ini :", new Font(Font.TIMES_ROMAN, 12));
        addEmptyLine(par, 2);
        document.add(par);

        document.add(createRequestor());

        par = new Paragraph();
        addEmptyLine(par, 1);
        document.add(par);

        par = new Paragraph("Mengajukan permohonan kepada :", new Font(Font.TIMES_ROMAN, 12));
        addEmptyLine(par, 1);
        document.add(par);

        document.add(getSupplier(detail));

        par = new Paragraph();
        addEmptyLine(par, 2);
        document.add(par);

        document.add(getOppData(detail.getItems()));

        par = new Paragraph();
        addEmptyLine(par, 1);
        document.add(par);

        par = new Paragraph("Untuk keperluan Instalasi Farmasi " + message("hospital.name.short")
                + " dengan alamat " + message("hospital.address2") + " " + message("hospital.city") + ".",
                new Font(Font.TIMES_ROMAN, 12));
        addEmptyLine(par, 2);
        document.add(par);

        document.add(createDate());

        par = new Paragraph();
        addEmptyLine(par, 1);
        document.add(par);

        document.add(createMengetahui());

        par = new Paragraph();
        addEmptyLine(par, 4);
        document.add(par);

        document.add(createSignature());

        document.close();
        return out.toByteArray();
    }

    /**
     * Header surat: logo + identitas instalasi farmasi. Migrasi dari legacy
     * {@code PORApproval.createHeader()} + {@code getHeaderInformation()}.
     */
    private PdfPTable createHeader() throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 1, 2 });

        Image img = Image.getInstance(new ClassPathResource("opp/" + message("logo.file")).getURL());
        PdfPCell cell = new PdfPCell(img, true);
        cell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(cell);

        cell = new PdfPCell(getHeaderInformation());
        cell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(cell);

        return table;
    }

    private PdfPTable getHeaderInformation() {
        Font font18b = new Font(Font.TIMES_ROMAN, 18, Font.BOLD);
        Font font10b = new Font(Font.TIMES_ROMAN, 10, Font.BOLD);

        PdfPTable table = new PdfPTable(1);
        table.addCell(centeredCell(new Phrase("INSTALASI FARMASI", font18b)));
        table.addCell(centeredCell(new Phrase(message("hospital.name"), font18b)));
        table.addCell(centeredCell(new Phrase(message("hospital.address"), font10b)));
        table.addCell(centeredCell(new Phrase(message("hospital.phone"), font10b)));
        return table;
    }

    /**
     * Judul surat. Migrasi dari legacy {@code PORApproval.createTitle()}.
     */
    private PdfPTable createTitle(String prCode) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        Font font12b = new Font(Font.TIMES_ROMAN, 12, Font.BOLD | Font.UNDERLINE);
        Font font10 = new Font(Font.TIMES_ROMAN, 10);

        table.addCell(centeredCell(new Phrase("SURAT PEMESANAN", font12b)));
        table.addCell(centeredCell(new Phrase("No. " + prCode, font10)));
        return table;
    }

    /**
     * Data pemohon (apoteker). Migrasi dari legacy
     * {@code PORApproval.createRequestor()}.
     */
    private PdfPTable createRequestor() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 1, 6 });

        Font font12b = new Font(Font.TIMES_ROMAN, 12);
        table.addCell(leftCell(new Phrase("Nama", font12b)));
        table.addCell(leftCell(new Phrase(message("nama.apoteker"), font12b)));
        table.addCell(leftCell(new Phrase("Alamat", font12b)));
        table.addCell(leftCell(new Phrase(message("alamat.apoteker"), font12b)));
        table.addCell(leftCell(new Phrase("Jabatan", font12b)));
        table.addCell(leftCell(new Phrase(message("jabatan.apoteker"), font12b)));
        return table;
    }

    /**
     * Data supplier tujuan. Migrasi dari legacy
     * {@code PORApproval.getSupplier()}.
     */
    private PdfPTable getSupplier(PurchaseRequestApprovalDetailResponse detail) {
        PdfPTable table = new PdfPTable(2);
        Font font12b = new Font(Font.TIMES_ROMAN, 12);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.5f, 5.5f });

        table.addCell(leftCell(new Phrase("Nama Perusahaan", font12b)));
        table.addCell(leftCell(new Phrase(": " + detail.getSupplierName(), font12b)));
        table.addCell(leftCell(new Phrase("Alamat", font12b)));
        table.addCell(leftCell(new Phrase(": " + detail.getSupplierAddress(), font12b)));
        table.addCell(leftCell(new Phrase("No. Telp", font12b)));
        table.addCell(leftCell(new Phrase(": " + detail.getSupplierTelp(), font12b)));
        return table;
    }

    /**
     * Tabel item OPP. Migrasi dari legacy {@code PORApproval.getOppData()}.
     */
    private PdfPTable getOppData(List<PurchaseRequestApprovalItemResponse> items) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 1, 2, 6, 2, 2 });

        Font font10b = new Font(Font.TIMES_ROMAN, 12, Font.BOLD);
        table.addCell(centeredCell(new Phrase("No", font10b)));
        table.addCell(centeredCell(new Phrase("Kode Obat", font10b)));
        table.addCell(centeredCell(new Phrase("Nama Obat", font10b)));
        table.addCell(centeredCell(new Phrase("Jumlah \n Pemesanan", font10b)));
        table.addCell(centeredCell(new Phrase("Satuan", font10b)));

        Font font10n = new Font(Font.TIMES_ROMAN, 12, Font.NORMAL);
        int nomor = 1;
        for (PurchaseRequestApprovalItemResponse item : items) {
            table.addCell(centeredCell(new Phrase(nomor + "", font10n)));
            table.addCell(leftCell(new Phrase(item.getItemCode(), font10n)));
            table.addCell(leftCell(new Phrase(item.getItemName(), font10n)));
            table.addCell(rightCell(new Phrase(item.getQtyRequested() == null ? "" : item.getQtyRequested().toString(), font10n)));
            table.addCell(leftCell(new Phrase(item.getMeasurementCode(), font10n)));
            nomor = nomor + 1;
        }
        return table;
    }

    /**
     * Baris tanggal. Migrasi dari legacy {@code PORApproval.createDate()}.
     */
    private PdfPTable createDate() {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 7, 2, 1 });

        Font font10b = new Font(Font.TIMES_ROMAN, 12);
        PdfPCell cell = new PdfPCell(new Phrase(message("hospital.city") + ",", font10b));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("", font10b));
        cell.setBorder(PdfPCell.BOTTOM);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("20", font10b));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
        return table;
    }

    /**
     * Kolom Penanggung Jawab / Mengetahui / Menyetujui. Migrasi dari legacy
     * {@code PORApproval.createMengetahui()}.
     */
    private PdfPTable createMengetahui() {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 1, 1, 1, 1 });

        Font font10b = new Font(Font.TIMES_ROMAN, 8);
        table.addCell(centeredCell(new Phrase("Penanggung Jawab", font10b)));
        table.addCell(centeredCell(new Phrase("Mengetahui", font10b)));

        PdfPCell cell = new PdfPCell(new Phrase("Menyetujui", font10b));
        cell.setColspan(2);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(cell);
        return table;
    }

    /**
     * Blok tanda tangan (Apoteker, Adm. Pengadaan, Direktur + SIPA). Migrasi
     * dari legacy {@code PORApproval.createSignature()}.
     */
    private PdfPTable createSignature() {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1, 1, 1, 1 });

        Font font10b = new Font(Font.TIMES_ROMAN, 8, Font.UNDERLINE);
        Font font10n = new Font(Font.TIMES_ROMAN, 8);
        Font fontSIPA = new Font(Font.TIMES_ROMAN, 6);

        table.addCell(centeredCell(new Phrase(message("hospital.apoteker"), font10b)));
        table.addCell(centeredCell(new Phrase(message("hospital.purchasing.admin.name"), font10b)));
        table.addCell(centeredCell(new Phrase(message("hospital.director.name"), font10b)));
        table.addCell(emptyCell());

        table.addCell(centeredCell(new Phrase(message("sipa.apoteker"), fontSIPA)));
        table.addCell(centeredCell(new Phrase(message("hospital.purchasing.admin"), font10n)));
        table.addCell(centeredCell(new Phrase(message("hospital.director"), font10n)));
        table.addCell(emptyCell());
        return table;
    }

    private void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }

    private PdfPCell centeredCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell leftCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell rightCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        return cell;
    }

    private String message(String key) {
        String value = messages.getProperty(key);
        return value == null ? "" : value.trim();
    }
}
