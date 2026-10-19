package com.meridian.contract.adapter.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractDocumentReaderTest {

    private static final String W = "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"";

    private final ContractDocumentReader reader = new ContractDocumentReader();

    static byte[] pdf(List<String> lines) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                content.newLineAtOffset(50, 720);
                for (String line : lines) {
                    content.showText(line);
                    content.newLineAtOffset(0, -16);
                }
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    static byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static String paragraph(String text, Integer level) {
        String numbering = level == null ? "" : "<w:pPr><w:numPr><w:ilvl w:val=\"%d\"/><w:numId w:val=\"1\"/></w:numPr></w:pPr>"
                .formatted(level);
        return "<w:p>%s<w:r><w:t>%s</w:t></w:r></w:p>".formatted(numbering, text);
    }

    @Test
    void readsTheTextOfAPdf() throws IOException {
        byte[] file = pdf(List.of("5.1 Liability for loss or damage is limited to 1 SDR per kilogram.",
                "5.2 Hidden damage must be reported within 3 days of delivery."));

        ExtractedDocument document = reader.read("terms.pdf", file);

        assertThat(document.format()).isEqualTo("PDF");
        assertThat(document.text()).isEqualTo("""
                5.1 Liability for loss or damage is limited to 1 SDR per kilogram.
                5.2 Hidden damage must be reported within 3 days of delivery.""");
    }

    @Test
    void wordNumberingIsWrittenBackSoClausesKeepTheirNumbers() throws IOException {
        String document = "<w:document %s><w:body>%s%s%s%s</w:body></w:document>".formatted(W,
                paragraph("Liability", 0),
                paragraph("Liability is limited to 1 SDR per kilogram.", 1),
                paragraph("Claims expire after 6 months.", 1),
                paragraph("Routing", 0));
        String numbering = ("<w:numbering %s><w:abstractNum w:abstractNumId=\"7\">"
                + "<w:lvl w:ilvl=\"0\"><w:numFmt w:val=\"decimal\"/></w:lvl>"
                + "<w:lvl w:ilvl=\"1\"><w:numFmt w:val=\"decimal\"/></w:lvl></w:abstractNum>"
                + "<w:num w:numId=\"1\"><w:abstractNumId w:val=\"7\"/></w:num></w:numbering>").formatted(W);

        ExtractedDocument read = reader.read("terms.docx",
                zip(Map.of("word/document.xml", document, "word/numbering.xml", numbering)));

        assertThat(read.format()).isEqualTo("Word");
        assertThat(read.text()).isEqualTo("""
                1. Liability
                1.1 Liability is limited to 1 SDR per kilogram.
                1.2 Claims expire after 6 months.
                2. Routing""");
    }

    @Test
    void bulletsAreNotTurnedIntoClauseNumbers() throws IOException {
        String document = "<w:document %s><w:body>%s</w:body></w:document>".formatted(W, paragraph("A note", 0));
        String numbering = ("<w:numbering %s><w:abstractNum w:abstractNumId=\"1\"><w:lvl w:ilvl=\"0\">"
                + "<w:numFmt w:val=\"bullet\"/></w:lvl></w:abstractNum>"
                + "<w:num w:numId=\"1\"><w:abstractNumId w:val=\"1\"/></w:num></w:numbering>").formatted(W);

        assertThat(reader.read("notes.docx", zip(Map.of("word/document.xml", document, "word/numbering.xml", numbering)))
                .text()).isEqualTo("A note");
    }

    @Test
    void readsOpenDocumentParagraphs() throws IOException {
        String content = "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" "
                + "xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\"><office:body><office:text>"
                + "<text:p>§ 7: Die Haftung ist auf 2 SZR je kg begrenzt.</text:p></office:text></office:body>"
                + "</office:document-content>";

        ExtractedDocument read = reader.read("agb.odt", zip(Map.of("content.xml", content)));

        assertThat(read.format()).isEqualTo("OpenDocument");
        assertThat(read.text()).isEqualTo("§ 7: Die Haftung ist auf 2 SZR je kg begrenzt.");
    }

    @Test
    void readsPlainTextInUtf8AndWindowsEncoding() {
        assertThat(reader.read("a.txt", "﻿§ 4: Pakete bis 31,5 kg.".getBytes(StandardCharsets.UTF_8)).text())
                .isEqualTo("§ 4: Pakete bis 31,5 kg.");
        assertThat(reader.read("b.txt", "Gebühr für Pakete".getBytes(Charset.forName("windows-1252")))
                .text()).isEqualTo("Gebühr für Pakete");
    }

    @Test
    void refusesFilesItCannotRead() throws IOException {
        assertThatThrownBy(() -> reader.read("old.doc", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0, 1}))
                .hasMessageContaining(".docx or PDF");
        assertThatThrownBy(() -> reader.read("scan.pdf", pdf(List.of())))
                .hasMessageContaining("scan");
        assertThatThrownBy(() -> reader.read("image.png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0}))
                .hasMessageContaining("not supported");
        assertThatThrownBy(() -> reader.read("empty.txt", new byte[0]))
                .hasMessageContaining("empty");
    }

    @Test
    void refusesContractsLongerThanTheCheckReads() {
        byte[] text = "a".repeat(ContractDocumentReader.MAX_CHARACTERS + 1).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> reader.read("long.txt", text)).hasMessageContaining("at most");
    }

    @Test
    void documentTypeDeclarationsAreRefused() throws IOException {
        String document = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                + "<w:document %s><w:body><w:p><w:r><w:t>&e;</w:t></w:r></w:p></w:body></w:document>".formatted(W);

        assertThatThrownBy(() -> reader.read("x.docx", zip(Map.of("word/document.xml", document))))
                .isInstanceOf(DocumentReadException.class);
    }

    @Test
    void wordsSplitAtTheEndOfALineAreJoined() {
        assertThat(reader.read("a.txt", "Eine Haftung für die Überschrei-\ntung der Lieferfrist".getBytes(StandardCharsets.UTF_8))
                .text()).isEqualTo("Eine Haftung für die Überschreitung der Lieferfrist");
        assertThat(reader.read("b.txt", "Paket-\nGebühr".getBytes(StandardCharsets.UTF_8)).text())
                .isEqualTo("Paket-\nGebühr");
    }
}
