package com.vone.simrs.cashier;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Generator PDF KWITANSI untuk tombol CETAK screen SC0021. Migrasi dari legacy
 * {@code CashierTransactionController.cetakKwitansi()} + {@code NoteReport}.
 *
 * <p>
 * Layout: kop RS + banner KWITANSI, data pembayaran/pasien, rincian seluruh
 * nota & baris transaksi, ringkasan (subtotal/diskon/pajak/total), rincian
 * pembayaran (tunai/non tunai/deposit/kembali), terbilang, dan tanda tangan.
 */
@Service
public class CashierPrintService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    private static final Color NAVY = new Color(48, 75, 115);
    private static final Color DARK = new Color(55, 65, 81);
    private static final Color GRAY = new Color(107, 114, 128);
    private static final Color LIGHT_GRAY = new Color(240, 243, 247);
    private static final Color LIGHT_GREEN = new Color(226, 245, 233);
    private static final Color LIGHT_YELLOW = new Color(255, 250, 228);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color BORDER = new Color(217, 225, 235);

    private final CashierService cashierService;

    public CashierPrintService(CashierService cashierService) {
        this.cashierService = cashierService;
    }

    public byte[] generateKwitansiPdf(String kwitansiCode) throws Exception {
        CashierBillDetailResponse bill = cashierService.getBillDetailByCode(kwitansiCode);

        Font fontTitle = new Font(Font.HELVETICA, 17, Font.BOLD, NAVY);
        Font fontSubtitle = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY);
        Font fontBanner = new Font(Font.HELVETICA, 13, Font.BOLD, WHITE);
        Font fontSection = new Font(Font.HELVETICA, 10, Font.BOLD, NAVY);
        Font fontLabel = new Font(Font.HELVETICA, 9, Font.BOLD, DARK);
        Font fontNormal = new Font(Font.HELVETICA, 9, Font.NORMAL, DARK);
        Font fontBig = new Font(Font.HELVETICA, 11, Font.BOLD, DARK);
        Font fontItalic = new Font(Font.HELVETICA, 9, Font.ITALIC, DARK);

        Document document = new Document(PageSize.A4, 30, 30, 30, 26);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // ===================== KOP RUMAH SAKIT =====================
        Paragraph title = new Paragraph("RS. TIARA SELLA", fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(2);
        document.add(title);
        Paragraph address = new Paragraph("Jalan Raya Bengkulu - Telp. 0736-20350", fontSubtitle);
        address.setAlignment(Element.ALIGN_CENTER);
        address.setSpacingAfter(10);
        document.add(address);

        // ===================== BANNER KWITANSI =====================
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell bannerCell = new PdfPCell(new Phrase("KWITANSI PEMBAYARAN", fontBanner));
        bannerCell.setBackgroundColor(NAVY);
        bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        bannerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bannerCell.setPadding(9);
        bannerCell.setBorder(PdfPCell.NO_BORDER);
        banner.addCell(bannerCell);
        document.add(banner);
        document.add(space(6));

        // ===================== NO. KWITANSI / TANGGAL =====================
        PdfPTable headInfo = new PdfPTable(2);
        headInfo.setWidthPercentage(100);
        headInfo.setWidths(new float[] { 1f, 1f });
        headInfo.addCell(boxCell(new Phrase("NO. KWITANSI : " + nvl(bill.getBillCode()), fontLabel),
                WHITE, Element.ALIGN_LEFT, PdfPCell.NO_BORDER));
        headInfo.addCell(boxCell(new Phrase("TANGGAL : " + nvl(bill.getDate()), fontLabel),
                WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        document.add(headInfo);
        document.add(space(8));

        // ===================== DATA PEMBAYARAN =====================
        document.add(sectionTitle("DATA PEMBAYARAN"));
        PdfPTable info = new PdfPTable(4);
        info.setWidthPercentage(100);
        info.setWidths(new float[] { 1.25f, 2f, 1.25f, 2f });
        info.addCell(boxCell(new Phrase("NO. MR", fontLabel), LIGHT_GRAY, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase(nvl(bill.getMrCode()), fontNormal), WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase("TIPE PASIEN", fontLabel), LIGHT_GRAY, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase(nvl(bill.getPatientTypeName()), fontNormal), WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));

        info.addCell(boxCell(new Phrase("NAMA PASIEN", fontLabel), LIGHT_GRAY, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase(nvl(bill.getPatientName()), fontNormal), WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase("BED", fontLabel), LIGHT_GRAY, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase(nvl(bill.getBed()), fontNormal), WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));

        info.addCell(boxCell(new Phrase("NAMA PENANGGUNG", fontLabel), LIGHT_GRAY, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase(nvl(bill.getNameOnBill()), fontNormal), WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase("", fontLabel), LIGHT_GRAY, Element.ALIGN_LEFT, PdfPCell.BOX));
        info.addCell(boxCell(new Phrase("", fontNormal), WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));

        info.addCell(boxCell(new Phrase("ALAMAT PENANGGUNG", fontLabel), LIGHT_GRAY, Element.ALIGN_LEFT, PdfPCell.BOX));
        PdfPCell addressCell = boxCell(new Phrase(nvl(bill.getAddrOnBill()), fontNormal), WHITE,
                Element.ALIGN_LEFT, PdfPCell.BOX);
        addressCell.setColspan(3);
        info.addCell(addressCell);
        document.add(info);
        document.add(space(10));

        // ===================== RINCIAN TRANSAKSI =====================
        document.add(sectionTitle("RINCIAN TRANSAKSI"));
        float[] widths = new float[] { 1.3f, 0.9f, 2.6f, 0.6f, 1.1f, 1.2f };
        PdfPTable detail = new PdfPTable(6);
        detail.setWidthPercentage(100);
        detail.setWidths(widths);
        String[] headers = { "NO. NOTA", "KODE", "KETERANGAN", "QTY", "HARGA", "SUBTOTAL" };
        for (String header : headers) {
            PdfPCell headerCell = boxCell(new Phrase(header, fontLabel), LIGHT_GRAY,
                    Element.ALIGN_CENTER, PdfPCell.BOX);
            headerCell.setPadding(5);
            detail.addCell(headerCell);
        }

        List<CashierNoteLineResponse> lines = bill.getLines();
        if (lines == null || lines.isEmpty()) {
            PdfPCell emptyCell = boxCell(new Phrase("TRANSAKSI DEPOSIT / RETUR DEPOSIT", fontNormal),
                    WHITE, Element.ALIGN_LEFT, PdfPCell.BOX);
            emptyCell.setColspan(5);
            detail.addCell(emptyCell);
            detail.addCell(boxCell(new Phrase(money(bill.getTotalPaid()), fontNormal),
                    WHITE, Element.ALIGN_RIGHT, PdfPCell.BOX));
        } else {
            for (CashierNoteLineResponse line : lines) {
                detail.addCell(boxCell(new Phrase(nvl(line.getNoteNo()), fontNormal),
                        WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));
                detail.addCell(boxCell(new Phrase(nvl(line.getCode()), fontNormal),
                        WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));
                detail.addCell(boxCell(new Phrase(nvl(line.getName()), fontNormal),
                        WHITE, Element.ALIGN_LEFT, PdfPCell.BOX));
                detail.addCell(boxCell(new Phrase(line.getQty() == null ? "" : String.valueOf(line.getQty().longValue()), fontNormal),
                        WHITE, Element.ALIGN_RIGHT, PdfPCell.BOX));
                detail.addCell(boxCell(new Phrase(money(line.getPrice()), fontNormal),
                        WHITE, Element.ALIGN_RIGHT, PdfPCell.BOX));
                detail.addCell(boxCell(new Phrase(money(line.getSubtotal()), fontNormal),
                        WHITE, Element.ALIGN_RIGHT, PdfPCell.BOX));
            }
        }
        document.add(detail);
        document.add(space(10));

        // ===================== RINGKASAN =====================
        document.add(sectionTitle("RINGKASAN PEMBAYARAN"));
        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(46);
        summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.setWidths(new float[] { 1.5f, 1f });
        summary.addCell(boxCell(new Phrase("SUBTOTAL", fontLabel), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        summary.addCell(boxCell(new Phrase(money(bill.getSubTotal()), fontNormal), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        summary.addCell(boxCell(new Phrase("DISKON", fontLabel), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        summary.addCell(boxCell(new Phrase(money(bill.getDiscount()), fontNormal), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        summary.addCell(boxCell(new Phrase("PAJAK (PPN)", fontLabel), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        summary.addCell(boxCell(new Phrase(money(bill.getTax()), fontNormal), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        summary.addCell(boxCell(new Phrase("TOTAL DIBAYAR", fontBig), LIGHT_GREEN, Element.ALIGN_RIGHT, PdfPCell.BOX));
        summary.addCell(boxCell(new Phrase(money(bill.getTotalPaid()), fontBig), LIGHT_GREEN, Element.ALIGN_RIGHT, PdfPCell.BOX));
        document.add(summary);
        document.add(space(8));

        // ===================== RINCIAN PEMBAYARAN =====================
        PdfPTable payment = new PdfPTable(2);
        payment.setWidthPercentage(46);
        payment.setHorizontalAlignment(Element.ALIGN_RIGHT);
        payment.setWidths(new float[] { 1.5f, 1f });
        payment.addCell(boxCell(new Phrase("TUNAI", fontLabel), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        payment.addCell(boxCell(new Phrase(money(bill.getCashAmount()), fontNormal), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        payment.addCell(boxCell(new Phrase("NON TUNAI", fontLabel), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        payment.addCell(boxCell(new Phrase(money(bill.getNonCashAmount()), fontNormal), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        payment.addCell(boxCell(new Phrase("DEPOSIT", fontLabel), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        payment.addCell(boxCell(new Phrase(money(bill.getDepositAmount()), fontNormal), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        payment.addCell(boxCell(new Phrase("KEMBALI", fontLabel), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        payment.addCell(boxCell(new Phrase(money(change(bill)), fontNormal), WHITE, Element.ALIGN_RIGHT, PdfPCell.NO_BORDER));
        document.add(payment);
        document.add(space(10));

        // ===================== TERBILANG =====================
        PdfPTable words = new PdfPTable(1);
        words.setWidthPercentage(100);
        PdfPCell wordCell = boxCell(
                new Phrase("TERBILANG : " + terbilang(Math.round(safe(bill.getTotalPaid()))) + " RUPIAH",
                        fontItalic),
                LIGHT_YELLOW, Element.ALIGN_LEFT, PdfPCell.BOX);
        wordCell.setPadding(8);
        words.addCell(wordCell);
        document.add(words);
        document.add(space(12));

        // ===================== TERIMA KASIH =====================
        Paragraph thanks = new Paragraph("TERIMA KASIH", new Font(Font.HELVETICA, 10, Font.BOLD, NAVY));
        thanks.setAlignment(Element.ALIGN_CENTER);
        thanks.setSpacingAfter(24);
        document.add(thanks);

        // ===================== TANDA TANGAN =====================
        PdfPTable signature = new PdfPTable(2);
        signature.setWidthPercentage(100);
        signature.setWidths(new float[] { 1f, 1f });
        signature.addCell(boxCell(new Phrase("PENERIMA / PENANGGUNG", fontLabel),
                WHITE, Element.ALIGN_CENTER, PdfPCell.NO_BORDER));
        signature.addCell(boxCell(new Phrase("KASIR", fontLabel),
                WHITE, Element.ALIGN_CENTER, PdfPCell.NO_BORDER));
        document.add(space(26));
        PdfPTable signatureLine = new PdfPTable(2);
        signatureLine.setWidthPercentage(100);
        signatureLine.setWidths(new float[] { 1f, 1f });
        signatureLine.addCell(boxCell(new Phrase("( " + nvl(bill.getNameOnBill()) + " )", fontNormal),
                WHITE, Element.ALIGN_CENTER, PdfPCell.NO_BORDER));
        signatureLine.addCell(boxCell(new Phrase("( .................... )", fontNormal),
                WHITE, Element.ALIGN_CENTER, PdfPCell.NO_BORDER));
        document.add(signatureLine);

        document.close();
        return out.toByteArray();
    }

    private Paragraph sectionTitle(String text) {
        Paragraph paragraph = new Paragraph(text, new Font(Font.HELVETICA, 10, Font.BOLD, NAVY));
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private Paragraph space(float points) {
        Paragraph paragraph = new Paragraph(" ", new Font(Font.HELVETICA, points, Font.NORMAL));
        paragraph.setSpacingAfter(0);
        return paragraph;
    }

    private PdfPCell boxCell(Phrase phrase, Color background, int alignment, int border) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorder(border);
        cell.setBorderColor(BORDER);
        return cell;
    }

    private double change(CashierBillDetailResponse bill) {
        return safe(bill.getCashAmount()) + safe(bill.getDepositAmount())
                + safe(bill.getNonCashAmount()) - safe(bill.getTotalPaid());
    }

    private String money(Double value) {
        return MONEY.format(safe(value));
    }

    private double safe(Double value) {
        return value == null ? 0 : value;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    // ===================== TERBILANG (ANGKA -> KATA) =====================

    private static final String[] SATUAN = { "", "SATU", "DUA", "TIGA", "EMPAT", "LIMA",
            "ENAM", "TUJUH", "DELAPAN", "SEMBILAN" };
    private static final String[] BELAS = { "SEPULUH", "SEBELAS", "DUA BELAS", "TIGA BELAS",
            "EMPAT BELAS", "LIMA BELAS", "ENAM BELAS", "TUJUH BELAS", "DELAPAN BELAS",
            "SEMBILAN BELAS" };

    private String terbilang(long value) {
        if (value < 0) {
            return "MINUS " + terbilang(-value);
        }
        if (value == 0) {
            return "NOL";
        }
        String result = "";
        long miliar = value / 1_000_000_000L;
        long sisa = value % 1_000_000_000L;
        if (miliar > 0) {
            result = combine(result, terbilangTiga(miliar) + " MILYAR");
        }
        long juta = sisa / 1_000_000L;
        sisa = sisa % 1_000_000L;
        if (juta > 0) {
            result = combine(result, terbilangTiga(juta) + " JUTA");
        }
        long ribu = sisa / 1_000L;
        sisa = sisa % 1_000L;
        if (ribu > 0) {
            result = combine(result, ribu == 1 ? "SERIBU" : terbilangTiga(ribu) + " RIBU");
        }
        if (sisa > 0) {
            result = combine(result, terbilangTiga(sisa));
        }
        return result;
    }

    private String terbilangTiga(long n) {
        String result = "";
        long ratusan = n / 100;
        long sisa = n % 100;
        if (ratusan > 0) {
            result = ratusan == 1 ? "SERATUS" : SATUAN[(int) ratusan] + " RATUS";
        }
        if (sisa == 0) {
            return result;
        }
        if (sisa == 10) {
            return combine(result, "SEPULUH");
        }
        if (sisa >= 11 && sisa <= 19) {
            return combine(result, BELAS[(int) (sisa - 10)]);
        }
        if (sisa >= 20) {
            long puluhan = sisa / 10;
            long satuan = sisa % 10;
            return combine(result, PULUHAN[(int) puluhan]
                    + (satuan > 0 ? " " + SATUAN[(int) satuan] : ""));
        }
        return combine(result, SATUAN[(int) sisa]);
    }

    private static final String[] PULUHAN = { "", "", "DUA PULUH", "TIGA PULUH", "EMPAT PULUH",
            "LIMA PULUH", "ENAM PULUH", "TUJUH PULUH", "DELAPAN PULUH", "SEMBILAN PULUH" };

    private String combine(String left, String right) {
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + " " + right;
    }
}
